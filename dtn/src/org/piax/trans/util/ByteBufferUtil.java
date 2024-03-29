/*
 * ByteBufferUtil.java
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
 * 2009/11/26 designed and implemented by M. Yoshida.
 * 
 * $Id: ByteBufferUtil.java 183 2010-03-03 11:41:21Z yos $
 */
package org.piax.trans.util;

import java.nio.ByteBuffer;
import java.nio.InvalidMarkException;

/**
 * Messaging Framework で使用するByteBufferに関するユーティティクラス。
 * <p>
 * Messaging Framework では、先頭部分に補助情報を付与することを行う。
 * このため、ByteBufferを割り当てる際には、先頭部分に空きを設けて、途中の
 * offsetから使用することを行う。
 * <p>
 * ByteBuffer では、position=0 が先頭バイトとなることを仮定してメソッドが
 * 用意されている。（例：clear(), flip(), rewind()）
 * ここでは、先頭バイトを指すindexとしてmarkを使用する。既存のメソッドに
 * 代わるものとして、positionを0にする代わりに、mark値にセットする、
 * clear(), flip(), rewind() を用意する。 
 * 
 * @author     Mikio Yoshida
 * @version    2.2.0
 */
public class ByteBufferUtil {
    static final int DEFAULT_HEAD_MARGIN = 200;
    static final int DEFAULT_ENHANCE_SIZE = 200;
    
    /**
     * 指定されたByteBufferをclearする。
     * positionはmarkの値に、limitはcapacityの値にセットされる。
     * 
     * @param b 対象となるByteBuffer
     * @throws  InvalidMarkException markがセットされてなかった場合
     */
    public static void clear(ByteBuffer b) {
        b.reset();      // position = mark
        b.limit(b.capacity());
    }

    /**
     * 指定されたByteBufferを反転する。
     * limitはpositionの値に、positionはmarkの値にセットされる。
     * 
     * @param b 対象となるByteBuffer
     * @throws  InvalidMarkException markがセットされてなかった場合
     */
    public static void flip(ByteBuffer b) {
        b.limit(b.position());
        b.reset();      // position = mark
    }

    /**
     * 指定されたByteBufferを巻き戻す。
     * positionはmarkの値にセットされる。
     * 
     * @param b 対象となるByteBuffer
     * @throws  InvalidMarkException markがセットされてなかった場合
     */
    public static void rewind(ByteBuffer b) {
        b.reset();      // position = mark
    }
    
    /**
     * 指定されたByteBufferの先頭マークを設定する。
     * markはpositionの値にセットされる。
     * 
     * @param b 対象となるByteBuffer
     */
    public static void mark(ByteBuffer b) {
        b.mark();       // mark = position
    }

    /**
     * 指定されたByteBufferの先頭をoffsetとしてresetする。
     * mark, positionともoffset値にセットされる。
     * 
     * @param offset reset時のoffset値
     * @param b 対象となるByteBuffer
     */
    public static void reset(int offset, ByteBuffer b) {
        b.position(offset);
        b.mark();       // mark = position
    }

    /**
     * 指定されたmarginと容量capacityを持つByteBufferを生成する。
     * 
     * @param margin 先頭部の余白
     * @param capacity 容量
     * @return 新規に生成されたByteBuffer
     */
    public static ByteBuffer newByteBuffer(int margin, int capacity) {
        ByteBuffer b = ByteBuffer.wrap(new byte[margin + capacity]);
        b.position(margin);
        b.mark();       // set mark to HEAD_MARGIN
        return b;
    }
    
    /**
     * 指定された容量capacityを持つByteBufferを生成する。
     * 
     * @param capacity 容量
     * @return 新規に生成されたByteBuffer
     */
    public static ByteBuffer newByteBuffer(int capacity) {
        return newByteBuffer(0, capacity);
    }

