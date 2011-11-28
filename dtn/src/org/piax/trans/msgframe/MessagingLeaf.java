/*
 * MessagingLeaf.java
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
 * 2007/10/28 designed and implemented by M. Yoshida.
 * 
 * $Id: MessagingLeaf.java 225 2010-06-20 12:34:07Z teranisi $
 */

package org.piax.trans.msgframe;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;

import org.grlea.log.SimpleLogger;
import org.piax.trans.util.ByteBufferUtil;

//-- I am waiting for someone to translate the following doc into English. :-)

/**
 * メッセージ受信可能なクラス。
 * 
 * @author     Mikio Yoshida
 * @version    2.2.0
 * 
 * @see MessageReachable
 * @see MessagingBranch
 * @see CallerHandle
 * @see Session
 */
public abstract class MessagingLeaf extends MessagingComponent {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(MessagingLeaf.class);

    private boolean isOnline = false;
    
    /**
     * <code>MessagingLeaf</code>オブジェクトを生成する。
     * <p>
     * オブジェクトの生成時に、指定した senderに対し、このオブジェクトが
     * receiverとして登録される。
     * <p>
     * 引数の magic および sender に<code>null</code>を指定すると、
     * <code>IllegalArgumentException</code>がスローされる。
     * 
     * @param magic magic number
     * @param sender senderとして使う<code>MessagingBranch</code>オブジェクト
     * @throws MagicNumberConflictException magic の衝突を検知した場合
     */ 
    /**
     * @param magic
     * @param parent
     * @throws MagicNumberConflictException
     */
    public MessagingLeaf(byte[] magic, MessagingComponent parent)
            throws MagicNumberConflictException {
        super(magic, parent);
        if (magic == null || parent == null)
            throw new IllegalArgumentException(
                    "magic and parent should not be null");
        isOnline = true;
    }

    public synchronized boolean isOnline() {
        return isOnline;
    }
    
    public synchronized void online() {
        isOnline = true;
    }
    
    public synchronized void offline() {
        isOnline = false;
    }
    
    private static class LockSession extends Session {
        ByteBuffer ret = null;
        public LockSession(MessagingLeaf receiver) {
            super(receiver);
        }

        @Override
        protected void receiveReply(byte[] msg) {
            throw new UnsupportedOperationException();
        }

        @Override
        public synchronized void receiveReply(ByteBuffer msg) {
            ret = msg;
            this.notify();
        }
    }

    public byte[] sendSync(MessageReachable toPeer, byte[] msg, long timeout)
            throws NoSuchPeerException, InterruptedIOException, IOException {
        return sendSync(toPeer, msg, 0, msg.length, timeout);
    }

    
    public byte[] sendSync(MessageReachable toPeer, byte[] msgBuf, int offset,
            int len, long timeout)
            throws NoSuchPeerException, InterruptedIOException, IOException {
        return ByteBufferUtil.buffer2Bytes(sendSync(toPeer, 
                ByteBufferUtil.byte2Buffer(msgBuf, offset, len), timeout));
    }
    
    public ByteBuffer sendSync(MessageReachable toPeer, ByteBuffer msg, long timeout)
    throws NoSuchPeerException, InterruptedIOException, IOException {
        if (!isOnline) {
            throw new OfflineSendException("sender is offline");
        }
        final LockSession lock = new LockSession(this);
        synchronized (lock) {
            lock.send(toPeer, msg);
            try {
                lock.wait(timeout);
            } catch (InterruptedException e) {
                lock.fin();
                throw new InterruptedIOException("sendSync interrupted");
            }
        }
        if (lock.ret == null) {
            lock.fin();
            throw new InterruptedIOException("Timeouted sendSync to:" + toPeer);
        }
        ByteBuffer ret = lock.ret;
        lock.fin();
        return ret;
    }

    public void send(MessageReachable toPeer, byte[] msg)
            throws NoSuchPeerException, IOException {
        send(toPeer, msg, 0, msg.length);
    }
    
    public void send(MessageReachable toPeer, byte[] msgBuf, int offset, int len)
            throws NoSuchPeerException, IOException {
        _send(getMagic(), null, toPeer, ByteBufferUtil.byte2Buffer(msgBuf, offset, len));
    }

    public void send(MessageReachable toPeer, ByteBuffer msg)
            throws NoSuchPeerException, IOException {
        _send(getMagic(), null, toPeer, msg);
    }

    public void reply(CallerHandle caller, byte[] msg)
            throws IOException {
        reply(caller, msg, 0, msg.length);
    }
    
    public void reply(CallerHandle caller, byte[] msgBuf, int offset, int len)
            throws IOException {
        _reply(getMagic(), caller, ByteBufferUtil.byte2Buffer(msgBuf, offset, len));
    }

    public void reply(CallerHandle caller, ByteBuffer msg)
            throws IOException {
        _reply(getMagic(), caller, msg);
    }

    /**
     * 送信元ピアからの送信データを受信する。
     * <p>
     * 送信が<code>sendOneway</code>メソッドにより実行された場合、callerには
     * <code>null</code>がセットされる。
     * 
     * @param msg 送信元ピアからの送信データ
     * @param callerHandle replyのために指定するhandle
     */
    protected abstract void receive(byte[] msg, CallerHandle callerHandle);

    protected void receive(ByteBuffer msg, CallerHandle callerHandle) {
        receive(ByteBufferUtil.buffer2Bytes(msg), callerHandle);
    }

    /* (non-Javadoc)
     * @see org.piax.trans.msgframe.MessagingComponent#_send(byte[], 
     * org.piax.trans.msgframe.Session, org.piax.trans.msgframe.MessageReachable, 
     * java.nio.ByteBuffer)
     */
    @Override
    protected final void _send(byte[] magic, Session session,
            MessageReachable toPeer, ByteBuffer msg)
            throws NoSuchPeerException, IOException {
        if (!isOnline) {
            throw new OfflineSendException("sender is offline");
        }
        parent._send(magic, session, toPeer, msg);
    }

    /* (non-Javadoc)
     * @see org.piax.trans.msgframe.MessagingComponent#_receive(
     * java.nio.ByteBuffer, org.piax.trans.msgframe.CallerHandle)
     */
    @Override
    protected final void _receive(ByteBuffer msg, CallerHandle callerHandle) {
        if (!isOnline) {
            log.info("receive msg purged as offline");
            return;
        }
        receive(msg, callerHandle);
    }

    /* (non-Javadoc)
     * @see org.piax.trans.msgframe.MessagingComponent#_reply(byte[],
     * org.piax.trans.msgframe.CallerHandle, java.nio.ByteBuffer)
     */
    @Override
    protected final void _reply(byte[] magic, CallerHandle callerHandle, 
            ByteBuffer msg) throws IOException {
        if (!isOnline) {
            throw new OfflineSendException("replyer is offline");
        }
        parent._reply(magic, callerHandle, msg);
    }

    /* (non-Javadoc)
     * @see org.piax.trans.msgframe.MessagingComponent#_receiveReply(
     * org.piax.trans.msgframe.Session, java.nio.ByteBuffer)
     */
    @Override
    protected final void _receiveReply(Session session, ByteBuffer msg) {
        if (!isOnline) {
            log.info("receiveReply msg purged as offline");
            return;
        }
        session.receiveReply(msg);
    }
}
