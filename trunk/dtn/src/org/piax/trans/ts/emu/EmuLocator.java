/*
 * EmuLocator.java
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
 * 2007/10/24 designed and implemented by M. Yoshida.
 * 
 * $Id: EmuLocator.java 210 2010-05-07 14:09:41Z teranisi $
 */

package org.piax.trans.ts.emu;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;

import org.piax.trans.common.PeerLocator;
import org.piax.trans.common.ServiceInfo;
import org.piax.trans.ts.BytesReceiver;
import org.piax.trans.ts.LocatorTransportSpi;

/**
 * @author     Mikio Yoshida
 * @version    2.0.0
 */
public class EmuLocator extends PeerLocator {
    private static final long serialVersionUID = 787534252384772017L;
    public static final byte ID = 10;

    private int vport;
    
    public EmuLocator(int vport) {
        this.vport = vport;
    }
    
    public EmuLocator(ByteBuffer bbuf) {
        vport = bbuf.getInt();
    }
    
    /* (non-Javadoc)
     * @see org.piax.trans.PeerLocator#newLocatorTransportService(
     * org.piax.trans.spi.BytesReceiver, java.util.Collection)
     */
    @Override
    public LocatorTransportSpi newLocatorTransportService(
            BytesReceiver bytesReceiver, Collection<PeerLocator> relays)
            throws IOException {
        return new EmuTransportService(bytesReceiver, this);
    }

    public int getVPort() {
        return vport;
    }

    @Override
    public boolean sameClass(PeerLocator target) {
        if (target == null) return false;
        return this.getClass().equals(target.getClass());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof EmuLocator) {
            return vport == ((EmuLocator) obj).vport;
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return vport;
    }
    
    @Override
    public String toString() {
        return "" + vport;
    }

    @Override
    public String getPeerNameCandidate() {
        return "p" + vport;
    }

    @Override
    public byte getId() {
        return ID;
    }

    @Override
    public void pack(ByteBuffer bbuf) {
        bbuf.putInt(vport);
    }

    @Override
    public int getPackLen() {
        return 4;
    }

    @Override
    public ServiceInfo getServiceInfo() {
    	return ServiceInfo.create(this.getClass().getName(), vport, getPeerNameCandidate(), "");
    }

}
