/*
 * MessagingBranch.java
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
 * $Id: MessagingBranch.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans.msgframe;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.grlea.log.SimpleLogger;
import org.piax.trans.common.Id;
import org.piax.trans.util.ByteBufferUtil;

// -- I am waiting for someone to translate the following doc into English. :-)

/**
 * メッセージ送受信可能なクラス。
 * 
 * @author      Mikio Yoshida
 * @version     2.2.0
 * 
 * @see MessageReachable
 * @see MessagingLeaf
 * @see CallerHandle
 * @see Session
 */
public abstract class MessagingBranch extends MessagingComponent {
    /*--- logger ---*/
    private static final SimpleLogger log = new SimpleLogger(
            MessagingBranch.class);

    /*
     * uses ConcurrentMap class, for avoiding the synchronization in
     * selectReceiver method calls.
     */
    private final ConcurrentMap<Id, MessagingComponent> receivers;

    /**
     * <code>MessagingBranch</code>オブジェクトを生成する。
     * <p>
     * 生成する<code>MessagingBranch</code>オブジェクトが、下位にsenderとして
     * 使う<code>MessagingBranch</code>オブジェクトを持つ場合に、
     * このコンストラクタを用いる。
     * <p>
     * 引数の magic および sender に<code>null</code>を指定すると、
     * <code>IllegalArgumentException</code>がスローされる。
     * 
     * @param magic magic number
     * @param sender
     *            senderとして使う<code>MessagingBranch</code>オブジェクト
     * @throws MagicNumberConflictException magic の衝突を検知した場合
     */
    public MessagingBranch(byte[] magic, MessagingBranch sender)
            throws MagicNumberConflictException {
        super(magic, sender);
        receivers = new ConcurrentHashMap<Id, MessagingComponent>();
    }
    
    protected MessagingBranch() {
        super();
        receivers = new ConcurrentHashMap<Id, MessagingComponent>();
    }
    
    /**
     * 指定された<code>MessagingLeaf</code>オブジェクトを登録する。
     * <p>
     * このメソッドは内部的に使用されるため、サブクラスで再定義できない。
     * 
     * @param receiver 登録する<code>MessagingLeaf</code>オブジェクト
     * @return すでに登録されている場合、<code>false</code>。
     *          それ以外は<code>true</code>
     */
    @Override
    protected final boolean register(MessagingComponent receiver) {
        return (receivers.putIfAbsent(new Id(receiver.getMagic()), receiver)
                == null);
    }

    /**
     * 指定された<code>MessagingLeaf</code>オブジェクトを登録抹消する。
     * <p>
     * このメソッドは内部的に使用されるため、サブクラスで再定義できない。
     * 
     * @param receiver
     *            登録抹消する<code>MessagingLeaf</code>オブジェクト
     * @return すでに登録抹消されている場合、<code>false</code>。
     *          それ以外は<code>true</code>
     */
    @Override
    protected final boolean unregister(MessagingComponent receiver) {
        return receivers.remove(new Id(receiver.getMagic()), receiver);
    }

    /**
     * 登録されている<code>MessagingLeaf</code>オブジェクトのセットを返す。
     * 
     * @return 登録されている<code>MessagingLeaf</code>オブジェクトのセット
     */
    @Override
    public final Set<MessagingComponent> getChildren() {
        return new HashSet<MessagingComponent>(receivers.values());
    }
    
    private MessagingComponent selectReceiver(ByteBuffer msg) {
        for (Id id : receivers.keySet()) {
            if (ByteBufferUtil.startsWith(id.getBytes(), msg)) {
                return receivers.get(id);
            }
        }
        return null;
    }

    /**
     * senderとして指定されている<code>MessagingBranch</code>オブジェクトの
     * <code>send</code>または <code>sendSync</code>メソッドを呼ぶ際に、ピアを指定
     * するオブジェクトの変換を行う。
     * <p>
     * このメソッドは、senderである<code>MessagingBranch</code>オブジェクトと
     * ピアの指定形式が異なる場合に、サブクラスで再定義する必要がある。
     * 再定義しない場合は、このメソッドはピアの指定形式を変換しない。
     * 
     * @param toPeer 変換前のピア指定
     * @return 変換後のピア指定
     */
    protected MessageReachable translate(MessageReachable toPeer)
            throws NoSuchPeerException {
        return toPeer;
    }
    
    /* (non-Javadoc)
     * @see org.piax.trans.msgframe.MessagingComponent#_send(byte[], 
     * org.piax.trans.msgframe.Session, 
     * org.piax.trans.msgframe.MessageReachable, java.nio.ByteBuffer)
     */
    @Override
    protected void _send(byte[] magic, Session session,
            MessageReachable toPeer, ByteBuffer msg)
            throws NoSuchPeerException, IOException {
        log.entry("_send()");
        log.debugObject("magic", magic);
        log.debugObject("msg", msg);
        getParent()._send(getMagic(), session, translate(toPeer),
                ByteBufferUtil.concat(magic, msg));
        log.exit("_send()");
    }
    
    /* (non-Javadoc)
     * @see org.piax.trans.msgframe.MessagingComponent#_receive(java.nio.ByteBuffer,
     * org.piax.trans.msgframe.CallerHandle)
     */
    @Override
    protected final void _receive(ByteBuffer msg, CallerHandle callerHandle) {
        log.entry("_receive()");
        MessagingComponent receiver = selectReceiver(msg);
        if (receiver == null) {
            log.warn("receive msg purged");
            return;
        }
        ByteBufferUtil.strip(receiver.getMagic().length, msg);
        receiver._receive(msg, callerHandle);
        log.exit("_receive()");
    }

    /* (non-Javadoc)
     * @see org.piax.trans.msgframe.MessagingComponent#_reply(byte[], 
     * org.piax.trans.msgframe.CallerHandle, java.nio.ByteBuffer)
     */
    @Override
    protected void _reply(byte[] magic, CallerHandle callerHandle, ByteBuffer msg)
            throws IOException {
        log.entry("_reply()");
        log.debugObject("magic", magic);
        getParent()._reply(getMagic(), callerHandle,
                ByteBufferUtil.concat(magic, msg));
        log.exit("_reply()");
    }

    /* (non-Javadoc)
     * @see org.piax.trans.msgframe.MessagingComponent#_receiveReply(
     * org.piax.trans.msgframe.Session, java.nio.ByteBuffer)
     */
    @Override
    protected final void _receiveReply(Session session, ByteBuffer msg) {
        log.entry("_receiveReply()");
        MessagingComponent receiver = selectReceiver(msg);
        if (receiver == null) {
            log.warn("receiveReply msg purged");
            return;
        }
        ByteBufferUtil.strip(receiver.getMagic().length, msg);
        receiver._receiveReply(session, msg);
        log.exit("_receiveReply()");
    }
}
