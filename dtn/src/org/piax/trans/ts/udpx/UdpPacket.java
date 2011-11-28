/*
 * UdpPacket.java
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
 * 2007/11/11 designed and implemented by M. Yoshida.
 * 
 * $Id: UdpPacket.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans.ts.udpx;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;


/**
 * UDP Packet を定義するクラス。
 * パケットは、headerとbodyに分かれる。
 * header には次のような制御情報が含まれる。
 * <ol>
 * <li>type packetを分類するための型
 * <li>msgId メッセージに振られる番号
 * <li>srcAddr 送信元アドレス
 * <li>dstAddr 送信先アドレス
 * <li>seq フラグメントパケットに振られる番号
 * </ol>
 * seqには、1から始まる整数が順々に振られる。最後のパケットは負の数となる。
 * （例：1,2,3,-4）
 * </p>
 * 現コードは、IPv6対応していない。
 * 
 * @author     Mikio Yoshida
 * @version    2.0.1
 */
class UdpPacket {
    private static final byte[] nullAddr = new byte[] {0,0,0,0,0,0};

    static final byte KEEPALIVE_REQ = (byte) 0x01;
    static final byte KEEPALIVE_ACK = (byte) 0x02;
    static final byte NAT_ADDR_REQ = (byte) 0x03;
    static final byte NAT_ADDR_ACK = (byte) 0x04;
    static final byte CONN_REVERSAL_REQ = (byte) 0xf1;
    static final byte CONN_REVERSAL_ACK = (byte) 0xf2;
    static final byte CONN_REVERSAL_DUM = (byte) 0xf3;
    static final byte SEND_MSG_TYPE = (byte) 0x81;
    static final byte REPLY_MSG_TYPE = (byte) 0x82;

    /**
     * NATをだますために、IPアドレスについてはビット反転させる。
     * @param ip
     */
    private static void xor(byte[] ip) {
        for (int i = 0; i < ip.length; i++) {
            ip[i] = (byte)~ip[i];
        }
    }
    
    private static void putAddr(ByteBuffer bbuf, InetSocketAddress addr) {
        if (addr == null) {
            bbuf.put(nullAddr);
        } else {
            byte[] ip = addr.getAddress().getAddress();
            xor(ip);
            bbuf.put(ip);
            bbuf.putShort((short) addr.getPort());
        }
    }

    private static InetSocketAddress getAddr(ByteBuffer bbuf) {
        byte[] ip = new byte[4];
        int port;
        InetSocketAddress addr = null;
        bbuf.get(ip);
        xor(ip);
        port = (bbuf.getShort() & 0xffff);
        if (port != 0) {
            try {
                addr = new InetSocketAddress(
                        InetAddress.getByAddress(ip), port);
            } catch (UnknownHostException e) {}
        }
        return addr;
    }
    
    private static void putInetXLocator(ByteBuffer bbuf, UdpXLocator xloc) {
        putAddr(bbuf, xloc.global);
        putAddr(bbuf, xloc.global2);
        putAddr(bbuf, xloc.nat);
        putAddr(bbuf, xloc.privateAddr);
    }

    private static UdpXLocator getInetXLocator(ByteBuffer bbuf) {
        UdpXLocator xloc = new UdpXLocator();
        
        xloc.global = getAddr(bbuf);
        xloc.global2 = getAddr(bbuf);
        xloc.nat = getAddr(bbuf);
        xloc.privateAddr = getAddr(bbuf);
        return xloc;
    }
    
    static final boolean isControl(ByteBuffer bbuf) {
        return (bbuf.get(bbuf.position()) & 0xf0) == 0;
    }

    static final boolean isType(ByteBuffer bbuf, byte ctrl) {
        return bbuf.get(bbuf.position()) == ctrl;
    }

    static final ByteBuffer newControlReq(byte type) {
        ByteBuffer bbuf = ByteBuffer.allocate(1);
        bbuf.put(type);
        bbuf.flip();
        return bbuf;
    }

    static final ByteBuffer newNATAddrAck(InetSocketAddress nat) {
        ByteBuffer bbuf = ByteBuffer.allocate(1 + 6);
        bbuf.put(NAT_ADDR_ACK);
        putAddr(bbuf, nat);
        bbuf.flip();
        return bbuf;
    }
    
