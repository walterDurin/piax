/*
 * TinyRPCWrapper.java
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
 * $Id: RPCWrapper.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans.rpc;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.grlea.log.SimpleLogger;
import org.piax.trans.IdTransport;
import org.piax.trans.common.FutureReturn;
import org.piax.trans.common.Id;
import org.piax.trans.common.PeerId;
import org.piax.trans.common.PeerLocator;
import org.piax.trans.common.ReturnSet;
import org.piax.trans.common.impl.AbstractReturnSet;
import org.piax.trans.common.impl.ReturnSetAggregator;
import org.piax.trans.msgframe.MagicNumberConflictException;
import org.piax.trans.msgframe.MessagingLeaf;
import org.piax.trans.msgframe.CallerHandle;
import org.piax.trans.msgframe.NoSuchPeerException;
import org.piax.trans.msgframe.Session;
import org.piax.trans.util.SerializingUtil;

//import com.metaparadigm.jsonrpc.JSONSerializer;

/**
 * @author     Mikio Yoshida
 * @version    1.0.0
 */
public class RPCWrapper extends MessagingLeaf {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(RPCWrapper.class);

    static final byte[] MAGIC_NUM = new byte[] {(byte) 0x80, (byte) 0x00};
    
    public static long RPC_TIMEOUT = 10 * 1000L;
    public static long CALLMULTI_TIMEOUT = 20 * 1000L;
    public static long RETURNSET_GETNEXT_TIMEOUT = 15 * 1000L;
    public static int RETURNSET_CAPACITY = 100;
    static final long EXPIRATION_PERIOD = 60 * 60 * 1000L;

    private static class RPCServiceEle {
        OldRPCService instance;
        Method[] methods;
        
        RPCServiceEle(OldRPCService instance, Method[] methods) {
            this.instance = instance;
            this.methods = methods;
        }
    }
    
    private final Map<String, RPCServiceEle> services;
    private final IdTransport transport;
    
    private final CallAsyncSupporter asyncSupp;
    private final CallMultiSupporter multiSupp;
    
//    JSONSerializer json = new JSONSerializer();
    
    public RPCWrapper(IdTransport transport) 
    throws MagicNumberConflictException {
        super(MAGIC_NUM, transport);
        services = new HashMap<String, RPCServiceEle>(1);
        this.transport = transport;
        
        asyncSupp = new CallAsyncSupporter(this);
        multiSupp = new CallMultiSupporter(this);
    }

    @Override
    public void fin() {
        services.clear();
        super.fin();
    }
    
    public final PeerId getPeerId() {
        return transport.getPeerId();
    }
    
    public final String getPeerName() {
        return transport.getPeerName();
    }
    
//    private final PeerLocator getLocator() {
        //return transport.getLocator();
//    }

    public synchronized void register(OldRPCService service) {
        String name = service.getServiceName();
        // musasabi's KernelImpl extends AgentHomeImpl.
        // we want to call methods both in KernelImpl and AgentHomeImpl!
        //Method[] methods = service.getClass().getDeclaredMethods();
        Method[] methods = service.getClass().getMethods();
        services.put(name, new RPCServiceEle(service, methods));
    }
    
    public synchronized boolean unregister(OldRPCService service) {
        String name = service.getServiceName();
        return services.remove(name) != null;
    }

    private synchronized OldRPCService getService(String name) {
        RPCServiceEle ele = services.get(name);
        if (ele == null) {
            log.error("Service " + name + " is not registered");
            return null;
        }
        return ele.instance;
    }

    private synchronized Method[] getMethods(String name) {
        return services.get(name).methods;
    }
    
    /** Major RPC Services **/
    
    public Object call(String serviceName, String method, Object... args) 
    throws InvocationTargetException, NoSuchMethodException {
        return invoke(new RPCallPack(serviceName, method, args));
    }
    
