/*
 * EmuTransport.java -- Emulation version of LocatorTransportService2
 * 
 * Copyright (c) 2006-2007 Osaka University
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
 *
 * $Id: EmuTransportService.java 290 2010-10-05 05:58:57Z teranisi $
 */

package org.piax.trans.ts.emu;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.piax.trans.common.PeerLocator;
import org.piax.trans.ts.BytesReceiver;
import org.piax.trans.ts.LocatorTransportSpi;

/**
 * @author     teranisi
 */
public class EmuTransportService implements LocatorTransportSpi {

    public static int hopDelay = 0;

    BytesReceiver bytesReceiver;
    EmuLocator peerLocator;     // my peer locator
    
    public EmuTransportService(BytesReceiver bytesReceiver, EmuLocator peerLocator)
            throws IOException {
        this.bytesReceiver = bytesReceiver;
        this.peerLocator = peerLocator;
        EmuPool.add(this);
    }

    public void putNewLocator(PeerLocator newLoc) {
        EmuPool.add(this);
    }
    
    public void fadeoutLocator(PeerLocator oldLoc) {
        EmuPool.remove((EmuLocator) oldLoc);
    }

    /* (non-Javadoc)
     * @see org.piax.trans.spi.LocatorTransportSpi#getLocator()
     */
    public PeerLocator getLocator() {
        return peerLocator;
    }
    
    public boolean canSend(PeerLocator target) {
        return peerLocator.sameClass(target);
    }

    /* (non-Javadoc)
     * @see org.piax.trans.spi.LocatorTransportSpi#fin()
     */
    public void fin() {
        EmuPool.remove((EmuLocator) peerLocator);
    }

    public void sendBytes(boolean isSend, PeerLocator toPeer, ByteBuffer msg) 
    throws IOException {
        try {
            EmuTransportService ts = EmuPool.lookup((EmuLocator) toPeer);
            ts.receive(msg);
        } catch (IOException e) {
//            sim.Params.numOfSendErr++;
            throw e;
        }
    }

    void receive(ByteBuffer msg) {
        // ここに1hopのディレイを挟めばよい
      if (hopDelay > 0) {
          try {
              Thread.sleep(hopDelay);
          } catch (InterruptedException ignore) {}
      }
      bytesReceiver.receiveBytes(this, msg);
    }

	public void setLocator(PeerLocator locator) {
		this.peerLocator = (EmuLocator) locator;
	}

	public boolean canSet(PeerLocator target) {
		// TODO Auto-generated method stub
		return target instanceof EmuLocator;
	}
}
