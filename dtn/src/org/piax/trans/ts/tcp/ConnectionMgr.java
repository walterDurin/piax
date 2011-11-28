/*
 * ConnectionMgr.java
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
 * $Id: ConnectionMgr.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans.ts.tcp;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.grlea.log.SimpleLogger;
import org.piax.trans.LocatorTransport;
import org.piax.trans.ts.InetLocator;

/**
 * TCP通信におけるconnectionを管理するクラス。
 * <p>
 * ConnectionMgrでは、Connectionの生成、破棄と送信先アドレス（InetLocator）
 * による検索機能を提供する。
 * また、性能向上のため、次の2つのpooling処理を行う。
 * <ul>
 * <li>thread pooling
 * <li>connection pooling
 * </ul>
 * 
 * @author     Mikio Yoshida
 * @version    2.1.0
 */
class ConnectionMgr {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(ConnectionMgr.class);

    // about thread pooling
    static int CORE_POOL_SIZE = 5;
    static int MAX_POOL_SIZE = 400;
    
    private final TcpTransportService transportService;
    private final ConcurrentMap<InetLocator, Connection> cliConns;
    private final ConcurrentMap<InetLocator, Connection> srvConns;
    private final Set<Connection> remains;
    private final ThreadPoolExecutor threadPool;
    private final Timer sweepTimer;

    /**
     * アイドル時間が超過したConnectionの破棄するためのTimerTask
     * 
     * @author     Mikio Yoshida
     * @version    2.1.0
     */
    private class Sweeper extends TimerTask {
        @Override
        public void run() {
            log.entry("run()");
            // アイドル時間が超過しているクライアントConnectionを破棄
            for (Map.Entry<InetLocator, Connection> ent: cliConns.entrySet()) {
                if (ent.getValue().terminateIfpurged(
                        TcpTransportService.CLI_CONN_KEEP_ALIVE_TIME)) {
                    cliConns.remove(ent.getKey());
                }
            }
            // アイドル時間が超過しているサーバConnectionを破棄
            for (Map.Entry<InetLocator, Connection> ent: srvConns.entrySet()) {
                if (ent.getValue().terminateIfpurged(
                        TcpTransportService.SRV_CONN_KEEP_ALIVE_TIME)) {
                    srvConns.remove(ent.getKey());
                }
            }
            // アイドル時間が超過している未割り当てサーバConnectionを破棄
            for (Connection conn: remains) {
                if (conn.terminateIfpurged(
                        TcpTransportService.SRV_CONN_KEEP_ALIVE_TIME)) {
                    remains.remove(conn);
                }
            }
            log.exit("run()");
        }
    }
    
    ConnectionMgr(TcpTransportService transportService) {
        this.transportService = transportService;
        cliConns = new ConcurrentHashMap<InetLocator, Connection>();
        srvConns = new ConcurrentHashMap<InetLocator, Connection>();
        remains = new CopyOnWriteArraySet<Connection>();
        threadPool = new ThreadPoolExecutor(
            CORE_POOL_SIZE, 
            MAX_POOL_SIZE,
            TcpTransportService.THREAD_KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS,
            new SynchronousQueue<Runnable>(),
            new ThreadPoolExecutor.CallerRunsPolicy());
        // as daemon
        sweepTimer = new Timer(true);
        sweepTimer.schedule(new Sweeper(), 
                TcpTransportService.SWEEP_PERIOD_TIME, 
                TcpTransportService.SWEEP_PERIOD_TIME);
    }
    
    /**
     * dstPeerを送信先locatorとするclient connectionを生成する。
     * 
     * @param dstPeer 送信先locator
     * @return dstPeerを送信先locatorとするclient connection
     * @throws IOException IO関係のエラーが発生した場合
     */
    private Connection newClientConnection(InetLocator dstPeer)
    throws IOException {
        Connection conn = 
            new Connection(transportService, dstPeer.getSocketAddress());
        cliConns.put(dstPeer, conn);
        threadPool.execute(conn);
        return conn;
    }
    
