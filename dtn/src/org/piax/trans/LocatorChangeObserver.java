/*
 * LocatorChangeObserver.java
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
 * 2007/10/25 designed and implemented by M. Yoshida.
 * 
 * $Id: LocatorChangeObserver.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans;

import org.piax.trans.common.PeerLocator;
import org.piax.trans.ts.LocatorTransportSpi;

//-- Appreciates translating the following Japanese doc into English. :-)

/**
 * locatorの変更通知を受理するために定義されるインタフェース。
 * <p>
 * <code>acceptChange</code>メソッドにより受理したlocatorの変更は、適切なタイミングで、
 * <code>IdResolver</code>オブジェクトに渡す必要がある。
 * 
 * @author     Mikio Yoshida
 * @version    1.1.0
 */
public interface LocatorChangeObserver {
    
    /**
     * locatorの変更通知を受理する。
     * 
     * @param locTrans 変更を発生した<code>LocatorTransportSpi</code>オブジェクト
     * @param newLoc 変更後のlocator
     */
    void acceptChange(LocatorTransportSpi locTrans, PeerLocator newLoc);

    /**
     * 
     * @param locTrans
     */
    void acceptFadeout(LocatorTransportSpi locTrans);
}
