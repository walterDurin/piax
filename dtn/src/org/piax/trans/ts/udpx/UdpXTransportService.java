/*
 * UdpXTransportService.java
 * 
 * Copyright (c) 2008 National Institute of Information and 
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
 * 2007/11/11 designed and implemented by M. Yoshida.
 * 
 * $Id: UdpXTransportService.java 290 2010-10-05 05:58:57Z teranisi $
 */

package org.piax.trans.ts.udpx;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.grlea.log.DebugLevel;
import org.grlea.log.SimpleLogger;
import org.piax.trans.ConfigValues;
import org.piax.trans.LocatorTransport;
import org.piax.trans.common.PeerLocator;
import org.piax.trans.ts.BytesReceiver;
import org.piax.trans.ts.InetLocator;
import org.piax.trans.ts.LocatorTransportSpi;
import org.piax.trans.ts.udp.UdpLocator;
import org.piax.trans.util.ByteBufferUtil;

/**
 * @author     Mikio Yoshida
 * @version    2.1.0
 */
public class UdpXTransportService implements LocatorTransportSpi,
        ReceiveHandler {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(UdpXTransportService.class);
    
    public static final int RELAY_PEER_CONNECT_TIMEOUT = 2000;
    public static final int CONNECTION_REVERSAL_TIMEOUT = 2000;
    public static final int MAX_PACKET_DATA_SIZE = 1300;

    private final static Timer maintenanceTimer = new Timer(true);

    private final Fragments fragments = new Fragments();

    private final BytesReceiver bytesReceiver;
    private UdpXLocator myLocator;
    private final Collection<PeerLocator> relays;
    final ConnectionMgr connMgr;
    private RelaySwapper relaySwapper;

    public UdpXTransportService(BytesReceiver bytesReceiver,
            PeerLocator peerLocator,
            Collection<PeerLocator> relays)
            throws IOException {
        this.bytesReceiver = bytesReceiver;
        myLocator = (UdpXLocator) peerLocator;
        this.relays = relays;

        int port = ((UdpXLocator) peerLocator).getLocalAddress().getPort();
        connMgr = new ConnectionMgr(this, port);
        
        UdpXLocator xloc = (UdpXLocator) peerLocator;
        if (!xloc.isGlobal()) {
            if (!setRelayAndNATAddr(xloc)) {
                log.debug("access failed to relay");
            }
            // RelaySwapperの定期起動を登録する
            relaySwapper = new RelaySwapper(this);
            schedule(relaySwapper, ConfigValues.relaySwapInterval,
                    ConfigValues.relaySwapInterval);
        }
    }

    boolean setRelayAndNATAddr(UdpXLocator xloc) {
        if (relays == null || relays.size() == 0) {
            log.warn("no relay peers given");
            return false;
        }
        UdpXLocator relay = null;
        for (PeerLocator loc : relays) {
            try {
                if (!((UdpXLocator) loc).isGlobal()) continue;
                relay = (UdpXLocator) loc;
                InetSocketAddress nat = connMgr.getNATAddr(relay.global, 
                        RELAY_PEER_CONNECT_TIMEOUT);
                xloc.global = relay.global;
                xloc.nat = nat;
                return true;
            } catch (ClassCastException e) {
                log.warn("invalid relay peer locator class");
            } catch (IOException e) {
                log.warn("specified relay peer not responsed");
            }
        }
        log.warn("no right relay peers given");
        return false;
    }
    
    public void fin() {
        connMgr.fin();
        maintenanceTimer.cancel();
    }

    void schedule(TimerTask task, long delay, long period) {
        maintenanceTimer.schedule(task, delay, period);
    }

    public boolean canSend(PeerLocator target) {
        return myLocator.sameClass(target);
    }

    public LocatorTransport getLocatorTransport() {
        if (bytesReceiver instanceof LocatorTransport) {
            return (LocatorTransport) bytesReceiver;
        }
        return null;
    }
    
    public PeerLocator getLocator() {
        return myLocator;
    }
    
    void acceptChange(PeerLocator newLoc) {
        LocatorTransport trans = getLocatorTransport();
        if (trans == null) return;
        trans.acceptChange(this, newLoc);
    }
    
    void acceptDeath(InetSocketAddress target) {
        if (target.equals(myLocator.global)
                || target.equals(myLocator.global2)) {
            relaySwapper.run();
        }
    }

    private short msgid = 0;
    private Object msgidLockobj = new Object();
    public void sendBytes(boolean isSend, PeerLocator peer, ByteBuffer msg) 
    throws IOException {
        byte[] data = ByteBufferUtil.buffer2Bytes(msg);
        log.entry("sendBytes() " + data.length + "bytes");

        UdpXLocator toPeer = (UdpXLocator) peer;
        int snd_msgid = -1;
        synchronized (msgidLockobj) {
            msgid = (short)((msgid+1) & 0x7fff);
            snd_msgid = msgid;
        }
        int pNum = (data.length == 0) ? 0 : 
            (data.length - 1) / MAX_PACKET_DATA_SIZE + 1;
        for (int i = 0; i < pNum; i++) {
            byte[] fdata;
            int seq;
            if (i == pNum - 1) {
                fdata = new byte[data.length - i * MAX_PACKET_DATA_SIZE];
                seq = - pNum;
            } else {
                fdata = new byte[MAX_PACKET_DATA_SIZE];
                seq = i + 1;
            }
            System.arraycopy(data, i * MAX_PACKET_DATA_SIZE, fdata, 0, fdata.length);
            log.debug("sec " + seq + " msgid " + snd_msgid);
            ByteBuffer bbuf = UdpPacket.newUdpPacketBuff(UdpPacket.SEND_MSG_TYPE, 
                    (UdpXLocator) myLocator, toPeer, (short) seq, (short) snd_msgid, fdata);
            // send
            UdpPacket pac = UdpPacket.getUdpPacket(bbuf);
            if (log.wouldLog(DebugLevel.L5_DEBUG))
                log.debug("send msg:"+pac.getCommonHeader()+" sec:"+pac.seq);

            connMgr.send(toPeer, bbuf);
        }
        log.exit("sendBytes()");
    }
    
    private Set<String> lastPacketRecved = new HashSet<String>();
    public void receive(ByteBuffer bbuf) {
        log.entry("receive()");
        UdpPacket pac = UdpPacket.getUdpPacket(bbuf);
        byte[] msg = null;
        if (log.wouldLog(DebugLevel.L5_DEBUG))
            log.debug("receive msg:"+pac.getCommonHeader()+" sec:"+pac.seq);
        if (pac.seq == -1) {
            msg = pac.body;
        } else {
            int idx = Math.abs(pac.seq) - 1;
            fragments.put(pac.getCommonHeader(), idx, pac.body);
            synchronized (lastPacketRecved) {
                if (lastPacketRecved.contains(pac.getCommonHeader())) {
                    msg = fragments.getMsg(pac.getCommonHeader());
                    if (msg != null) {
                        log.debug("msg fragments are complete");
                        lastPacketRecved.remove(pac.getCommonHeader());
                    }
                }
                if (pac.seq <= -1) {
                    msg = fragments.getMsg(pac.getCommonHeader());
                    if (msg == null) {
                        log.debug("msg fragments are not complete");
                        lastPacketRecved.add(pac.getCommonHeader());
                    }
                }
            }
        }
        if (msg == null) {
            log.exit("receive()");
            return;
        }

        ByteBuffer bb = ByteBufferUtil.byte2Buffer(msg);
        bytesReceiver.receiveBytes(this, bb);
        log.debugObject("rsv bytes", msg.length);
        log.exit("receive()");
//      log.debugObject("rsv frag", ByteUtil.dumpBytes(msg));
    }

	public void setLocator(PeerLocator locator) {
		// XXX relays are not updated.
		myLocator = (UdpXLocator) locator;
	}

	public boolean canSet(PeerLocator target) {
		return target instanceof UdpXLocator && ((InetLocator) target).getPort() == myLocator.getLocalAddress().getPort();
	}
}
