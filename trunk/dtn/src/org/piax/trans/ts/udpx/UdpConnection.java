/*
 * UdpConnection.java
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
 * $Id: UdpConnection.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans.ts.udpx;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.TimerTask;

import org.grlea.log.SimpleLogger;

/*
 * TODO
 * タイムアウト時間の調整については未実装。
 * RTTの管理はすべてのコネクションについて行う必要がある。
 * これは、send-replyの間の応答遅延を調べるのではなく、
 * （そもそも、この応答遅延は、LocatorTransでないと解らない）
 * パケットを受理したコネクションは相手側に必ずackを送るといった
 * 対処が必要である。
 * 加えて、コネクションがtimeoutしたことの判定も簡易化しているため、
 * 維持管理しているコネクションに対してしか解らない。
 * これは上記の対応によって改善する。
 */

/**
 * <p>
 * ここで行うことは、コネクション自体の管理に限定される。
 * コネクション保持のためのkeep aliveのやり取りや通信エラーを判別するための
 * タイムアウト時間の調整を行う。
 * 
 * @author     Mikio Yoshida
 * @version    2.1.0
 */
class UdpConnection  implements ReceiveHandler {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(UdpConnection.class);
    
    public static int NO_ACK_THRESHOLD = 1;
    public static final int KEEPALIVE_PERIOD = 20 * 1000;
    
    private class KeepAliver extends TimerTask {
        @Override
        public void run() {
            if (noAck >= NO_ACK_THRESHOLD) {
                // コネクションが不通であると判断
                mgr.acceptDeath(remote);
                return;
            }
            noAck++;
            try {
                send(UdpPacket.newKeepAliveReq(mgr.getMe().nat));
            } catch (IOException e) {
                // TODO
                log.warnException(e);
            }
        }
    }

    private final ConnectionMgr mgr;
    private final InetSocketAddress remote;
    private int noAck = 0;
    private boolean needsMaintain = false;
    
    /**
     * UDPコネクションを生成する。
     * needsMaintain を trueに指定した場合は、コネクション維持のため、
     * 定期的に keepAliveパケットを送信する。
     * 
     * @param mgr ConnectionMgrオブジェクト
     * @param remote 接続先
     * @param needsMaintain 維持が必要な場合true 
     */
    UdpConnection(ConnectionMgr mgr, InetSocketAddress remote, boolean needsMaintain) {
        this.mgr = mgr;
        this.remote = remote;
        // if !given set timer
        if (needsMaintain) {
            this.needsMaintain = true;
            mgr.transService.schedule(new KeepAliver(), KEEPALIVE_PERIOD, KEEPALIVE_PERIOD);
        }
    }

    void setMaintain() {
        if (!needsMaintain) {
            needsMaintain = true;
            mgr.transService.schedule(new KeepAliver(), 0L, KEEPALIVE_PERIOD);
        }
    }
    
    void send(ByteBuffer bbuf) throws IOException {
        mgr.getChannel().send(bbuf, remote);
    }

    /**
     * ここでは、keep aliveに関係する受信処理だけを行う。
     */
    public void receive(ByteBuffer bbuf) {
        byte c = bbuf.get(bbuf.position());
        if (c == UdpPacket.KEEPALIVE_ACK) {
            noAck--;
            return;
        }
        if (c == UdpPacket.KEEPALIVE_REQ) {
            log.debug("keepAlive REQ received");
            try {
                ByteBuffer buf = ByteBuffer.allocate(1);
                buf.put(UdpPacket.KEEPALIVE_ACK);
                buf.flip();
                send(buf);
            } catch (IOException e) {
                // TODO
                log.warnException(e);
            }
            return;
        }
        log.error("Illegal control packet:" + bbuf.get(0));
    }
}