    public ReturnSet<Object> callMulti(String serviceName,
            String method, Object... args) {
        return invokeMulti(new RPCallPack(serviceName, method, args), true);
    }

    public Object remoteCall(PeerId toPeer, 
            String serviceName, String method, Object... args) 
            throws InterruptedIOException, IOException, NoSuchMethodException,  
            InvocationTargetException {
        
        return remoteCall(toPeer, RPC_TIMEOUT, serviceName, method, args);
    }

    public Object remoteCall(PeerId toPeer, long timeout, 
            String serviceName, String method, Object... args) 
            throws InterruptedIOException, IOException, NoSuchMethodException,  
            InvocationTargetException {
        
        FutureReturn<?> future = remoteCallAsync(toPeer, timeout, serviceName, method, args);
        try {
            return future.get(timeout);
        } catch (InterruptedException e) {
            throw new InterruptedIOException("Remote call timeouted");
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            if (t instanceof NoSuchMethodException) {
                throw (NoSuchMethodException) t;
            } else if (t instanceof InvocationTargetException) {
                throw (InvocationTargetException) t;
            } else {
                log.errorException(t);
                throw new UndeclaredThrowableException(t);
            }
        }
    }

    public FutureReturn<Object> remoteCallAsync(PeerId toPeer,
            String serviceName, String method, Object... args) 
            throws IOException {
        return remoteCallAsync(toPeer, RPC_TIMEOUT, serviceName, method, args);
    }

    public FutureReturn<Object> remoteCallAsync(PeerId toPeer, long timeout,
            String serviceName, String method, Object... args) 
            throws IOException {
        
        RPCallPack pack = new RPCallPack(serviceName, method, args);
        return asyncSupp.sendCall(toPeer, pack, timeout);
    }

    public FutureReturn<Object> localCallAsync(Id objId, Object option, 
            String serviceName, String method, Object... args) {
        
        RPCallPack pack = new RPCallPack(serviceName, method, args);
        return asyncSupp.localSendCall(objId, option, pack);
    }

    public void remoteCallOneway(PeerId toPeer, 
            String serviceName, String method, Object... args) 
            throws IOException {
        remoteCallAsync(toPeer, RPC_TIMEOUT, serviceName, method, args);
    }

    public ReturnSet<Object> remoteCallMulti(PeerId toPeer, 
            String serviceName, String method, Object... args) 
            throws IOException {

        RPCallPack pack = new RPCallPack(serviceName, method, args);
        return multiSupp.sendCall(toPeer, pack, true);
    }

    // TODO 
    // filterExcepted maybe false
    public ReturnSet<Object> remoteCallMulti(Set<PeerId> toPeers, 
            String serviceName, String method, Object... args) 
            throws IOException {
        
        if (toPeers == null || toPeers.size() == 0) {
            throw new IllegalArgumentException("toPeers is null or zero");
        }
        
        RPCallPack pack = new RPCallPack(serviceName, method, args);
        ReturnSetAggregator<Object> parent = 
            new ReturnSetAggregator<Object>(RETURNSET_CAPACITY);
        for (PeerId toPeer : toPeers) {
            ReturnSet<Object> rset = multiSupp.sendCall(toPeer, pack, true);
            // TODO buggy!!
            parent.chain((AbstractReturnSet<Object>) rset);
        }
        parent.noMoreChains();
        return parent;
    }
    
    /** Methods for friend instances **/

