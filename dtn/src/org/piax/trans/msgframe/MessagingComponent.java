/*
 * MessagingComponent.java
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
 * 2009/11/18 designed and implemented by M. Yoshida.
 * 
 * $Id: MessagingComponent.java 183 2010-03-03 11:41:21Z yos $
 */
package org.piax.trans.msgframe;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

import org.piax.trans.util.ByteUtil;

/**
 * @author     Mikio Yoshida
 * @version    2.2.0
 */
public abstract class MessagingComponent {
    protected final byte[] magic;
    protected final MessagingComponent parent;

    protected MessagingComponent(byte[] magic, MessagingComponent parent)
    throws MagicNumberConflictException {
        this.magic = magic;
        this.parent = parent;
        // register this to parent
        if (parent != null && !parent.register(this)) {
            throw new MagicNumberConflictException(ByteUtil.bytes2Hex(magic)
                    + " is conflicted");
        }
    }

    /**
     * senderを持たない <code>MessagingLeaf</code>オブジェクトを生成する。
     * <p>
     * rootに位置する <code>MessagingBranch</code>オブジェクトの
     * superコンストラクタ、または、特殊用途に用いる。
     */
    protected MessagingComponent() {
        this.magic = null;
        this.parent = null;
    }

    /**
     * magicを返す。
     * 
     * @return magic number
     */
    public byte[] getMagic() {
        return magic;
    }

    /**
     * parent を返す。
     * 
     * @return parent
     */
    public MessagingComponent getParent() {
        return parent;
    }

    /**
     * このオブジェクトを終了させる。
     * <p>
     * parentの登録が抹消される。
     */
    public void fin() {
        // unregister this from parent
        if (getParent() != null)
            getParent().unregister(this);
    }
    
    /**
     * local peer の<code>MessageReachable</code>を取得する。
     * <p>
     * parent が <code>null</code>であるか、または、
     * parent の持つ値を継承しない場合はオーバライドする必要がある。
     * 
     * @return local peer の<code>MessageReachable</code>
     */
    public MessageReachable getLocalPeer() {
        if (getParent() == null)
            throw new UnsupportedOperationException();
        return getParent().getLocalPeer();
    }

    /**
     * 指定された<code>MessagingComponent</code>オブジェクトを
     * childとして登録する。
     * 
     * @param child 登録する<code>MessagingComponent</code>オブジェクト
     * @return すでに登録されている場合、<code>false</code>。
     *          それ以外は<code>true</code>
     */
    protected boolean register(MessagingComponent child) {
         throw new UnsupportedOperationException();
    }

    /**
     * 指定された<code>MessagingComponent</code>オブジェクトを
     * childから登録抹消する。
     * 
     * @param child
     *            登録抹消する<code>MessagingComponent</code>オブジェクト
     * @return すでに登録抹消されている場合、<code>false</code>。
     *          それ以外は<code>true</code>
     */
    protected boolean unregister(MessagingComponent child) {
         throw new UnsupportedOperationException();
    }

    /**
     * 登録されているchildrenとして、<code>MessagingComponent</code>
     * オブジェクトのセットを返す。
     * 
     * @return 登録されている<code>MessagingComponent</code>オブジェクトのセット
     */
    public Set<MessagingComponent> getChildren() {
        throw new UnsupportedOperationException();
    }

    
//    public abstract void send(MessageReachable toPeer, byte[] msg)
//            throws NoSuchPeerException, IOException;

    protected abstract void _send(byte[] magic, Session session,
            MessageReachable toPeer, ByteBuffer msg) 
            throws NoSuchPeerException, IOException;

    /**
     * 送信元ピアからの送信データを受信する。
     * 
     * @param msg 送信元ピアからの送信データ
     * @param callerHandle 送信元の情報を扱うためのハンドラ
     */
    protected abstract void _receive(ByteBuffer msg, CallerHandle callerHandle);

    protected abstract void _reply(byte[] magic, CallerHandle callerHandle, 
            ByteBuffer msg)
            throws IOException;

    /**
     * 送信先ピアからのreplyデータを受信する。
     * 
     * @param session <code>Session</code>オブジェクト
     * @param msg 送信先ピアからのreplyデータ
     */
    protected abstract void _receiveReply(Session session, ByteBuffer msg);
}
