/*
 * MessagingRoot.java
 * 
 * Copyright (c) 2008-2010 National Institute of Information and 
 * Communications Technology
 * Copyright (c) 2006 Osaka University
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
 * 2009/11/29 designed and implemented by M. Yoshida.
 * 
 * $Id: MessagingRoot.java 183 2010-03-03 11:41:21Z yos $
 */
package org.piax.trans.msgframe;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.grlea.log.SimpleLogger;
import org.piax.trans.util.ByteBufferUtil;

/**
 * @author      Mikio Yoshida
 * @version     2.2.0
 */
public abstract class MessagingRoot extends MessagingBranch {
    /*--- logger ---*/
    private static final SimpleLogger log = new SimpleLogger(
            MessagingRoot.class);

    private final SessionMgr sessionMgr;
    
    /**
     * <code>MessagingBranch</code>オブジェクトを生成する。
     * <p>
     * 生成する<code>MessagingBranch</code>オブジェクトが、下位にsenderとして
     * 使う<code>MessagingBranch</code>オブジェクトを持たない場合（すなわち
     * rootである場合）に、このコンストラクタを用いる。
     */
    public MessagingRoot() {
        super();
        sessionMgr = new SessionMgr();
    }

    @Override
    public void fin() {
        super.fin();
        sessionMgr.fin();
    }
    
    public int newSessionId(Session session) {
        return sessionMgr.newSessionId(session);
    }
    
    public Session getSession(int sessionId) {
        return sessionMgr.getSession(sessionId);
    }
    
    /**
     * 指定されたピアに対して、データを送信する。
     * sessionがnullでない場合は、非同期にreplyデータを受信する。
     * sessionがnullの場合は、一方向の送信を意味し、timeoutの設定は無視される。
     * 送信内容が一方向である場合は、replyデータは返信されないが、
     * replyデータを返信するかどうかの判断は、送信先に委ねられる。
     * <p>
     * タイムアウトとして、replyが返るまでの時間（単位ミリ秒）を設定する。
     * 指定した時間までにreplyが返らない場合、そのsessionは破棄される。
     * <p>
     * このオブジェクトが rootである場合、このメソッドを実装する必要がある。
     * 
     * @param session <code>Session</code>オブジェクト
     * @param toPeer 送信先ピア
     * @param msg 送信データ
     * @param timeout タイムアウト（ミリ秒）
     * @throws IOException I/O関係の例外が発生した場合
     */
    protected abstract void send(Session session, MessageReachable toPeer, 
            ByteBuffer msg) throws IOException;

    /**
     * replyデータをcaller handleによって指定されたピアへ送信する。
     * <p>
     * このオブジェクトが rootである場合、このメソッドを実装する必要がある。
     * 
     * @param caller replyの契機となった送信元ピアを指定するhandle
     * @param msg replyデータ
     * @throws IOException I/O関係の例外が発生した場合
     */
    protected abstract void reply(CallerHandle caller, ByteBuffer msg)
            throws IOException;
    
    public void receive(ByteBuffer msg, CallerHandle callerHandle) {
        _receive(msg, callerHandle);
    }
    
    public void receiveReply(Session session, ByteBuffer msg) {
        _receiveReply(session, msg);
    }
    
    /* (non-Javadoc)
     * @see org.piax.trans.msgframe.MessagingComponent#_send(byte[], 
     * org.piax.trans.msgframe.Session, 
     * org.piax.trans.msgframe.MessageReachable, java.nio.ByteBuffer)
     */
    @Override
    protected final void _send(byte[] magic, Session session,
            MessageReachable toPeer, ByteBuffer msg)
            throws NoSuchPeerException, IOException {
        log.entry("_send()");
        send(session, translate(toPeer), ByteBufferUtil.concat(magic, msg));
        log.exit("_send()");
    }
    
    /* (non-Javadoc)
     * @see org.piax.trans.msgframe.MessagingComponent#_reply(byte[], 
     * org.piax.trans.msgframe.CallerHandle, java.nio.ByteBuffer)
     */
    @Override
    protected final void _reply(byte[] magic, CallerHandle callerHandle, ByteBuffer msg)
            throws IOException {
        log.entry("_reply()");
        reply(callerHandle, ByteBufferUtil.concat(magic, msg));
        log.exit("_reply()");
    }
}
