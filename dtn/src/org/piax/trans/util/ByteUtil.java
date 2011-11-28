/*
 * ByteUtil.java
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
 * 2007/01/12 designed and implemented by M. Yoshida.
 * 
 * $Id: ByteUtil.java 183 2010-03-03 11:41:21Z yos $
 */
package org.piax.trans.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;

//-- I am waiting for someone to translate the following doc into English. :-)

/**
 * byte配列を処理するためのユーティリティ。
 * <p>
 * Javaのクラスライブラリには、byte配列を扱うものが少ないので、自分で作りました。
 * 必要に応じて、機能を足しているので、次第にメソッドが増えていきます。
 * <p>
 * あくまでも、piaxの中で内部処理を分かって用いられることを前提にしているため、
 * 引数チェックはしていません。
 * <p>
 * メモ：<br>
 * 可変長byte配列を扱うクラスライブラリが望まれる。
 * <code>ByteArrayOutputStream</code> をこの用途で使っている箇所もあるが、
 * <code>ByteArrayOutputStream</code> が行う処理は byte配列を頻繁にreallocateしていて
 * 必ずしも効率の良いコードになっていない。
 * 効率化のためには、apache commons-io の
 * <code>org.apache.commons.io.output.ByteArrayOutputStream</code> を使うように
 * 変更する（または自分で実装する）必要が出てくるかもしれない。
 * 
 * @author     Mikio Yoshida
 * @version    1.1.0
 */
public class ByteUtil {
    public static boolean isEqual(byte[] a, byte[] b) {
        if (a.length != b.length) 
            return false;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }
    
    public static int reverse32(int baseno) {
        int result = 0;
        byte[] l = new byte[4];
        l[0] = (byte)  (baseno         & 0xffL);
        l[1] = (byte) ((baseno >>  8 ) & 0xffL);
        l[2] = (byte) ((baseno >> 16 ) & 0xffL);
        l[3] = (byte) ((baseno >> 24 ) & 0xffL);

        for (int i=0; i<l.length; i++) {
            l[i] = reverse8(l[i]);
        }

        result = l[0];
        result = ((result << 8) | ((int)l[1] & 0xff));
        result = ((result << 8) | ((int)l[2] & 0xff));
        result = ((result << 8) | ((int)l[3] & 0xff));

        return result;
    }

    public static byte reverse8(byte v) {
        byte result = 0;
        result |= (v & 0x01) << 7;
        result |= (v & 0x02) << 5;
        result |= (v & 0x04) << 3;
        result |= (v & 0x08) << 1;
        result |= (v & 0x10) >> 1;
        result |= (v & 0x20) >> 3;
        result |= (v & 0x40) >> 5;
        result |= (v & 0x80) >> 7;
        return result;
    }

    public static int bytes2Int(final byte[] b) {
        return bytes2Int(b, 0);
    }
    
    public static int bytes2Int(final byte[] b, int off) {
        return ((b[off] & 0xff) << 24) 
                | ((b[off + 1] & 0xff) << 16)
                | ((b[off + 2] & 0xff) << 8) 
                | (b[off + 3] & 0xff);
    }
    
    public static byte[] int2bytes(int x) {
        byte[] b = new byte[4];
        b[3] = (byte) (x & 0xff);
        x >>= 8; b[2] = (byte) (x & 0xff);
        x >>= 8; b[1] = (byte) (x & 0xff);
        x >>= 8; b[0] = (byte) (x & 0xff);
        return b;
    }

    public static long bytes2Long(final byte[] b) {
        return bytes2Long(b, 0);
    }

    public static long bytes2Long(final byte[] b, int off) {
        long l1 = bytes2Int(b, off);
        long l0 = bytes2Int(b, off + 4) & 0xffffffffL;
        return (l1 << 32) | l0;
    }

    public static byte[] long2bytes(long x) {
        byte[] b = new byte[8];
        b[7] = (byte) (x & 0xff);
        x >>= 8; b[6] = (byte) (x & 0xff);
        x >>= 8; b[5] = (byte) (x & 0xff);
        x >>= 8; b[4] = (byte) (x & 0xff);
        x >>= 8; b[3] = (byte) (x & 0xff);
        x >>= 8; b[2] = (byte) (x & 0xff);
        x >>= 8; b[1] = (byte) (x & 0xff);
        x >>= 8; b[0] = (byte) (x & 0xff);
        return b;
    }

