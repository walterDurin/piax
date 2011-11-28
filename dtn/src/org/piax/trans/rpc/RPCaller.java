/*
 * RPCaller.java
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
 * $Id: RPCaller.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans.rpc;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import org.piax.trans.common.FutureReturn;
import org.piax.trans.common.Id;
import org.piax.trans.common.PeerId;
import org.piax.trans.common.ReturnSet;

/**
 * @author     Mikio Yoshida
 * @version    1.0.0
 */
public abstract class RPCaller {
    
    private final RPCWrapper rpcw;
    protected final String sid;
    
    public RPCaller(RPCWrapper rpcw, String sid) {
        this.rpcw = rpcw;
        this.sid = sid;
    }

    public Object remoteCall(PeerId toPeer, String method, Object... args) 
    throws IOException, NoSuchMethodException, InterruptedIOException, 
    InvocationTargetException {
        return rpcw.remoteCall(toPeer, sid, method, args);
    }

    public Object remoteCall(PeerId toPeer, long timeout, 
            String method, Object... args) 
            throws IOException, NoSuchMethodException, InterruptedIOException, 
            InvocationTargetException {
        return rpcw.remoteCall(toPeer, timeout, sid, method, args);
    }

    public FutureReturn<Object> remoteCallAsync(PeerId toPeer, String method, Object... args) 
    throws IOException {
        return rpcw.remoteCallAsync(toPeer, sid, method, args);
    }

    @Deprecated
    public FutureReturn<Object> localCallAsync(Id objId, Object option, 
            String method, Object... args) {
        return rpcw.localCallAsync(objId, option, sid, method, args);
    }

    public void remoteCallOneway(PeerId toPeer, String method, Object... args) 
            throws IOException {
        rpcw.remoteCallOneway(toPeer, sid, method, args);
    }

    public ReturnSet<Object> remoteCallMulti(PeerId toPeer, String method, Object... args) 
            throws IOException {
        return rpcw.remoteCallMulti(toPeer, sid, method, args);
    }

    public ReturnSet<Object> remoteCallMulti(Set<PeerId> toPeers, String method, Object... args) 
            throws IOException {
        return rpcw.remoteCallMulti(toPeers, sid, method, args);
    }
    
}
