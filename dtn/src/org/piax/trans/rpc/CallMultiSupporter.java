/*
 * CallMultiSupporter.java
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
 * $Id: CallMultiSupporter.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans.rpc;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import org.grlea.log.SimpleLogger;
import org.piax.trans.common.Id;
import org.piax.trans.common.PeerId;
import org.piax.trans.common.ReturnSet;
import org.piax.trans.common.impl.FutureReturnImpl;
import org.piax.trans.common.impl.ReturnSetImpl;
import org.piax.trans.msgframe.CallerHandle;

/**
 * @author     Mikio Yoshida
 * @version    1.0.0
 */
class CallMultiSupporter {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(CallMultiSupporter.class);
    
    /*--- nested classes ---*/
    
    static class CallPack implements Serializable {
        private static final long serialVersionUID = 6328970642447770083L;

        final PeerId orgPeer;
        final int seqNo;
        final RPCallPack callBody;
        final boolean filterExcepted;
        final long timeout;

        CallPack(PeerId orgPeer, int seqNo, RPCallPack callBody, 
                boolean filterExcepted, long timeout) {
            this.orgPeer = orgPeer;
            this.seqNo = seqNo;
            this.callBody = callBody;
            this.filterExcepted = filterExcepted;
            this.timeout = timeout;
        }
    }
    
    static class ReturnPack implements Serializable {
        private static final long serialVersionUID = 8203240492605761516L;

        final int seqNo;
        final boolean isLast;
        final Object retValueOrException;
        final PeerId peerId;
        final Id objId;
        final Object option;

        ReturnPack(int seqNo, boolean isLast, Object retValueOrException,
                PeerId peerId, Id objId, Object option) {
            this.seqNo = seqNo;
            this.isLast = isLast;
            this.retValueOrException = retValueOrException;
            this.peerId = peerId;
            this.objId = objId;
            this.option = option;
        }
    }
    
    static class AckPack implements Serializable {
        private static final long serialVersionUID = 2795986046097066160L;
        final int seqNo;
        AckPack(int seqNo) {
            this.seqNo = seqNo;
        }
    }
    
    private static class Entry {
        final int seqNo;
        final long date;
        final ReturnSetImpl<Object> rset;
        
        Entry(int seqNo, ReturnSetImpl<Object> rset) {
            this.seqNo = seqNo;
            this.rset = rset;
            date = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - date > RPCWrapper.EXPIRATION_PERIOD;
        }
        
        boolean isCancelled() {
            return rset.isCancelled();
        }
    }
    
    private static class WrapperException extends Exception {
        private static final long serialVersionUID = 7774788153804321566L;

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
    
    CallMultiSupporter(RPCWrapper rpcw) {
        this.rpcw = rpcw;
        register = new LinkedList<Entry>();
    }
    
    /*--- instance methods ---*/
    
    /** on local peer **/

    ReturnSet<Object> sendCall(PeerId toPeer, RPCallPack callBody) 
            throws IOException {
        return sendCall(toPeer, callBody, false, 
                RPCWrapper.RETURNSET_GETNEXT_TIMEOUT);
    }

    ReturnSet<Object> sendCall(PeerId toPeer, RPCallPack callBody, 
            boolean filterExcepted) throws IOException {
        return sendCall(toPeer, callBody, filterExcepted, 
                RPCWrapper.RETURNSET_GETNEXT_TIMEOUT);
    }
    
    ReturnSet<Object> sendCall(PeerId toPeer, RPCallPack callBody, 
            boolean filterExcepted, long timeout) 
            throws IOException {
        log.entry("sendCall()");
        
        int seq = genSeqNo();
        CallPack callPk = new CallPack(rpcw.getPeerId(), seq, callBody, 
                filterExcepted, timeout);
        ReturnSetImpl<Object> rset = new ReturnSetImpl<Object>();
        synchronized (register) {
            register.offer(new Entry(seq, rset));
        }        
        rpcw.send(toPeer, callPk, RPCWrapper.CALLMULTI_TIMEOUT);
        return rset;
    }

