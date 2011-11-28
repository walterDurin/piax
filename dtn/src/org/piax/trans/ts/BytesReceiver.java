/*
 * BytesReceiver.java
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
 * $Id: BytesReceiver.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans.ts;

import java.nio.ByteBuffer;

import org.piax.trans.common.PeerLocator;

/**
 * LocatorTransportServiceからの受信データをLocatorTransportなどの上位の
 * オブジェクトに転送するためのメソッドインタフェース。
 * LocatorTransportServiceからの受信を受けるクラスはこのインタフェースを
 * 実装する必要がある。
 * 
 * @author     Mikio Yoshida
 * @version    2.1.0
 */
public interface BytesReceiver {
    
    /**
     * 受信データを受け取る。
     * 
     * @param caller 呼び出し元のLocatorTransportService
     * @param data 受信データ
     */
    void receiveBytes(LocatorTransportSpi caller, ByteBuffer msg);
    PeerLocator getFromPeer(LocatorTransportSpi caller, ByteBuffer msg);
    /**
     * Interface to receive locator availability.    
     */
    void locatorUnavailable(PeerLocator locator);
    /**
     * Interface to receive locator availability.
     */
    void locatorAvailable(PeerLocator locator);
}