    /**
     * 指定されたbyte列を指定されたByteBufferの指定位置にコピーする。
     * 
     * @param src コピー元のbyte配列
     * @param srcOff コピー元となるbyte配列のoffset
     * @param srcLen コピー元となるbyte列の長さ
     * @param dst コピー先のByteBuffer
     * @param dstOff コピー先のoffset
     * @throws BufferOverflowException 
     *          ByteBuffer内に残っている容量が不足している場合
     * @throws IndexOutOfBoundsException
     *          srcOffとsrcLengthパラメータの前提条件が満たされていない場合 
     */
    public static void copy2Buffer(byte[] src, int srcOff, int srcLen,
            ByteBuffer dst, int dstOff) {
        dst.reset();    // position <- mark
        dst.position(dst.position() + dstOff);
        dst.put(src, srcOff, srcLen);
        dst.reset();
    }

    public static ByteBuffer byte2Buffer(int margin, byte[] buf, int offset,
            int len) {
        ByteBuffer b = newByteBuffer(margin, len);
        b.put(buf, offset, len);
        rewind(b);
        return b;
    }

    /**
     * 指定されたbyte列を値として持つByteBufferを生成する。
     * 生成されたByteBufferの先頭には、DEFAULT_HEAD_MARGINで指定されたbyte数の
     * 領域が確保される。
     * 
     * @param data byte列全体を格納するbyte配列
     * @return 新規に生成されたByteBuffer
     */
    public static ByteBuffer byte2Buffer(byte[] data) {
        return byte2Buffer(DEFAULT_HEAD_MARGIN, data, 0, data.length);
    }

    /**
     * 指定されたbyte列を値として持つByteBufferを生成する。
     * 生成されたByteBufferの先頭には、DEFAULT_HEAD_MARGINで指定されたbyte数の
     * 領域が確保される。
     * 
     * @param buf byte列を格納するbyte配列
     * @param offset 格納するbyte列の先頭を示すoffset
     * @param len 格納するbyte列の長さ
     * @return 新規に生成したByteBuffer
     */
    public static ByteBuffer byte2Buffer(byte[] buf, int offset, int len) {
        return byte2Buffer(DEFAULT_HEAD_MARGIN, buf, offset, len);
    }

    /**
     * ByteBufferのpositionからlimitで指定されたbyte列を新規のbyte配列として返す。
     * 尚、この操作によって、ByteBufferのpositionは変化しない。
     * 
     * @param bbuf 対象となるByteBuffer
     * @return bbufによって指定されるbyte列を値として持つbyte配列
     */
    public static byte[] buffer2Bytes(ByteBuffer bbuf) {
        byte[] b = new byte[bbuf.remaining()];
        int pos = bbuf.position();
        bbuf.get(b);
        bbuf.position(pos);     // rewind position
        return b;
    }
    
    /**
     * ByteBufferのヘッダマージンをsizeのbyte分だけ拡張する。
     * 
     * @param size 拡張するbyte数
     * @param bbuf 対象となるByteBuffer
     * @return ヘッダマージンを拡張したByteBuffer
     */
    private static ByteBuffer enhance(int size, ByteBuffer bbuf) {
        ByteBuffer bbuf2 = ByteBuffer.wrap(new byte[bbuf.capacity() + size]);
        bbuf2.limit(bbuf.limit() + size);
        bbuf2.position(bbuf.position() + size);
        bbuf2.mark();
        bbuf2.put(bbuf);
        rewind(bbuf2);
        return bbuf2;
    }

    /**
     * ByteBufferのbodyをsizeのbyte分だけ拡張する。
     * 
     * @param bbuf 対象となるByteBuffer
     * @param size 拡張するbyte数
     * @return bodyを拡張したByteBuffer
     */
    private static ByteBuffer enhance(ByteBuffer bbuf, int size) {
        byte[] b = new byte[bbuf.capacity() + size];
        System.arraycopy(bbuf.array(), 0, b, 0, bbuf.limit());
        ByteBuffer bbuf2 = ByteBuffer.wrap(b);
        bbuf2.limit(bbuf.limit() + size);
        // save current position and mark
        int pos = bbuf.position();
        bbuf.reset();
        int mark = bbuf.position();
        // restore to bbuf2
        bbuf2.position(mark);
        bbuf2.mark();
        bbuf2.position(pos);
        return bbuf2;
    }