    boolean cancel(ReturnSet<Object> rset) {
        synchronized (register) {
            Iterator<Entry> it = register.iterator();
            while (it.hasNext()) {
                Entry ent = it.next();
                if (ent.rset == rset) {
                    it.remove();
                    rset.cancel();
                    return true;
                }
            }
            return false;
        }        
    }
    
    private ReturnSetImpl<Object> search(int seqNo, boolean delete) {
        synchronized (register) {
            Iterator<Entry> it = register.iterator();
            while (it.hasNext()) {
                Entry ent = it.next();
                if (ent.seqNo == seqNo) {
                    if (delete) {
                        it.remove();
                    }
                    return ent.rset;
                }
                // remove meanless entry
                if (ent.isCancelled() || ent.isExpired()) {
                    it.remove();
                }
            }
            return null;
        }        
    }

    void receiveAck(AckPack ackPk) {
        log.entry("receiveAck()");
        
        int seq = ackPk.seqNo;
        ReturnSetImpl<Object> rset = search(seq, true);
        if (rset == null) {
            return;
        }
        rset.noMoreFutures();
    }
    
    void receiveRet(ReturnPack retPk, CallerHandle handle) {
        log.entry("receiveRet");
        
        int seq = retPk.seqNo;
        ReturnSetImpl<Object> rset = search(seq, false);
        if (rset == null) {
            log.warn("Returned but ReturnSet disappeared");
            return;
        }
        if (rset.isCancelled()) {
            return;
        }
        
        FutureReturnImpl<Object> future = new FutureReturnImpl<Object>();
        future.setObjectId(retPk.objId);
        future.setPeerId(retPk.peerId);
        future.setOption(retPk.option);
        if (retPk.retValueOrException instanceof WrapperException) {
            Throwable excep = 
                ((WrapperException) retPk.retValueOrException).getCause();
            future.setException(excep);
        } else {
            future.setResult(retPk.retValueOrException);
        }
        if (!retPk.isLast) {
            try {
                rset.addFuture(future);
            } catch (IllegalStateException e) {
                log.warn("Unexpected ReturnPack reordering");
            }
        } else {
            rset.addFuture(future);
            rset.noMoreFutures();
        }
        try {
            rpcw.reply(handle, new AckPack(-1));
        } catch (IOException e) {
            // remote side hungup
            rset.cancel();
        }
    }
    
    /** on remote peer **/
    
    void localCall(CallPack callPk, CallerHandle handle) {
        log.entry("localCall()");
        
        PeerId fromPeer = callPk.orgPeer;
        int seq = callPk.seqNo;
        
        ReturnSet<Object> rset = 
            rpcw.invokeMulti(callPk.callBody, callPk.filterExcepted);

        // TODO rset never null ?!
        while (rset != null && rset.hasNext()) {
            Object ret;
            PeerId pId = null;
            Id oId = null;
            Object option = null;
            try {
                log.entry("localCall+getNext");
                ret = rset.getNext(callPk.timeout);
                pId = rset.getThisPeerId();
                oId = rset.getThisTargetId();
                option = rset.getThisOption();
                log.exit("localCall+getNext");
            } catch (NoSuchElementException e) {
                break;
            } catch (InvocationTargetException e) {
                ret = new WrapperException(e.getCause());
            } catch (InterruptedException e) {
                break;
            }
            ReturnPack retPk;
            if (rset.hasNext()) {
                retPk = new ReturnPack(seq, false, ret, pId, oId, option);
            } else {
                retPk = new ReturnPack(seq, true, ret, pId, oId, option);
            }
            try {
                rpcw.sendSync(fromPeer, retPk, RPCWrapper.CALLMULTI_TIMEOUT);
            } catch (InterruptedIOException e) {
                // local side timeouted
                rset.cancel();
                break;
            } catch (ClassNotFoundException e) {
                log.errorException(e);
                return;
            } catch (IOException e) {
                log.errorException(e);
                return;
            }
        }
        try {
            rpcw.reply(handle, new AckPack(seq));
        } catch (IOException e) {
            log.warn("requester timeout and closed");
        }
    }

}
