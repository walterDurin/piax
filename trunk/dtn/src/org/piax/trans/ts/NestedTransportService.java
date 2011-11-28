/*
 * NestedTransportService.java
 * 
 * Copyright (c) 2008 National Institute of Information and 
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
 * 2009/02/16 designed and implemented by M. Yoshida.
 * 
 * $Id: NestedTransportService.java 290 2010-10-05 05:58:57Z teranisi $
 */

package org.piax.trans.ts;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;

import org.grlea.log.SimpleLogger;
import org.piax.trans.LocatorTransport;
import org.piax.trans.common.PeerLocator;

/**
 * @author     Mikio Yoshida
 * @version    2.1.0
 */
public class NestedTransportService<O extends PeerLocator, I extends PeerLocator>
        implements LocatorTransportSpi {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(NestedTransportService.class);

    protected final LocatorTransport trans;
    protected NestedLocator<O,I> myLocator;  // my peer locator
    
    /** 実際の送信を行う足回りのLocatorTransportService */
    protected LocatorTransportSpi outerTS = null;
    protected LocatorTransportSpi innerTS;
    
    public NestedTransportService(BytesReceiver bytesReceiver, 
            NestedLocator<O,I> peerLocator, Collection<PeerLocator> relays)
            throws IOException {
        trans = (LocatorTransport) bytesReceiver;
        myLocator = peerLocator;
    
        /*
         * TransPortServiceを取得する
         * 起動していなかった場合は起動する
         */
        if (myLocator.isOuter()) {
            outerTS = trans.getSameLocatorTransportService(myLocator.outer);
            if (outerTS == null) {
                trans.addLocator(myLocator.outer, relays);
                outerTS = trans.getSameLocatorTransportService(myLocator.outer);
            }
        }
        innerTS = trans.getSameLocatorTransportService(myLocator.inner);
        if (innerTS == null) {
            trans.addLocator(myLocator.inner, relays);
            innerTS = trans.getSameLocatorTransportService(myLocator.inner);
        }
    }

    public PeerLocator getLocator() {
        return myLocator;
    }

    public boolean canSend(PeerLocator target) {
        if (myLocator.sameClass(target)) return true;
        if (myLocator.outer.sameClass(target)) return true;
        if (outerTS != null) {
            if (outerTS.canSend(target)) return true;
        }
        return innerTS.canSend(target);
    }
    
    public void sendBytes(boolean isSend, PeerLocator toPeer, ByteBuffer msg) 
            throws IOException {
        if (myLocator.isOuter()) {
            if (myLocator.sameClass(toPeer)) {
                NestedLocator<?,?> to = (NestedLocator<?,?>) toPeer;
                // outer が同じ -> inner転送
                if (myLocator.outer.equals(to.outer)) {
                    innerTS.sendBytes(isSend, to.inner, msg);
                } else {
                    // outer が異なる -> gateway 介して外に
                    outerTS.sendBytes(isSend, to.outer, msg);
                }
            } else if (outerTS.canSend(toPeer)) {
                // そのまま流す
                outerTS.sendBytes(isSend, toPeer, msg);
            } else if (innerTS.canSend(toPeer)) {
                // そのまま流す
                innerTS.sendBytes(isSend, toPeer, msg);
            } else {
                // error 流せない
                log.error("cannot dispatch error");
            }
        } else {
            if (myLocator.sameClass(toPeer)) {
                NestedLocator<?,?> to = (NestedLocator<?,?>) toPeer;
                if (myLocator.outer.equals(to.outer)) {
                    // そのまま流す
                    innerTS.sendBytes(isSend, to.inner, msg);
                } else {
                    // 流せないので、gatewayにdelegateする
                    innerTS.sendBytes(isSend, myLocator.gateway, msg);
                }
            } else if (innerTS.canSend(toPeer)) {
                // そのまま流す
                innerTS.sendBytes(isSend, toPeer, msg);
            } else {
                // 流せないので、gatewayにdelegateする
                innerTS.sendBytes(isSend, myLocator.gateway, msg);
            }
        }
    }

    public void fin() {
    }

	public void setLocator(PeerLocator locator) {
		myLocator = (NestedLocator) locator;
	}

	public boolean canSet(PeerLocator target) {
		return target instanceof NestedLocator;
	}
}
