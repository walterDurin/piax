/*
 * UdpLocator.java
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
 * $Id: UdpLocator.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans.ts.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collection;

import org.piax.trans.common.PeerLocator;
import org.piax.trans.ts.BytesReceiver;
import org.piax.trans.ts.InetLocator;
import org.piax.trans.ts.LocatorTransportSpi;

/**
 * UDP用LocatorTransportサービスのためのPeerLocatorを表現するクラス。
 * 
 * @author     Mikio Yoshida
 * @version    2.1.0
 */
public class UdpLocator extends InetLocator {
    private static final long serialVersionUID = 2241638173501270770L;
    public static final byte ID = 1;

    public UdpLocator(InetSocketAddress addr) {
        super(addr);
    }

    public UdpLocator(ByteBuffer bbuf) throws UnknownHostException {
        super(bbuf);
    }

    /* (non-Javadoc)
     * @see org.piax.trans.PeerLocator#newLocatorTransportService(
     * org.piax.trans.spi.BytesReceiver, java.util.Collection)
     */
    @Override
    public LocatorTransportSpi newLocatorTransportService(
            BytesReceiver bytesReceiver, Collection<PeerLocator> relays)
            throws IOException {
        return new UdpTransportService(bytesReceiver, this);
    }

    @Override
    public byte getId() {
        return ID;
    }
}
