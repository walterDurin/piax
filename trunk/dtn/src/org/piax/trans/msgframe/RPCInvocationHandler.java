/*
 * RPCInvocationHandler.java
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
 * $Id: RPCInvocationHandler.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans.msgframe;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;

import org.grlea.log.SimpleLogger;

//-- I am waiting for someone to translate the following doc into English. :-)

/**
 * RPCServiceクラスにおいて生成されたstubのための<code>InvocationHandler</code>
 * の実装クラス。
 * 
 * @author     Mikio Yoshida
 * @version    2.1.0
 */
class RPCInvocationHandler implements InvocationHandler {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(RPCInvocationHandler.class);

    private RPCInvoker service;
    private MessageReachable toPeer;    // null means local peer
    private long timeout;       // if toPeer is null, will be ignored
    
    public RPCInvocationHandler(RPCInvoker service, MessageReachable toPeer,
            long timeout) {
        this.service = service;
        this.toPeer = toPeer;
        this.timeout = timeout;
    }

    /* (non-Javadoc)
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
     */
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        Object result;
        // case of local peer
        if (toPeer == null) {
            // TODO THINK! proc of UndeclaredThrowableException
            try {
                result = method.invoke(service, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            } catch (Throwable e) {
                throw e;
            } finally {
            }
            return result;
        }
        
        // case of remote peer
        result = service.sendInvoke(toPeer, timeout, method.getName(), args);
        if (result instanceof InvocationTargetException)
            throw ((InvocationTargetException) result).getCause();
        if (result instanceof UndeclaredThrowableException) {
            /*
             * ここでは、以下の例外が起こりうる
             * NoSuchPeerException, InterruptedIOException, IOException
             * 例外を明示的に扱うためには、RPCInvokerIfのサブインタフェースに
             * 定義しておく必要がある
             */
            throw ((UndeclaredThrowableException) result).getCause();
        }
        return result;
    }
}
