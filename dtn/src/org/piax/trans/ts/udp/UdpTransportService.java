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
 * $Id: UdpTransportService.java 290 2010-10-05 05:58:57Z teranisi $
 */

package org.piax.trans.ts.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.grlea.log.DebugLevel;
import org.grlea.log.SimpleLogger;
import org.piax.trans.ConfigValues;
import org.piax.trans.common.PeerLocator;
import org.piax.trans.ts.BytesReceiver;
import org.piax.trans.ts.InetLocator;
import org.piax.trans.ts.LocatorTransportSpi;
import org.piax.trans.ts.tcp.TcpLocator;
import org.piax.trans.util.ByteBufferUtil;
import org.piax.trans.util.ByteUtil;

/**
 * UDP用LocatorTransportサービスを実現するクラス。
 * 
 * @author     Mikio Yoshida
 * @version    2.2.0
 */
public class UdpTransportService implements LocatorTransportSpi {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(UdpTransportService.class);

    private static final int MAX_PACKET_DATA_SIZE = 
        ConfigValues.MAX_PACKET_SIZE - Fragments.PACKET_HEADER_SIZE;
    
    class Listener extends Thread {
        private final byte[] in = new byte[ConfigValues.MAX_PACKET_SIZE];
        private DatagramPacket inPac = new DatagramPacket(in, in.length);

        @Override
        public void run() {
            while (!isTerminated) {
                try {
                    udpSoc.receive(inPac);
                    receiveBytes((InetSocketAddress) inPac.getSocketAddress(),
                            in, inPac.getLength());
                } catch (IOException e) {
                    if (isTerminated) break;  // means shutdown
                    log.warnException(e);      // temp I/O error
                }
            }
        }
    }

    private final Fragments frags = new Fragments();

    private final BytesReceiver bytesReceiver;
    private UdpLocator peerLocator;
    private final DatagramSocket udpSoc;
    private final Listener listener;
    private volatile boolean isTerminated;
    
    public UdpTransportService(BytesReceiver bytesReceiver,
            UdpLocator peerLocator) throws IOException {
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

    void receiveBytes(InetSocketAddress from, byte[] pac, int len) {
        if  (log.isTracing())
            log.entry("receiveBytes() " + len + "bytes");
        
        Fragments.FragmentPacket fpac = new Fragments.FragmentPacket(pac, len);
        if (log.wouldLog(DebugLevel.L5_DEBUG)) {
            log.debug("fragPacket tag:" + frags.getTag(from, fpac.msgId) 
                    + " seq:" + fpac._seq + " len:" + len);
            log.debugObject("fragPacket", ByteUtil.dumpBytes(pac, 0, len));
        }
        ByteBuffer msg = null;
        if (fpac.msgId == 0) {
            msg = ByteBufferUtil.byte2Buffer(fpac.bbuf, fpac.boff, fpac.blen);
        } else {
            msg = frags.put(from, fpac);
        }
        if (msg == null) {
            log.exit("receiveBytes()");
            return;
        }
        log.debugObject("rsv bytes", msg.remaining());
        bytesReceiver.receiveBytes(this, msg);
        log.exit("receiveBytes()");
    }

    private short msgId = 0;
    
    /**
     * フラグメント化したパケットを識別するためのmsgIdを採番する。
     * msgIdには、1～32767の番号を巡回的に使用する。
     * msgId == 0 はフラグメント化の必要のないパケットのmsgIdとして使用する。
     * 
     * @return 採番したmsgId
     */
    private synchronized short newMsgId() {
        if (msgId < Short.MAX_VALUE) msgId++;
        else msgId = 1;
        return msgId;
    }
    
    public void sendBytes(boolean isSend, PeerLocator peer, ByteBuffer msg) 
            throws IOException {
        int msgLen = msg.remaining();
        if  (log.isTracing())
            log.entry("sendBytes() " +  + msgLen + "bytes");
        if (msgLen > ConfigValues.MAX_MSG_SIZE) {
            log.error("send data over MAX_MSG_SIZE:" + msgLen + "bytes");
            return;
        }
        
        short msgId = 0;
        int pNum = 1;
        if (msgLen > MAX_PACKET_DATA_SIZE) {
            msgId = newMsgId();
            pNum = (msgLen - 1) / MAX_PACKET_DATA_SIZE + 1;
        }
        
        /*
         * 長さ0のメッセージの送信も行う。
         */
        for (int i = 0; i < pNum; i++) {
            int boff = msg.arrayOffset() + msg.position() + i * MAX_PACKET_DATA_SIZE;
            int blen;
            if (i == pNum - 1) {
                blen = msgLen - i * MAX_PACKET_DATA_SIZE;
            } else {
                blen = MAX_PACKET_DATA_SIZE;
            }
            byte[] pac = frags.newPacketBytes(msgId, i, pNum, msg.array(), boff, blen);
            if (log.wouldLog(DebugLevel.L5_DEBUG)) {
                log.debug("fragPacket msgId:" + msgId + " i:" + i + " len:" + pac.length);
                log.debugObject("fragPacket", ByteUtil.dumpBytes(pac));
            }
            udpSoc.send(new DatagramPacket(pac, pac.length, 
                    ((InetLocator) peer).getSocketAddress()));
        }
        log.exit("sendBytes()");
    }

	public void setLocator(PeerLocator locator) {
		this.peerLocator = (UdpLocator) locator;
	}

	public boolean canSet(PeerLocator target) {
		return target instanceof UdpLocator && ((InetLocator) target).getPort() == peerLocator.getPort();
	}
}
