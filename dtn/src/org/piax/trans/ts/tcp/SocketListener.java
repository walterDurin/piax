package org.piax.trans.ts.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.grlea.log.SimpleLogger;

class SocketListener extends Thread {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(SocketListener.class);
    
    /**
     * 受信するconnection(接続要求)のキューの最大長。
     * キューが埋まった際の接続要求は拒否される。
     * ServerSocketのデフォルトは 50
     */
    static int REQ_QUEUE_LEN = 50;
    
    private final TcpTransportService transportService;
    private final ServerSocket ssoc;
    private volatile boolean isTerminated;
    
    SocketListener(TcpTransportService transportService, int port)
            throws IOException {
        this.transportService = transportService;
        ssoc = new ServerSocket(port, REQ_QUEUE_LEN);
        isTerminated = false;
    }

    synchronized void terminate() {
        log.entry("terminate()");
        if (isTerminated) return;
        isTerminated = true;
        try {
            // causes ServerSocket#accept interruption!
            ssoc.close();
        } catch (IOException ignore) {
        }
        log.exit("terminate()");
    }
    
    boolean isTerminated() {
        return isTerminated;
    }

    @Override
    public void run() {
        log.entry("run()");
        while (!isTerminated) {
            try {
                Socket soc = ssoc.accept();
                transportService.newServerConnection(soc);
            } catch (IOException e) {
                if (!isTerminated) {
                    // unexpected socket error
                    log.warnException(e);
                    terminate();
                    transportService.hangup(e);
                }
                break;
            }
        }
        log.exit("run()");
    }
}
