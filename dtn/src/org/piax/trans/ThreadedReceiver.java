/*
 * ThreadedReceiver.java
 * 
 * Copyright (c) 2008- National Institute of Information and 
 * Communications Technology
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
 * $Id: ThreadedReceiver.java 218 2010-05-19 03:51:55Z teranisi $
 */

package org.piax.trans;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.grlea.log.SimpleLogger;
import org.piax.trans.msgframe.CallerHandle;
import org.piax.trans.msgframe.MessagingRoot;
import org.piax.trans.msgframe.NoReplyCallerHandle;
import org.piax.trans.msgframe.Session;

/**
 * 受信したメッセージの受信にスレッドを割り当てて実行するための管理クラス。
 * <p>
 * LocatorTransportが、LocatorTransportServiceからreceiveまたはreceiveReply
 * により受け取った受信メッセージは、そのまま実行すると、LocatorTransportService
 * のListener用スレッドを占有することになり、非効率である。
 * このクラスでは、Listener用スレッドの占有を防ぐため、各メッセージの受信処理に
 * スレッドを割り当てる。スレッドの割り当てと開放は頻繁に起こるため、
 * スレッドプーリングにより効率化させる。
 * <p>
 * TODO
 * 尚、現在の実装では、Executors#newCachedThreadPoolを用いている。
 * スレッドは60秒間アイドル状態が続いた場合に破棄されるため、スレッドの占有時間
 * が短い多数のリクエストが発生する状況において有効である。
 * スレッドプールに上限がないため、同時に多数のメッセージを受信しても待たずに
 * 処理を行う。このため、単位時間あたりの受信処理メッセージ数を制限する場合は
 * 下位の層にその制限を組み込むか、別にスレッドプールの実装を検討する必要がある。
 * 現時点の実装では、1つのJVMで10,000を越えるピアを実行する（その場合も使用
 * するスレッドプールは1つである）ことを想定しているため、上限のない
 * スレッドプールを用いている。
 * 
 * @author     Mikio Yoshida
 * @version    2.2.0
 */
public class ThreadedReceiver {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(ThreadedReceiver.class);

    /** thread pool for receivers */
    private static ExecutorService threadPool = 
        Executors.newCachedThreadPool();
    
    private static int instanceCount = 0;
    
    /**
     * receive用スレッド
     */
    private class Receiver implements Runnable {
        private final CallerHandle caller;
        private final ByteBuffer msg;
        private final int sessionId;
        
        Receiver(CallerHandle caller, ByteBuffer msg, int sessionId) {
            this.caller = caller;
            this.msg = msg;
            this.sessionId = sessionId;
        }

        public void run() {
            msgReceiver.receive(msg, sessionId == 0 ? new NoReplyCallerHandle(caller) : caller);
        }
    }

    /**
     * receiveReply用スレッド
     */
    private class ReplyReceiver implements Runnable {
        private final ByteBuffer msg;
        private final Session session;
        
        ReplyReceiver(ByteBuffer msg, Session session) {
            this.msg = msg;
            this.session = session;
        }

        public void run() {
            msgReceiver.receiveReply(session, msg);
        }
    }

    /** スレッドプールを適切にfinさせるための参照カウンタ */
    private final MessagingRoot msgReceiver;
    
    /**
     * ReceiverMgrオブジェクトを起動する。
     * 
     * @param msgReceiver
     *          LocatorTransportが受信メッセージを引き渡すオブジェクト
     */
    ThreadedReceiver(MessagingRoot msgReceiver) {
        if (threadPool.isShutdown()) {
            threadPool = Executors.newCachedThreadPool();
        }
        this.msgReceiver = msgReceiver;
        instanceCount++;
    }
    
    /**
     * receiveメソッドをスレッドを割り当てて実行する。
     * 
     * @param caller MessageReplyHandleオブジェクト
     * @param msg メッセージ
     */
    void doThreadedReceive(CallerHandle caller, ByteBuffer msg, int sessionId) {
        threadPool.execute(new Receiver(caller, msg, sessionId));
    }
    
    /**
     * receiveReplyメソッドをスレッドを割り当てて実行する。
     * 
     * @param session Sessionオブジェクト
     * @param msg メッセージ
     */
    void doThreadedReceiveReply(ByteBuffer msg, Session session) {
        threadPool.execute(new ReplyReceiver(msg, session));
    }

    /**
     * ReceiverMgrオブジェクトを終了させる。
     * スレッドプールがこのメソッドにより終了する。
     */
    synchronized void fin() {
        instanceCount--;
        
        // 最後のインスタンスを終了させる場合は、thread poolを終わらせる
        // 必要がある。
        if (instanceCount == 0) {
            // shutdown thread pool
            threadPool.shutdown();
            try {
                threadPool.awaitTermination(
                        LocatorTransport.MAX_WAIT_TIME_FOR_TERMINATION, 
                        TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                log.warn("some tasks not terminated");
            }
        }
    }
}
