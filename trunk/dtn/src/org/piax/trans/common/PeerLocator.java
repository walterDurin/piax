/*
 * PeerLocator.java
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
 * 2007/10/03 designed and implemented by M. Yoshida.
 * 
 * $Id: PeerLocator.java 218 2010-05-19 03:51:55Z teranisi $
 */

package org.piax.trans.common;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collection;

import org.piax.trans.msgframe.MessageReachable;
import org.piax.trans.ts.BytesReceiver;
import org.piax.trans.ts.LocatorTransportSpi;
import org.piax.trans.ts.bluetooth.BluetoothLocator;
import org.piax.trans.ts.emu.EmuLocator;
import org.piax.trans.ts.nfc.NfcLocator;
import org.piax.trans.ts.tcp.TcpLocator;
import org.piax.trans.ts.tinyudp.TinyUdpLocator;
import org.piax.trans.ts.udp.UdpLocator;
import org.piax.trans.ts.udpx.UdpXLocator;
import org.piax.trans.util.AddressUtil;

/**
 * ピアのlocatorを示す抽象クラスを定義する。
 * 
 * @author     Mikio Yoshida
 * @version    2.1.0
 */
public abstract class PeerLocator implements Serializable, MessageReachable {
    private static final long serialVersionUID = 6865339166154187882L;

    private static final byte[] nullAddr = new byte[] {0,0,0,0,0,0};

    /**
     * NATをだますために、IPアドレスについてはビット反転させる。
     * @param ip
     */
    private static void xor(byte[] ip) {
        for (int i = 0; i < ip.length; i++) {
            ip[i] = (byte)~ip[i];
        }
    }
    
    public static void putAddr(ByteBuffer bbuf, InetSocketAddress addr) {
        if (addr == null) {
            bbuf.put(nullAddr);
        } else {
            byte[] ip = addr.getAddress().getAddress();
            xor(ip);
            bbuf.put(ip);
            bbuf.putShort((short) addr.getPort());
        }
    }

    public static InetSocketAddress getAddr(ByteBuffer bbuf) 
    throws UnknownHostException {
        byte[] ip = new byte[4];
        int port;
        InetSocketAddress addr = null;
        bbuf.get(ip);
        xor(ip);
        port = (bbuf.getShort() & 0xffff);
        
        if (port != 0) {
            addr = AddressUtil.getAddress(ip, port);
        }
        return addr;
    }

    public static PeerLocator create (ServiceInfo info) {
        if (info.getInetSocketAddress() != null) {
            try {
                Class clazz = Class.forName(info.getType());
                Object[] args = new Object[1];
                args[0] = info.getInetSocketAddress();
                Class[] argTypes = new Class[args.length];
                for (int i = 0; i < args.length; i++) {
                    argTypes[i] = args[i].getClass();
                }
                return (PeerLocator) clazz.getConstructor(argTypes).newInstance(args);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        else {
            try {
                Class clazz = Class.forName(info.getType());
                Object[] args = new Object[1];
                args[0] = info.getHost();
                Class[] argTypes = new Class[args.length];
                for (int i = 0; i < args.length; i++) {
                    argTypes[i] = args[i].getClass();
                }
                return (PeerLocator) clazz.getConstructor(argTypes).newInstance(args);   
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public PeerLocator() {}
    public PeerLocator(ByteBuffer bbuf) {}
    
    /**
     * このピアlocatorを使った通信をサポートするLocatorTransportサービスを
     * 生成する。
     * </p>
     * bytesReceiverにはLocatorTransportサービスが受信したバイト列を受け取る
     * 上位層のオブジェクトを指定する。
     * 通常は、LocatorTransportオブジェクトであるが、他のLocatorTransportサービス
     * を指定してもよい。
     * PeerLocatorオブジェクトから関連するLocatorTransportサービスを複数生成
     * することはAPI上可能であるが、LocatorTransportオブジェクトには、
     * PeerLocator型1つにつき、LocatorTransportサービスを1つしか登録できない。
     * このため、PeerLocatorオブジェクトに対し、実質的に1つの
     * LocatorTransportサービスが機能することとなる。
     * 
     * @param bytesReceiver BytesReceiverオブジェクト
     * @param relays 関連するリレーピアのlocatorから成るリスト
     * @return このピアlocatorを使った通信をサポートするLocatorTransportサービス
     * @throws IOException I/O関連の例外が発生した時
     */
    public abstract LocatorTransportSpi newLocatorTransportService(
            BytesReceiver bytesReceiver,
            Collection<PeerLocator> relays) throws IOException;
    
    /**
     * Gets a compact string for the use of the peer name.
     * The string must be unique in this machine scope and 
     * can be used as a directory name.
     * 
     * @return a compact string for the use of the peer name.
     */
    public abstract String getPeerNameCandidate();
    
    /**
     * targetに指定されたPeerLocatorオブジェクトと同一のクラスであるときに
     * trueを返す。
     * 
     * @param target 比較対象となるPeerLocatorオブジェクト
     * @return targetが同じクラスであるときtrue
     */
    public abstract boolean sameClass(PeerLocator target);
    
    public abstract byte getId();
    
    public abstract void pack(ByteBuffer bbuf);
    
    public abstract int getPackLen();

    public abstract ServiceInfo getServiceInfo();

    public boolean immediateLink() {
        return true;
    }
    
    public static PeerLocator unpack(byte locatorId, ByteBuffer bbuf) {
        try {
            switch (locatorId) {
            case UdpLocator.ID:
                return new UdpLocator(bbuf);
            case TcpLocator.ID:
                return new TcpLocator(bbuf);
            case UdpXLocator.ID:
                return new UdpXLocator(bbuf);
            case EmuLocator.ID:
                return new EmuLocator(bbuf);
            case TinyUdpLocator.ID:
                return new TinyUdpLocator(bbuf);
            case BluetoothLocator.ID:
                return new BluetoothLocator(bbuf);
            case NfcLocator.ID:
                return new NfcLocator(bbuf);
            }
        } catch (Exception e) {
        }
        return null;
    }
}