    /**
     * dstPeerを送信先locatorとするclient connectionを取得する。
     * connection poolに該当するconnectionが存在する場合は、それを返す。
     * 該当する該当するconnectionが存在しない場合は、新規にconnectionを
     * 生成する。
     * 
     * @param dstPeer 送信先locator
     * @return dstPeerを送信先locatorとするclient connection
     * @throws IOException IO関係のエラーが発生した場合
     */
    synchronized Connection getClientConnection(InetLocator dstPeer) 
    throws IOException {
        log.entry("getClientConnection()");
        try {
            Connection conn = cliConns.get(dstPeer);
            if (conn != null && conn.checkActiveAndTouch())
                return conn;
            if (TcpTransportService.USE_CLI_CONN_AT_SRV_SEND) {
                conn = getServerConnection(dstPeer);
                if (conn != null && conn.checkActiveAndTouch())
                    return conn;
            }
            return newClientConnection(dstPeer);
        } finally {
            log.exit("getClientConnection()");
        }
    }
    
    /**
     * 指定されたSocketオブジェクトからserver connectionを生成する。
     * この場合、newClientConnectionの場合と異なり、返信先locatorとの関連付け
     * は行わない。
     * 
     * @param soc Socket
     * @throws IOException IO関係のエラーが発生した場合
     */
    void newServerConnection(Socket soc) throws IOException {
        log.entry("newServerConnection()");
        Connection conn = 
            new Connection(transportService, soc);
        remains.add(conn);
        threadPool.execute(conn);
        log.exit("newServerConnection()");
    }
    
    /**
     * srcPeerを返信先locatorとするserver connectionを取得する。
     * connection poolに該当するconnectionが存在する場合は、それを返すが、
     * getClientConnection の場合と異なり、
     * 該当するconnectionが存在しない場合は、nullを返す。
     * これは、server connectionがsocket listenerにのみにより生成されるため
     * である。
     * 
     * @param srcPeer 返信先locator
     * @return srcPeerを返信先locatorとするserver connection。
     *          存在しない場合はnull
     */
    Connection getServerConnection(InetLocator srcPeer) {
        log.entry("getServerConnection()");
        try {
            Connection conn = srvConns.get(srcPeer);
            if (conn != null) {
                if (conn.checkActiveAndTouch()) {
                    return conn;
                }
                // 並行してputが起こった場合に備えてremove(key, value) を使う
                srvConns.remove(srcPeer, conn);
            }
//            try {
//                if (TcpTransportService.USE_CLI_CONN_AT_SRV_SEND)
//                    return getClientConnection(srcPeer);
//            } catch (IOException e) {
//                log.warnException(e);
//            }
            return null;
        } finally {
            log.exit("getServerConnection()");
        } 
    }

    /**
     * 返信先locatorとserver connectionの関連付けを行う。
     * 
     * @param srcPeer 返信先locator
     * @param serverConn server connection
     */
    void map(TcpLocator srcPeer, Connection serverConn) {
        log.entry("map()");
        Connection tmp = srvConns.put(srcPeer, serverConn);
        if (tmp == null) {
            log.debugObject("srcPeer", srcPeer);
            log.debugObject("serverConn", serverConn);
            log.debugObject("srcAddress", serverConn.getRemoteSocketAddress());
            
        }
        if (tmp != null) remains.add(tmp);
        remains.remove(serverConn);
        log.exit("map()");
    }

    private void terminateAllConns() {
        for (Connection conn: cliConns.values()) {
            conn.terminate();
        }
        for (Connection conn: srvConns.values()) {
            conn.terminate();
        }
        for (Connection conn: remains) {
            conn.terminate();
        }
    }
    
    /**
     * thread poolをshutdownする。
     * 活動中のConnectionの正しく終了させるため、
     * ThreadPoolExecutor#awaitTerminationメソッドを使っている。
     * TODO think!
     * awaitTerminationしないでよいかもしれない。
     */
    private void terminatePool() {
        threadPool.shutdownNow();
        try {
            threadPool.awaitTermination(
                    LocatorTransport.MAX_WAIT_TIME_FOR_TERMINATION, 
                    TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.warn("some tasks not terminated");
        }
    }

    void fin() {
        sweepTimer.cancel();
        terminateAllConns();
        terminatePool();
    }
}