    /**
     * bbufの持つbyte列の前方にpreLenで指定されたbyte長だけ領域確保する。
     * bbufの先頭に十分なマージンがない場合は、bbufが拡張されて後、結合を行う。
     * 
     * @param preLen 前方に確保する領域のbyte長
     * @param bbuf 対象となるByteBuffer
     * @return 確保後のByteBuffer
     */
    public static ByteBuffer reserve(int preLen, ByteBuffer bbuf) {
        if (preLen == 0) return bbuf;
        if (preLen > bbuf.position()) {
            // enhance the ByteBuffer
            int esize = DEFAULT_ENHANCE_SIZE + preLen - bbuf.position();
            bbuf = enhance(esize, bbuf);
        }
        bbuf.position(bbuf.position() - preLen);
        bbuf.mark();
        return bbuf;
    }
    
    /**
     * bbufの持つbyte列の先頭にpreで指定されたbyte列を結合する。
     * bbufの先頭に十分なマージンがない場合は、bbufが拡張されて後、結合を行う。
     * ByteBufferのpositionとmarkは、結合後のpreを含めたbyte列の先頭にセットされる。
     * 
     * @param pre 先頭に結合するbyte列
     * @param bbuf 対象となるByteBuffer
     * @return 結合後のByteBuffer
     */
    public static ByteBuffer concat(byte[] pre, ByteBuffer bbuf) {
        ByteBuffer b = reserve(pre.length, bbuf);
        b.put(pre);
        rewind(b);
        return b;
    }

    /**
     * bbufにpostで指定されたbyte列を追加する。
     * bbufに空き領域がない場合、ByteBufferの領域が拡張され、返り値には、
     * 領域拡張されたByteBufferが返る。
     * 
     * @param bbuf ByteBuffer
     * @param post 追加するbyte列
     * @return post追加後のByteBuffer
     */
    public static ByteBuffer put(ByteBuffer bbuf, byte[] post) {
        ByteBuffer b = bbuf;
        if (post.length == 0) return b;
        if (post.length > bbuf.remaining()) {
            // enhance the ByteBuffer
            int esize = DEFAULT_ENHANCE_SIZE + post.length - bbuf.remaining();
            b = enhance(bbuf, esize);
        }
        b.put(post);
        return b;
    }

    /**
     * bbufの持つbyte列から先頭部preLen分のbyte列を削る。
     * 
     * @param preLen 先頭部から削るbyte数
     * @param bbuf 対象となるByteBuffer
     * @throws IllegalArgumentException 
     *          保持するbyte列より削るbyte数が同じであるか大きい場合
     */
    public static void strip(int preLen, ByteBuffer bbuf) {
        if (preLen == 0) return;
        if (preLen >= bbuf.remaining())
            throw new IllegalArgumentException();
        bbuf.position(bbuf.position() + preLen);
        bbuf.mark();
    }

    /**
     * bbufの持つbyte列の先頭が、prefixと等しいか判定する。
     * 
     * @param prefix 比較するbyte列
     * @param bbuf 対象となるByteBuffer
     * @return 先頭がprefixと等しい場合true、そうでない場合false
     */
    public static boolean startsWith(byte[] prefix, ByteBuffer bbuf) {
        if (bbuf.remaining() < prefix.length)
            return false;
        int p = bbuf.position();
        for (int i = 0; i < prefix.length; i++) {
            if (prefix[i] != bbuf.get(p + i)) 
                return false;
        }
        return true;
    }
}
