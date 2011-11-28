/*
 * UdpTransportService.java
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
 * 2009/02/04 designed and implemented by M. Yoshida.
 * 
 * $Id: TinyUdpTransportService.java 290 2010-10-05 05:58:57Z teranisi $
 */

package org.piax.trans.ts.tinyudp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;

import org.grlea.log.SimpleLogger;
import org.piax.trans.common.PeerLocator;
import org.piax.trans.ts.BytesReceiver;
import org.piax.trans.ts.InetLocator;
import org.piax.trans.ts.LocatorTransportSpi;
import org.piax.trans.ts.tcp.TcpLocator;
import org.piax.trans.util.ByteBufferUtil;

/**
 * 簡易なUDP用LocatorTransportサービスを実現するクラス。
 * IPフラグメンテーションを抑制するためのMTU以下へのパケット分割の処理は
 * 行っていない。
 * <p>
 * PeerLocatorとLocatorTransportサービスの実装方法を示すサンプルコードとして
 * 提供したもの。
 * 
 * @author     Mikio Yoshida
 * @version    2.1.0
 */
public class TinyUdpTransportService implements LocatorTransportSpi {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(TinyUdpTransportService.class);

    public static int MAX_PAYLOAD_SIZE = 65535 - 20 - 8;
    
    class Listener extends Thread {
        private final byte[] in = new byte[MAX_PAYLOAD_SIZE];
        private DatagramPacket inPac = new DatagramPacket(in, in.length);

        @Override
        public void run() {
            while (!isTerminated) {
                try {
                    udpSoc.receive(inPac);

                    int len = inPac.getLength();
                    ByteBuffer b = ByteBufferUtil.byte2Buffer(in, 0, len);
//                    byte[] data = new byte[len];
//                    System.arraycopy(in, 0, data, 0, len);
                    receiveBytes(b);
                } catch (IOException e) {
                    if (isTerminated) break;  // means shutdown
                    log.warnException(e);      // temp I/O error
                }
            }
        }
    }

    private final BytesReceiver bytesReceiver;
    private TinyUdpLocator peerLocator;
    private final DatagramSocket udpSoc;
    private final Listener listener;
    private volatile boolean isTerminated;
    
    public TinyUdpTransportService(BytesReceiver bytesReceiver,
            TinyUdpLocator peerLocator) throws IOException {
        this.bytesReceiver = bytesReceiver;
        this.peerLocator = peerLocator;
        // create socket
        int port = peerLocator.getPort();
        udpSoc = new DatagramSocket(port);
        
        // start lister thread
        isTerminated = false;
        listener = new Listener();
        listener.start();
    }

    /* (non-Javadoc)
     * @see org.piax.trans.spi.LocatorTransportSpi#getLocator()
     */
    public PeerLocator getLocator() {
        return peerLocator;
    }

    public boolean canSend(PeerLocator target) {
        return peerLocator.sameClass(target);
    }

    /* (non-Javadoc)
     * @see org.piax.trans.spi.LocatorTransportSpi#fin()
     */
    public void fin() {
        if (isTerminated) {
            return;     // already terminated
        }
        
        // terminate the listener thread
        isTerminated = true;
        udpSoc.close();
        try {
            listener.join();
        } catch (InterruptedException e) {}
    }
    
    void receiveBytes(ByteBuffer msg) {
        bytesReceiver.receiveBytes(this, msg);
    }

    public void sendBytes(boolean isSend, PeerLocator toPeer, ByteBuffer msg) 
    throws IOException {
        if (msg.remaining() > MAX_PAYLOAD_SIZE) {
            log.error("send data over MAX_PAYLOAD_SIZE:" + msg.remaining() + "bytes");
            return;
        }
        // send
        byte[] b = msg.array();
        int off = msg.arrayOffset() + msg.position();
        udpSoc.send(new DatagramPacket(
                b, off, msg.remaining(), ((InetLocator) toPeer).getSocketAddress()));
    }

	public void setLocator(PeerLocator locator) {
		// TODO Auto-generated method stub
		peerLocator = (TinyUdpLocator) locator;
	}

	public boolean canSet(PeerLocator target) {
		// TODO Auto-generated method stub
		return target instanceof TinyUdpLocator && ((InetLocator) target).getPort() == peerLocator.getPort();
	}
}
