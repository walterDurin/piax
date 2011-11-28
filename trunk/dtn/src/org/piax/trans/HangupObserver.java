/*
 * HangupObserver.java
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
 * 2007/02/11 designed and implemented by M. Yoshida.
 * 
 * $Id: HangupObserver.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans;

import org.piax.trans.ts.LocatorTransportSpi;

//-- Appreciates translating the following Japanese doc into English. :-)

/**
 * LocatorTransport層で発生する致命的な例外に対処するため、上位クラス
 * が持つ機能として定義されるインタフェース。
 * <p>
 * このインタフェースを実装するオブジェクトは、
 * <code>acceptHangup</code>メソッドにより通知を受けた後、例外を起こした
 * <code>LocatorTransportService2</code>オブジェクトを別のタイミングで再起動する
 * などの処置をし、例外に対するリカバリーを行う必要がある。
 * 
 * @author     Mikio Yoshida
 * @version    1.1.0
 */
public interface HangupObserver {
    
    /**
     * LocatorTransport層自体をハングアップさせた例外の発生を受理する。
     * 
     * @param locTrans 例外を発生した<code>LocatorTransportSpi</code>オブジェクト
     * @param cause transport層で発生した致命的例外
     */
    void acceptHangup(LocatorTransportSpi locTrans, Throwable cause);
}
