/*
 * ConfigValues.java
 * 
 * Copyright (c) 2008-2010 National Institute of Information and 
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
 * 2007/10/03 designed and implemented by M. Yoshida.
 * 
 * $Id: ConfigValues.java 184 2010-03-03 16:22:21Z yos $
 */

package org.piax.trans;

import org.piax.trans.rpc.RPCWrapper;
import org.piax.trans.common.PeerId;

/**
 * システム設定値を保持するクラス。
 * 
 * @author     Mikio Yoshida
 * @version    2.1.0
 */
public class ConfigValues {
    public static final byte VERSION_NO = 22; 
    public static final short MSG_MAGIC = 12367 + VERSION_NO;
    
    public static long relaySwapInterval = 1 * 60 * 60 * 1000; // 1 hour

    // ID length
    public static int peerIdByteLength = 16;

    // timeout
    public static long rpcTimeout = 10 * 1000L;
    public static long callMultiTimeout = 120 * 1000L;
    public static long returnSetGetNextTimeout = 15 * 1000L;

    // ReturnSet capacity
    public static int defaultReturnSetCapacity = 30;
    
    /** ピアが一時的に保持できるSession数の上限 */
    public static int MAX_SESSIONS = 100000;

    /**
     * MTU
     */
    public static int MAX_PACKET_SIZE = 1400;

    /**
     * send/replyで送受信可能なメッセージ長の上限。
     * UdpTransportServiceで行っているフラグメント数の最大値より、
     * メッセージ長の最大は、47MBまで設定可能であるが、
     * 大きなサイズのメッセージを分割しないで送受信することは、
     * メッセージのロスと遅延の点でよくない。
     * ここでは、現実的な上限値として1MBとしておく。
     * （実際のメッセージは、100KB以下にしておくべきである）
     * <p>
     * MTU(1450) * 32767 = 47,512,150 
     */
    public static int MAX_MSG_SIZE = 1000 * 1000;

    static {
        set();
    }

    public static void set() {
        PeerId.BYTE_LENGTH = peerIdByteLength;

        RPCWrapper.RPC_TIMEOUT = rpcTimeout;
        RPCWrapper.CALLMULTI_TIMEOUT = callMultiTimeout;
        RPCWrapper.RETURNSET_GETNEXT_TIMEOUT = returnSetGetNextTimeout;
        RPCWrapper.RETURNSET_CAPACITY = defaultReturnSetCapacity;
    }
}