    static final ByteBuffer newKeepAliveReq(InetSocketAddress nat) {
        ByteBuffer bbuf = ByteBuffer.allocate(1 + 6);
        bbuf.put(KEEPALIVE_REQ);
        putAddr(bbuf, nat);
        bbuf.flip();
        return bbuf;
    }

    static final InetSocketAddress getNatAddr(ByteBuffer bbuf) {
        bbuf.mark();
        bbuf.get();
        InetSocketAddress addr = getAddr(bbuf);
        bbuf.reset();
        return addr;
    }

    static final int headerSize() {
        return 1 + 24 + 24 + 2 + 2; // 53bytes
    }
    
    static UdpXLocator getSrcLocator(ByteBuffer bbuf) {
        bbuf.mark();
        bbuf.position(bbuf.position() + 1);
        UdpXLocator xloc = getInetXLocator(bbuf);
        bbuf.reset();
        return xloc;
    }
    
    static UdpXLocator getDstLocator(ByteBuffer bbuf) {
        bbuf.mark();
        bbuf.position(bbuf.position() + 1 + 24);
        UdpXLocator xloc = getInetXLocator(bbuf);
        bbuf.reset();
        return xloc;
    }

    static UdpPacket getUdpPacket(ByteBuffer bbuf) {
        UdpPacket pac = new UdpPacket(bbuf.remaining() - headerSize());
        bbuf.mark();
        pac.decode(bbuf);
        bbuf.reset();
        return pac;
    }
    
    static ByteBuffer newUdpPacketBuff(byte type, UdpXLocator src, 
            UdpXLocator dst, short seq, short msgid, byte[] body) {
        /*
         * TODO more efficient
         */
        UdpPacket pac = new UdpPacket(type, src, dst, seq, msgid, body);
        ByteBuffer bbuf = ByteBuffer.allocate(pac.size());
        pac.encode(bbuf);
        bbuf.flip();
        return bbuf;
    }
    
    byte type = 0;
    UdpXLocator src = null;
    UdpXLocator dst = null;
    short seq = 0;
    short msgid = 0;
    byte[] body = null;
    int len = 0;
    
    private UdpPacket() {}
    
    private UdpPacket(int bodyLen) {
        body = new byte[bodyLen];
        len = bodyLen;
    }

    private UdpPacket(byte type, UdpXLocator src, 
            UdpXLocator dst, short seq, short msgid, byte[] body) {
        this.type =type;
        this.src = src;
        this.dst = dst;
        this.seq = seq;
        this.msgid = msgid;
        this.body = body;
        this.len = body.length;
    }
    
    final int size() {
        return headerSize() + len;
    }
    
    String getCommonHeader() {
        return String.format("%x-%s:%d", type, src, msgid);
    }

    void encode(ByteBuffer bbuf) throws BufferOverflowException {
        if (bbuf.capacity() < size())
            throw new BufferOverflowException();
        
        //bbuf.clear();
        // set 3 bytes
        bbuf.put(type);
        // set src
        putInetXLocator(bbuf, src);
        // set dst
        putInetXLocator(bbuf, dst);
        // set seq
        bbuf.putShort(seq);
        // set msgid
        bbuf.putShort(msgid);
        // set body
        bbuf.put(body, 0, len);
        //bbuf.flip();
    }
    
    final void decode(ByteBuffer bbuf) throws BufferUnderflowException,
            IndexOutOfBoundsException {
        if (bbuf.remaining() < headerSize())
            throw new BufferUnderflowException();
        if (bbuf.remaining() > headerSize() + body.length)
            throw new BufferOverflowException();
        
        decodeHeader(bbuf);
        decodeBody(bbuf);
    }

    final void decodeHeader(ByteBuffer bbuf) throws BufferUnderflowException {
        if (bbuf.remaining() < headerSize())
            throw new BufferUnderflowException();
        
        type = bbuf.get();
        src = getInetXLocator(bbuf);
        dst = getInetXLocator(bbuf);
        seq = bbuf.getShort();
        msgid = bbuf.getShort();
    }

    final void decodeBody(ByteBuffer bbuf) throws IndexOutOfBoundsException {
        if (bbuf.remaining() > body.length)
            throw new IndexOutOfBoundsException();
        len = bbuf.remaining();
        bbuf.get(body, 0, len);
    }
}
