/*
 * RPCInvoker.java
 * 
 * Copyright (c) 2008-2010 National Institute of Information and 
 * Communications Technology
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
 * 2007/11/01 designed and implemented by M. Yoshida.
 * 
 * $Id: RPCInvoker.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans.msgframe;

import java.io.IOException;
import java.io.NotSerializableException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.grlea.log.SimpleLogger;
import org.piax.trans.util.ByteBufferUtil;
import org.piax.trans.util.ClassUtil;
import org.piax.trans.util.MethodUtil;
import org.piax.trans.util.SerializingUtil;

//-- I am waiting for someone to translate the following doc into English. :-)

/**
 * MessagingLeafのRPC拡張クラス。
 * 
 * @author     Mikio Yoshida
 * @version    2.2.0
 */
public abstract class RPCInvoker extends MessagingLeaf
        implements RPCInvokerIf {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(RPCInvoker.class);

    public static long RPC_TIMEOUT = 10 * 1000L;

    /*
     * stubの生成は頻繁に行われるが、コストのかかるproxyクラスの生成において、
     * キャッシュが機能するため、getStub全体のコストは非常に低い。
     */
    
    /**
     * ローカルピア上の指定されたserviceオブジェクトに対して、メソッド呼び出し
     * を行うためのstubを返す。
     * 
     * @param service service object
     * @return stub
     */
    public static RPCInvokerIf getStub(RPCInvoker service) {
        return getStub(service, service.getLocalPeer(), 0);
    }

    /**
     * ピア上の指定されたserviceオブジェクトに対して、メソッド呼び出し
     * を行うためのstubを返す。
     * <p>
     * timeoutは、デフォルト（10秒）がセットされる。
     * 呼び出し先をローカルピアに指定した場合、timeout設定は無視される。
     * 
     * @param service service object
     * @param toPeer メソッド呼び出し先となるリモートまたはローカルピア
     * @return stub
     */
    public static RPCInvokerIf getStub(RPCInvoker service, 
            MessageReachable toPeer) {
        return getStub(service, toPeer, RPC_TIMEOUT);
    }
    
    /**
     * ピア上の指定されたserviceオブジェクトに対して、メソッド呼び出し
     * を行うためのstubを返す。
     * <p>
     * 呼び出し先をローカルピアに指定した場合、timeout設定は無視される。
     * 
     * @param service service object
     * @param toPeer メソッド呼び出し先となるリモートまたはローカルピア
     * @param timeout timeout(millis)
     * @return stub
     */
    public static RPCInvokerIf getStub(RPCInvoker service,
            MessageReachable toPeer, long timeout) {
        if (service == null || toPeer == null) {
            throw new IllegalArgumentException(
                    "rpc service and dest peer should not be null");
        }
        MessageReachable _toPeer = toPeer.equals(service.getLocalPeer()) ?
                null : toPeer;
        Class<?> serviceCls = service.getClass();
        ClassLoader loader = serviceCls.getClassLoader();
        Class<?>[] ifs = ClassUtil.gatherLowerBoundSuperInterfaces(
                service.getClass(), RPCInvokerIf.class);
        RPCInvocationHandler invoker = new RPCInvocationHandler(service,
                _toPeer, timeout);
        return (RPCInvokerIf) Proxy.newProxyInstance(loader, ifs, invoker);
    }

    public static class MethodCall {
        String method = null;
        List<Object> args = new ArrayList<Object>();
    }

    /**
     * @param magic
     * @param sender
     * @throws MagicNumberConflictException magic の衝突を検知した場合
     */
    public RPCInvoker(byte[] magic, MessagingBranch sender)
            throws MagicNumberConflictException {
        super(magic, sender);
    }

    /**
     * 指定されたオブジェクトをserializeして、指定されたByteBufferに追記する。
     * <p>
     * ByteBufferの容量を越えた場合は、領域拡張したByteBufferを割り当てる。
     * ByteBufferの指定がnullの場合は、ByteBufferを新規に作成する。
     * このByteBufferはヘッダを追加できるようmarginを持つ。
     * ByteBufferから追記したbyteデータを使う場合は、rewindする必要がある。
     * 
     * @param bbuf ByteBuffer
     * @param obj serializeするオブジェクト
     * @return serializeしたdataを追記したByteBuffer
     * @throws Exception serializeに失敗したとき
     */
    protected ByteBuffer serialize(ByteBuffer bbuf, Object obj) throws Exception {
        try {
            if (bbuf == null) {
                ByteBuffer bb = ByteBufferUtil.byte2Buffer(SerializingUtil.serialize(obj));
                bb.position(bb.limit());
                return bb;
            }
            return ByteBufferUtil.put(bbuf, SerializingUtil.serialize(obj));
        } catch (NotSerializableException e) {
            log.errorException(e);
            throw e;
        }
    }
    
    protected ByteBuffer serializeMethodCall(ByteBuffer bbuf, String method,
            Object... args) throws Exception {
        return serialize(bbuf, new Object[] {method, args});
    }

    /**
     * ByteBufferからオブジェクトをdeserializeする。
     * ByteBufferのpositionは、deserializeされたバイト分進む。
     * 
     * @param bbuf ByteBuffer
     * @return deserializeされたオブジェクト
     * @throws Exception serializeに失敗したとき
     */
    protected Object deserialize(ByteBuffer bbuf) throws Exception {
        return SerializingUtil.deserialize(bbuf);
    }

    protected MethodCall deserializeMethodCall(ByteBuffer bbuf) throws Exception {
        MethodCall mc = new MethodCall();
        Object[] methodAndArgs = (Object[]) deserialize(bbuf);
        mc.method = (String) methodAndArgs[0];
        Object[] args = (Object[]) methodAndArgs[1];
        for (Object arg : args) {
            mc.args.add(arg);
        }
        return mc;
    }
    
    protected Object sendInvoke(MessageReachable toPeer, long timeout,
            String method, Object... args) {
        Object ret;
        if (args == null) {
            args = new Object[0];
        }
        try {
            ByteBuffer sdata = serializeMethodCall(null, method, args);
            ByteBufferUtil.rewind(sdata);
            ByteBuffer rdata = sendSync(toPeer, sdata, timeout);
            ret = deserialize(rdata);
        } catch (Throwable e) {
            // any Exception or Error except for InvocationTargetException
            ret = new UndeclaredThrowableException(e);
        }
        return ret;
    }

    @Override
    protected void receive(byte[] msg, CallerHandle callerHandle) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void receive(ByteBuffer msg, CallerHandle callerHandle) {
        ByteBuffer rdata;
        Object ret;
        try {
            MethodCall mc = deserializeMethodCall(msg);
            Object[] args = new Object[mc.args.size()];
            mc.args.toArray(args);
            ret = MethodUtil.invoke(this, RPCInvokerIf.class,
                    mc.method, args);
        } catch (InvocationTargetException e) {
            ret = e;
        } catch (Throwable e) {
            // any Exception or Error except for InvocationTargetException
            ret = new UndeclaredThrowableException(e);
        }
        try {
            rdata = serialize(null, ret);
            ByteBufferUtil.rewind(rdata);
            reply(callerHandle, rdata);
        } catch (IOException e) {
            log.info("requester timeout and closed");
        } catch (OfflineSendException e) {
            log.info("reply msg purged as offline");
        } catch (Exception e) {
            // error in serializing
            log.errorException(e);
        }
    }
}
