/*
 * TcpTransportService.java
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
 * 2007/01/22 designed and implemented by M. Yoshida.
 * 
 * $Id: TcpTransportService.java 290 2010-10-05 05:58:57Z teranisi $
 */

package org.piax.trans.ts.tcp;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.grlea.log.SimpleLogger;
import org.piax.trans.common.PeerLocator;
import org.piax.trans.ts.BytesReceiver;
import org.piax.trans.ts.InetLocator;
import org.piax.trans.ts.LocatorTransportSpi;

/**
 * @author     Mikio Yoshida
 * @version    2.1.0
 */
public class TcpTransportService implements LocatorTransportSpi {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(TcpTransportService.class);

    /**
     * アイドル状態のスレッドが終了前に新規タスクを待機する最大時間
     */
    public static long THREAD_KEEP_ALIVE_TIME = 10 * 60 * 1000L;

    // about connection pooling
    /**
     * アイドル状態のClient Connectionの維持時間
     */
    public static long CLI_CONN_KEEP_ALIVE_TIME = 1 * 60 * 1000L;

    /**
     * アイドル状態のServer Connectionの維持時間
     */
    public static long SRV_CONN_KEEP_ALIVE_TIME = 10 * 60 * 1000L;
    
    /**
     * Sweeperを定期起動するための時間間隔
     */
    public static long SWEEP_PERIOD_TIME = 1 * 60 * 1000L;

    /**
     * Sweeperを定期起動するための時間間隔
     */
    public static boolean USE_CLI_CONN_AT_SRV_SEND = true;

    private final BytesReceiver bytesReceiver;
    private TcpLocator peerLocator;
    private final SocketListener listener;
    private final ConnectionMgr connMgr;

    public TcpTransportService(BytesReceiver bytesReceiver, TcpLocator peerLocator)
            throws IOException {
        log.entry("new()");
        this.bytesReceiver = bytesReceiver;
        this.peerLocator = peerLocator;

        connMgr = new ConnectionMgr(this);
        // listener thread start
        listener = new SocketListener(this, peerLocator.getPort());
        listener.start();
        log.exit("new()");
    }

    /* (non-Javadoc)
     * @see org.piax.trans.spi.LocatorTransportSpi#getLocator()
     */
    public PeerLocator getLocator() {
        return peerLocator;
    }

    public void setLocator(PeerLocator peerLocator) {
        this.peerLocator = (TcpLocator) peerLocator;
    }
    
    public boolean canSend(PeerLocator target) {
        return peerLocator.sameClass(target);
    }

    /* (non-Javadoc)
     * @see org.piax.trans.spi.LocatorTransportSpi#fin()
     */
    public void fin() {
        log.entry("fin()");
        listener.terminate();
        connMgr.fin();
        log.exit("fin()");
    }

    public void sendBytes(boolean isSend, PeerLocator toPeer, ByteBuffer msg)
            throws IOException {
        if (isSend) {
            Connection conn = connMgr.getClientConnection((InetLocator) toPeer);
            conn.send(msg);
        } else {
            Connection conn = connMgr.getServerConnection((InetLocator) toPeer);
            if (conn == null) {
                throw new IOException("The connection for reply purged");
            }
            conn.send(msg);
        }
    }

    void receiveBytes(Connection conn, ByteBuffer msg) {
        PeerLocator fromPeer = bytesReceiver.getFromPeer(this, msg);
//        if (fromPeer != null && conn.isServer()) {
        if (fromPeer != null) {
            connMgr.map((TcpLocator) fromPeer, conn);
        }
        bytesReceiver.receiveBytes(this, msg);
    }
    
    void newServerConnection(Socket soc) {
        try {
            connMgr.newServerConnection(soc);
        } catch (IOException e) {
            log.errorException(e);
        }
    }

    void hangup(Throwable cause) {
        // TODO
    }

	public boolean canSet(PeerLocator target) {
		// TODO Auto-generated method stub
		return target instanceof TcpLocator && ((InetLocator) target).getPort() == peerLocator.getPort();
	}
	
	public void locatorUnavailable(PeerLocator locator) {
	    bytesReceiver.locatorUnavailable(locator);
	}
}