    Object invoke(RPCallPack callPk) 
    throws InvocationTargetException, NoSuchMethodException {
        
        // TODO
        // many spaces to be brushed up
        String serviceName = callPk.getServiceName();
        String methodName = callPk.getMethod();
        Object[] args = callPk.getArgs();

        OldRPCService service = getService(serviceName);
        if (service == null) {
            throw new IllegalArgumentException("invalid service name: " 
                    + serviceName);
        }
        Method[] methods = getMethods(serviceName);
        
        Method method = null;
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().equals(methodName)) {
                Class<?>[] params = methods[i].getParameterTypes();
                int arglen = (args == null)? 0 : args.length;
                if (params.length == arglen) {
                    method = methods[i];
                    break;
                }
            }
        }
        
        if (method == null) {
            throw new NoSuchMethodException(methodName);
        }
        try {
            return method.invoke(service, args);
        } catch (IllegalAccessException e) {
            log.errorException(e);
            throw new NoSuchMethodException();
        }
    }

    // for friend class
    // TODO more good design
    @SuppressWarnings("unchecked")
    ReturnSet<Object> invokeMulti(RPCallPack callPk, boolean filterExcepted) {
        try {
            return (ReturnSet<Object>) invoke(callPk);
        } catch (InvocationTargetException e) {
            // TODO
            log.warnException(e.getCause());
            return null;
        } catch (NoSuchMethodException e) {
            // TODO
            log.warn("NoSuchMethodException raised");
            return null;
        }
    }

    private byte[] encode(Serializable data) {
        try {
            return SerializingUtil.serialize(data);
        } catch (ObjectStreamException e) {
            log.errorException(e);
            return null;
        }
    }
    
    private Serializable decode(byte[] bdata) {
        try {
            return SerializingUtil.deserialize(bdata);
        } catch (ObjectStreamException e) {
            log.errorException(e);
            return null;
        } catch (ClassNotFoundException e) {
            log.errorException(e);
            return null;
        }
    }

    Serializable sendSync(PeerId toPeer, Serializable data, long timeout) 
            throws InterruptedIOException, IOException, ClassNotFoundException {
        log.entry("sendSync()");
        
        byte[] callData = encode(data);
        try {
            byte[] retData = super.sendSync(toPeer, callData, timeout);
            return decode(retData);
        } catch (NoSuchPeerException e) {
            throw new IOException("No such peer");
        }
    }

    private class RPCSession extends Session {
        RPCSession(MessagingLeaf receiver) {
            super(receiver);
        }
        @Override
        public void receiveReply(byte[] data) {
            Object obj = decode(data);
            if (obj == null) return;
            
            if (obj instanceof CallAsyncSupporter.ReturnPack) {
                asyncSupp.receiveRet((CallAsyncSupporter.ReturnPack) obj);
            } else if (obj instanceof CallMultiSupporter.AckPack) {
                multiSupp.receiveAck((CallMultiSupporter.AckPack) obj);
            } else {
                log.error("Invalid received reply packet");
            }
        }
    }

    /*****/
    void send(PeerId toPeer, Serializable data, long timeout)
            throws IOException {
        Session session = new RPCSession(this);
        byte[] callData = encode(data);
        try {
            session.send(toPeer, callData);
        } catch (NoSuchPeerException e) {
            IOException ioe = new IOException("No such peer");
            ioe.initCause(e);
            throw ioe;
        }
    }

    void reply(CallerHandle caller, Serializable data)
            throws IOException {
        byte[] replyData = encode(data);
        reply(caller, replyData);
    }

    @Override
    public void receive(byte[] bdata, CallerHandle caller) {
        Object obj = decode(bdata);
        if (obj == null) return;
        
        if (obj instanceof CallAsyncSupporter.CallPack) {
            asyncSupp.localCall((CallAsyncSupporter.CallPack) obj, caller);
        } else if (obj instanceof CallMultiSupporter.CallPack) {
            multiSupp.localCall((CallMultiSupporter.CallPack) obj, caller);
        } else if (obj instanceof CallMultiSupporter.ReturnPack) {
            multiSupp.receiveRet((CallMultiSupporter.ReturnPack) obj, caller);
//        } else if (obj instanceof ResolvPack) {
//            try {
//                reply(caller, transport.getPeerId());
//            } catch (IOException e) {
//                log.warn("requester timeout and closed");
//            }
        } else {
            log.error("Invalid received packet");
        }
    }
}
