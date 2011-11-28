/*
 * CallAsyncSupporter.java
 * 
 * Copyright (c) 2006- Osaka University
 * Copyright (c) 2004-2005 BBR Inc, Osaka University
 * 
 * Permission is hereby granted, free of charge, to any person obtaining 
 * a copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including 
 * without limitation the rights to use, copy, modify, merge, publish, 
 * distribute, sublicense, and/or sell copies of the Software, and to 
 * permit persons to whom the Software is furnished to do so, subject to 
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be 
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
/*
 * Revision History:
 * ---
 * 2007/02/01 designed and implemented by M. Yoshida.
 * 
 * $Id: CallAsyncSupporter.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans.rpc;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.LinkedList;

import org.grlea.log.SimpleLogger;
import org.piax.trans.common.FutureReturn;
import org.piax.trans.common.Id;
import org.piax.trans.common.PeerId;
import org.piax.trans.common.impl.FutureReturnImpl;
import org.piax.trans.msgframe.CallerHandle;

/**
 * @author     Mikio Yoshida
 * @version    1.0.0
 */
class CallAsyncSupporter {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(CallAsyncSupporter.class);
    
    /*--- nested classes ---*/
    
    static class CallPack implements Serializable {
        private static final long serialVersionUID = -6560119202456569289L;
        
        final PeerId orgPeer;
        final int seqNo;
        final RPCallPack callBody;

        CallPack(PeerId orgPeer, int seqNo, RPCallPack callBody) {
            this.orgPeer = orgPeer;
            this.seqNo = seqNo;
            this.callBody = callBody;
        }
    }
    
    static class ReturnPack implements Serializable {
        private static final long serialVersionUID = 2905083277883245897L;
        
        final int seqNo;
        final Object retValueOrException;
        final PeerId peerId;

        ReturnPack(int seqNo, Object retValueOrException, PeerId peerId) {
            this.seqNo = seqNo;
            this.retValueOrException = retValueOrException;
            this.peerId = peerId;
        }
    }
    
    private static class Entry {
        final int seqNo;
        final long date;
        final FutureReturnImpl<Object> future;
        
        Entry(int seqNo, FutureReturnImpl<Object> future) {
            this.seqNo = seqNo;
            this.future = future;
            date = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - date > RPCWrapper.EXPIRATION_PERIOD;
        }
        
        boolean isCancelled() {
            return future.isCancelled();
        }
    }

    private static class WrapperException extends Exception {
        private static final long serialVersionUID = -340505015309276528L;

        WrapperException(Throwable cause) {
            super(cause);
        }
    }
    
    /*--- class methods ---*/   
    
    private static int seqNo = 0;
    private static synchronized int genSeqNo() {
        return seqNo++;
    }
    
    /*--- instance fields ---*/
    
    private final RPCWrapper rpcw;
    private final LinkedList<Entry> register;
    
    /*--- constructors ---*/
    
    CallAsyncSupporter(RPCWrapper rpcw) {
        this.rpcw = rpcw;
        register = new LinkedList<Entry>();
    }
    
    /*--- instance methods ---*/
    
    FutureReturn<Object> sendCall(PeerId toPeer, RPCallPack callBody, long timeout) 
            throws IOException {
        int seq = genSeqNo();
        CallPack callPk = new CallPack(rpcw.getPeerId(), seq, callBody);
        FutureReturnImpl<Object> future = new FutureReturnImpl<Object>();
        synchronized (register) {
            register.offer(new Entry(seq, future));
        }        
        rpcw.send(toPeer, callPk, timeout);
        return future;
    }

    FutureReturn<Object> localSendCall(Id objId, Object option, RPCallPack callBody) {
        int seq = genSeqNo();
        CallPack callPk = new CallPack(rpcw.getPeerId(), seq, callBody);
        FutureReturnImpl<Object> future = new FutureReturnImpl<Object>();
        future.setObjectId(objId);
        future.setOption(option);
        synchronized (register) {
            register.offer(new Entry(seq, future));
        }
        try {
            rpcw.send(rpcw.getPeerId(), callPk, RPCWrapper.RPC_TIMEOUT);
        } catch (IOException e) {
            // not occurred
            log.errorException(e);
        }
        return future;
    }

    boolean cancel(FutureReturn<Object> future) {
        synchronized (register) {
            Iterator<Entry> it = register.iterator();
            while (it.hasNext()) {
                Entry ent = it.next();
                if (ent.future == future) {
                    it.remove();
                    future.cancel();
                    return true;
                }
            }
            return false;
        }        
    }
    
    private FutureReturnImpl<Object> search(int seqNo) {
        synchronized (register) {
            Iterator<Entry> it = register.iterator();
            while (it.hasNext()) {
                Entry ent = it.next();
                if (ent.seqNo == seqNo) {
                    it.remove();
                    return ent.future;
                }
                // remove meaningless entry
                if (ent.isCancelled() || ent.isExpired()) {
                    it.remove();
                }
            }
            return null;
        }        
    }
    
    void receiveRet(ReturnPack retPk) {
        int seq = retPk.seqNo;
        FutureReturnImpl<Object> future = search(seq);
        if (future == null) {
            log.info("Returned but FutureReturn disappeared");
            return;
        }
        future.setPeerId(retPk.peerId);
        if (retPk.retValueOrException instanceof WrapperException) {
            Throwable excep = 
                ((WrapperException) retPk.retValueOrException).getCause();
            future.setException(excep);
        } else {
            future.setResult(retPk.retValueOrException);
        }
    }
    
    void localCall(CallPack callPk, CallerHandle handle) {
        int seq = callPk.seqNo;
        Object ret;
        try {
            ret = rpcw.invoke(callPk.callBody);
        } catch (InvocationTargetException e) {
            log.debug("localCall:InvocationTargetException");
            ret = new WrapperException(e);
        } catch (NoSuchMethodException e) {
            log.debug("localCall:NoSuchMethodException");
            ret = new WrapperException(e);
        }
        ReturnPack retPk = new ReturnPack(seq, ret, rpcw.getPeerId());
        try {
            rpcw.reply(handle, retPk);
        } catch (IOException e) {
            log.warn("requester timeout and closed");
        }
    }

}