    public static byte[] stream2Bytes(InputStream in) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(in.available());
        byte[] b = new byte[in.available()];
        try {
            int len;
            while ((len = in.read(b)) != -1) {
                bout.write(b, 0, len);
            }
        } finally {
            try {
                in.close();
            } catch (IOException e) {}
        }
        return bout.toByteArray();
    }

    public static byte[] file2Bytes(File file) 
            throws FileNotFoundException, IOException {
        InputStream fin = new FileInputStream(file);
        return stream2Bytes(fin);
    }

    public static byte[] url2Bytes(URL url) 
            throws FileNotFoundException, IOException {
        File file;
        try {
            file = new File(url.toURI());
        } catch (URISyntaxException e) {
            throw new FileNotFoundException("URL syntax error");
        }
        return file2Bytes(file);
    }
    
    public static void bytes2Stream(final byte[] b, OutputStream out)
            throws IOException {
        try {
            out.write(b);
            out.flush();
        } finally {
            try {
                out.close();
            } catch (IOException e) {}
        }
    }

    public static void bytes2File(final byte[] b, File file)
    throws FileNotFoundException, IOException {
        OutputStream out = new FileOutputStream(file);
        bytes2Stream(b, out);
    }
    
    /*
     * 必要に迫られ作ったが、eolの指定の部分が汎用性に欠ける。
     */
    @Deprecated
    public static byte[] bytes2HexBytes(final byte[] b, byte eol) {
        byte[] hex = new byte[b.length * 2 + 1];
        
        int j = 0;
        for (int i = 0; i < b.length; i++) {
            int _b = ((int) b[i]) & 0xff;
            hex[j++] = (byte) Character.forDigit(_b / 16, 16);
            hex[j++] = (byte) Character.forDigit(_b % 16, 16);
        }
        hex[j] = eol;
        return hex;
    }
    
    public static String bytes2Hex(byte[] b, int offset, int len) {
        StringBuilder str = new StringBuilder();
        
        for (int i = offset; i < offset+len; i++) {
            int _b = ((int) b[i]) & 0xff;
            char upper = Character.forDigit(_b / 16, 16);
            char lower = Character.forDigit(_b % 16, 16);
            str.append(upper);
            str.append(lower);
        }
        return str.toString();
    }

    public static String bytes2Hex(final byte[] b, int len) {
        StringBuilder str = new StringBuilder();
        
        for (int i = 0; i < len; i++) {
            int _b = ((int) b[i]) & 0xff;
            char upper = Character.forDigit(_b / 16, 16);
            char lower = Character.forDigit(_b % 16, 16);
            str.append(upper);
            str.append(lower);
        }
        return str.toString();
    }

    public static String bytes2Hex(final byte[] b) {
        return bytes2Hex(b, b.length);
    }

    public static byte[] hexBytes2Bytes(byte[] hex) {
        byte[] b = new byte[hex.length / 2];
        
        int j = 0;
        for (int i = 0; i < b.length; i++) {
            int upper = Character.digit(hex[j++], 16);
            int lower = Character.digit(hex[j++], 16);
            b[i] = (byte) (16 * upper + lower);
        }
        return b;
    }
    
    public static byte[] hex2Bytes(String s) throws IllegalArgumentException {
        byte[] b = new byte[s.length() / 2];
        
        for (int i = 0; i < b.length; i++) {
            String ss = null;
            try {
                ss = s.substring(i * 2, i * 2 + 2);
                // as byte is signed use int
                b[i] = (byte) Integer.parseInt(ss, 16);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException();
            } catch (IndexOutOfBoundsException e) {
                throw new IllegalArgumentException();
            }
        }
        return b;
    }
    
    /**
     * byte列をダンプ用の文字列に変換する。
     * <p>
     * 0x20-0x7e はACSII文字として出力し、
     * 他のbyte値については、16進表現+'.' を出力する。(例：".8f")
     * 
     * @param b byte列
     * @param offset offset
     * @param len 長さ
     * @return ダンプ用文字列
     */
    public static String dumpBytes(byte[] b, int offset, int len) {
        StringBuilder str = new StringBuilder();
        
        for (int i = offset; i < offset + len; i++) {
            int _b = ((int) b[i]) & 0xff;
            if (_b >= 0x20 && _b <= 0x7e) {
                str.append((char) _b);
            } else {
                char upper = Character.forDigit(_b / 16, 16);
                char lower = Character.forDigit(_b % 16, 16);
                str.append('.');
                str.append(upper);
                str.append(lower);
            }
        }
        return str.toString();
    }
    
    public static String dumpBytes(byte[] b) {
        return dumpBytes(b, 0, b.length);
    }
    
    public static String dumpBytes(ByteBuffer bbuf) {
        byte[] b = bbuf.array();
        int off = bbuf.arrayOffset() + bbuf.position();
        return dumpBytes(b, off, bbuf.remaining());
    }

    public static boolean startsWith(byte[] b, byte[] prefix) {
        if (b.length < prefix.length)
            return false;
        for (int i = 0; i < prefix.length; i++) {
            if (prefix[i] != b[i]) 
                return false;
        }
        return true;
    }

    public static int indexOf(byte[] b, byte x) {
        for (int i = 0; i < b.length; i++) {
            if (b[i] == x) return i;
        }
        return -1;
    }
    
    /**
     * 与えられたbyte配列を指定されたbyte値が出現しないようにencodeする。
     * <p>
     * 元のbyte配列に、指定されたbyte値が出現した場合、1byte目をescape用の
     * byte, 2byte目を対応するescape codeとする2byteの列に置き換える。
     * <p>
     * 引数 escapeAndElimBytes の1byte目にはescape用のbyte, 2byte目以降に
     * 出現を抑制したいbyte値（複数可）をセットする。
     * 
     * @param b ソースとなるbyte配列
     * @param len ソースとなるbyte配列の長さ
     * @param buf 変換後のbyte配列を入れるバッファ
     * @param escapeAndElimBytes escape用のbyteと出現を抑制したいbyte値（複数可）
     * @return 変換後のbyte配列の長さ
     * @throws ArrayIndexOutOfBoundsException 
     *          バッファが十分な容量を持たない場合
     */
    public static int encode4escape(final byte[] b, final int len, 
            byte[] buf, final byte[] escapeAndElimBytes) 
            throws ArrayIndexOutOfBoundsException {
        byte escape = escapeAndElimBytes[0];
        byte[] escapeCodes = new byte[escapeAndElimBytes.length];
        
        // decides elimBytesCodes
        byte ecode = 1;
        for (int i = 0; i < escapeAndElimBytes.length; i++) {
            while (indexOf(escapeAndElimBytes, ecode) >= 0) ++ecode;
            escapeCodes[i] = ecode++;
        }
        int bufIx = 0;
        for (int i = 0; i < len; i++) {
            int ix = indexOf(escapeAndElimBytes, b[i]);
            if (ix == -1) {
                // no match
                buf[bufIx++] = b[i];
            } else {
                // match
                buf[bufIx++] = escape;
                buf[bufIx++] = escapeCodes[ix];
            }
        }
        return bufIx;
    }

    /**
     * <code>encode4escape</code>メソッドによってencodeされたbyte配列を
     * 元のbyte配列にdecodeする。
     * <p>
     * decode処理の場合に限り、ソースとして指定する配列を変換後のバッファと
     * して用いる配列に指定してもよい。
     * 
     * @param b ソースとなるbyte配列
     * @param len ソースとなるbyte配列の長さ
     * @param buf 変換後のbyte配列を入れるバッファ
     * @param escapeAndElimBytes escape用のbyteと出現を抑制したいbyte値（複数可）
     * @return 変換後のbyte配列の長さ
     * @throws IllegalArgumentException 
     *          ソースとなるbyte配列が正しくencodeされたものでない場合
     * @throws ArrayIndexOutOfBoundsException 
     *          バッファが十分な容量を持たない場合
     */
    public static int decode4escape(byte[] b, final int len,
            byte[] buf, final byte[] escapeAndElimBytes) 
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        byte escape = escapeAndElimBytes[0];
        byte[] escapeCodes = new byte[escapeAndElimBytes.length];
        
        // decides elimBytesCodes
        byte ecode = 1;
        for (int i = 0; i < escapeAndElimBytes.length; i++) {
            while (indexOf(escapeAndElimBytes, ecode) >= 0) ++ecode;
            escapeCodes[i] = ecode++;
        }
        int bufIx = 0;
        for (int i = 0; i < len; i++) {
            if (b[i] != escape) {
                // no match
                buf[bufIx++] = b[i];
            } else {
                // match
                int ix = indexOf(escapeCodes, b[++i]);
                if (ix == -1)
                    throw new IllegalArgumentException("invalid source bytes");
                buf[bufIx++] = escapeAndElimBytes[ix];
            }
        }
        return bufIx;
    }
}
