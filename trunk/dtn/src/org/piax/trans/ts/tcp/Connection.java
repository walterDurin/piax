/*
 * Connection.java
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
 * 2009/02/05 designed and implemented by M. Yoshida.
 * 
 * $Id: Connection.java 287 2010-09-06 01:40:08Z teranisi $
 */

package org.piax.trans.ts.tcp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.grlea.log.SimpleLogger;
import org.piax.trans.util.ByteBufferUtil;
import org.piax.trans.util.ByteUtil;

/**
 * TCP通信におけるconnectionを実現する。
 * <p>
 * Connectionオブジェクトは、client側またはserver側の端子として機能する。
 * Connectionオブジェクトはまた、poolingの対象となるため、メッセージ単位が
 * 連続して入ってくるメッセージ群から区別できるようにする必要がある。
 * このため、メッセージには4byteの長さヘッダ（int値に変換可能）を設ける。
 * 
 * @author     Mikio Yoshida
 * @version    2.2.0
 */
class Connection implements Runnable {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(Connection.class);

    static int BUF_SIZE = 16384;

    private final boolean isServer;
    private final TcpTransportService transportService;
    private final Socket soc;
    private final InputStream in;
    private final OutputStream out;
    private volatile boolean isTerminated;
    private volatile long lastActivated;

    /**
     * server connection を生成する。
     * 
     * @param transportService TcpTransportServiceオブジェクト
     * @param soc Socket
     * @throws IOException IO関係の例外が発生した場合
     */
    Connection(TcpTransportService transportService, Socket soc)
    throws IOException {
        isServer = true;
        this.soc = soc;
        this.transportService = transportService;
        soc.setSendBufferSize(BUF_SIZE);
        soc.setReceiveBufferSize(BUF_SIZE);
        in = new BufferedInputStream(
                soc.getInputStream(), soc.getReceiveBufferSize());
        out = new BufferedOutputStream(
                soc.getOutputStream(), soc.getSendBufferSize());
        isTerminated = false;
        lastActivated = System.currentTimeMillis();
    }

    /**
     * client connection を生成する。
     * 
     * @param transportService TcpTransportServiceオブジェクト
     * @param addr SocketAddress
     * @throws IOException IO関係の例外が発生した場合
     */
    Connection(TcpTransportService transportService, SocketAddress addr)
    throws IOException {
        isServer = false;
        soc = new Socket();
        //        System.out.println("!!!! connecting to " + addr + "!!!!");
        // cannot wait the timeout //
        soc.connect(addr, 5000);
        this.transportService = transportService;
        soc.setSendBufferSize(BUF_SIZE);
        soc.setReceiveBufferSize(BUF_SIZE);
        in = new BufferedInputStream(
                soc.getInputStream(), soc.getReceiveBufferSize());
        out = new BufferedOutputStream(
                soc.getOutputStream(), soc.getSendBufferSize());
        isTerminated = false;
        lastActivated = System.currentTimeMillis();
    }
    
    boolean isServer() {
        return isServer;
    }

    synchronized void send(ByteBuffer msg) throws IOException {
        // 長さheader（4byte）を付与する
        int len = msg.remaining();
        ByteBuffer bb = ByteBufferUtil.reserve(4, msg);
        bb.putInt(len);
        ByteBufferUtil.rewind(bb);
        out.write(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining());
        out.flush();
        lastActivated = System.currentTimeMillis();
    }
    
    private long getIdleTime() {
        long t = System.currentTimeMillis() - lastActivated;
        return t;
    }
    
    synchronized void terminate() {
        log.entry("terminate()");
        if (isTerminated) return;
        isTerminated = true;
        try {
            soc.shutdownOutput();
            soc.close();
        } catch (IOException ignore) {
        }
        // XXX how this should be implemented? transportService.locatorUnavailable(locator);
        log.exit("terminate()");
    }
    
    /**
     * connectionが生きているかチェックし、生きている場合は、
     * 並行スレッドにより破棄されないために、lastActivatedを更新しておく。
     * このメソッドは、並行スレッドの排他制御のためのatomicな処理になる。
     * 
     * @return connectionが生きている場合はtrue、それ以外はfalse
     */
    synchronized boolean checkActiveAndTouch() {
        if (isTerminated) return false;
        lastActivated = System.currentTimeMillis();
        return true;
    }
    
    /**
     * connectionが指定したpurgeTimeより長くアイドル状態にある場合は、
     * connectionを終了させる。そうでない場合は、何もしないで、falseを返す。
     * このメソッドは、並行スレッドの排他制御のためのatomicな処理になる。
     * 
     * @param purgeTime 破棄時間
     * @return connectionが終了した場合はtrue、それ以外はfalse
     */
    synchronized boolean terminateIfpurged(long purgeTime) {
        if (isTerminated) return true;
        if (getIdleTime() < purgeTime) return false;
        terminate();
        return true;
    }
    
    // for debug
    SocketAddress getRemoteSocketAddress() {
        return soc.getRemoteSocketAddress();
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        log.entry("run()");
        byte[] msgLen = new byte[4];
        while (true) {
            // 1メッセージの読み込み
            try {
                // header（4byteの長さ情報）の読み込み
                int n = in.read(msgLen);
                if (n == -1) {
                    terminate();
                    break;
                }
                if (n != 4) {
                    log.warn("broken message, lost header");
                    terminate();
                    break;
                }
                int len = ByteUtil.bytes2Int(msgLen);
                ByteBuffer bb = ByteBufferUtil.newByteBuffer(len);
                int b = 0;
                for (int i = 0; i < len; i++) {
                    b = in.read();
                    if (b == -1) break;
                    bb.put((byte) b);
                }
                if (b == -1) {
                    log.warn("broken message");
                    terminate();
                    break;
                }
                ByteBufferUtil.flip(bb);
                lastActivated = System.currentTimeMillis();
                transportService.receiveBytes(this, bb);
            } catch (IOException e) {
                if (!isTerminated) {
                    // unexpected socket error
                    log.warnException(e);
                    terminate();
                }
                break;
            }
        }
        log.exit("run()");
    }
}
