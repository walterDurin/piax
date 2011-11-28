/*
 * MsgHeader.java
 * 
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
 * 2010/03/03 designed and implemented by M. Yoshida.
 * 
 * $Id: MsgHeader.java 225 2010-06-20 12:34:07Z teranisi $
 */
package org.piax.trans;

import java.nio.ByteBuffer;

import org.piax.trans.common.PeerLocator;
import org.piax.trans.util.ByteBufferUtil;

/**
 * @author     Mikio Yoshida
 * @version    2.2.0
 */
class MsgHeader {
    
    /**
     * ByteBufferから、MsgHeaderを取り出す。
     * ByteBufferのポインタは、MsgHeaderの次のバイトに設定される。
     * 
     * @param bbuf ByteBuffer
     * @return 取り出したMsgHeader
     */
    static MsgHeader strip(ByteBuffer bbuf) {
        short magic = bbuf.getShort();
        int sessionId = bbuf.getInt();
        byte srcId = bbuf.get();
        PeerLocator srcPeer = PeerLocator.unpack(srcId, bbuf);
        byte dstId = bbuf.get();
        PeerLocator dstPeer = PeerLocator.unpack(dstId, bbuf);
        long timestamp = bbuf.getLong();
        ByteBufferUtil.mark(bbuf);
        return new MsgHeader(magic, sessionId, srcPeer, dstPeer, timestamp);
    }
    
    /**
     * ByteBufferの前に余白をとって、MsgHeaderを書き込む。
     * ByteBufferに容量がない場合は、自動的に拡張される。
     * ByteBufferのmarkは、MsgHeaderの最初のバイトにセットされる。
     * 
     * @param h MsgHeader
     * @param bbuf ByteBuffer
     * @return MsgHeaderが書き込まれたByteBuffer
     */
    static ByteBuffer concat(MsgHeader h, ByteBuffer bbuf) {
//         if (h.srcPeer == null) {
//             System.out.println("srcPeer is null");
//         }
//         if (h.dstPeer == null) {
//             System.out.println("dstPeer is null");
//         }
        int len = 2 + 4 + 1 + h.srcPeer.getPackLen() 
                + 1 + h.dstPeer.getPackLen() + 8;
        ByteBuffer b = ByteBufferUtil.reserve(len, bbuf);
        b.putShort(h.msgMagic);
        b.putInt(h.sessionId);
        b.put(h.srcPeer.getId());
        h.srcPeer.pack(b);
        b.put(h.dstPeer.getId());
        h.dstPeer.pack(b);
        b.putLong(h.timestamp);
        ByteBufferUtil.rewind(b);
        return b;
    }
    
    /**
     * MsgHeaderを格納しているByteBufferから、src peerのLocatorを取り出す。
     * 
     * @param bbuf ByteBuffer
     * @return src peerのLocator
     */
    static PeerLocator extractSrcPeer(ByteBuffer bbuf) {
        bbuf.getShort();
        bbuf.getInt();
        byte srcId = bbuf.get();
        PeerLocator srcPeer = PeerLocator.unpack(srcId, bbuf);
        ByteBufferUtil.rewind(bbuf);
        return srcPeer;
    }
    
    private final short msgMagic;
    private final int sessionId;
    final PeerLocator srcPeer;
    final PeerLocator dstPeer;
    final long timestamp;

    /*
     * ByteBufferからのMsgHeader抽出時に呼び出される。
     */
    private MsgHeader(short msgMagic, int sessionId,
            PeerLocator srcPeer, PeerLocator dstPeer, long timestamp) {
        /*
         * TODO
         * msgMagic が一致しているかのチェックを行う必要がある
         */
        this.msgMagic = msgMagic;
        this.sessionId = sessionId;
        this.srcPeer = srcPeer;
        this.dstPeer = dstPeer;
        this.timestamp = timestamp;
    }

    /**
     * MsgHeaderを生成する。
     * sendまたはreplay時に呼ばれる。
     * 
     * @param isSend sendの時true
     * @param sessionId sessionId
     * @param srcPeer src peer locator
     * @param dstPeer dst peer locator
     * @param timestamp timestamp
     */
    MsgHeader(boolean isSend, int sessionId,
            PeerLocator srcPeer, PeerLocator dstPeer, long timestamp) {
        msgMagic = ConfigValues.MSG_MAGIC;
        this.sessionId = isSend? sessionId : -sessionId;
        this.srcPeer = srcPeer;
        this.dstPeer = dstPeer;
        this.timestamp = timestamp;
    }

    boolean isSend() {
        return (sessionId >= 0);
    }
    
    int getSessionId() {
        return Math.abs(sessionId);
    }

    @Override
    public String toString() {
        return String.format("[magic=%d sessId=%d src=%s dst=%s timestamp=%tT]", 
                msgMagic, sessionId, srcPeer, dstPeer, timestamp);
    }
}
