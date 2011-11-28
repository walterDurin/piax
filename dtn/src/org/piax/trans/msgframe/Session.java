/*
 * Session.java
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
 * 2007/10/31 designed and implemented by M. Yoshida.
 * 
 * $Id: Session.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans.msgframe;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.grlea.log.SimpleLogger;
import org.piax.trans.util.ByteBufferUtil;

/**
 * send/reply型のメッセージ送信において、送信元として使用されるセッション。
 * 
 * @author     Mikio Yoshida
 * @version    2.2.0
 * 
 * @see MessageReachable
 * @see MessagingLeaf
 */
public abstract class Session {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(Session.class);

    private final MessagingLeaf receiver;
    int sessionId = 0;
    SessionMgr mgr = null;
    
    public Session(MessagingLeaf receiver) {
        this.receiver = receiver;
    }
    
    public void send(MessageReachable toPeer, byte[] msg)
            throws NoSuchPeerException, IOException {
        send(toPeer, msg, 0, msg.length);
    }
    
    public void send(MessageReachable toPeer, byte[] msgBuf, int offset, int len)
            throws NoSuchPeerException, IOException {
        send(toPeer, ByteBufferUtil.byte2Buffer(msgBuf, offset, len));
    }

    public void send(MessageReachable toPeer, ByteBuffer msg)
            throws NoSuchPeerException, IOException {
        receiver.getParent()._send(receiver.getMagic(), this, toPeer, msg);
    }

    protected abstract void receiveReply(byte[] msg);
    
    protected void receiveReply(ByteBuffer msg) {
        receiveReply(ByteBufferUtil.buffer2Bytes(msg));
    }
    
    public void fin() {
        if (mgr == null) return;
        if (!mgr.removeSession(sessionId, this)) {
            log.warn("could not remove session: " + sessionId);
        }
    }
}
