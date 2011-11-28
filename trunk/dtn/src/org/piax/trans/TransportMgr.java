/*
 * TransportMgr.java
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
 * 2009/05/26 designed and implemented by M. Yoshida.
 * 
 * $Id: TransportMgr.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.piax.trans.common.PeerId;
import org.piax.trans.common.PeerLocator;
import org.piax.trans.msgframe.MagicNumberConflictException;

/**
 * @author     Mikio Yoshida
 * @version    2.2.0
 */
@Deprecated
class TransportMgr {
    
    private IdTransport transport;
    
    public TransportMgr(PeerId peerId, String peerName,
            PeerLocator myLocator, Collection<PeerLocator> relays)
    throws IOException, MagicNumberConflictException {
        transport = new IdTransport(peerId, peerName, myLocator, relays);
    }
    
//    public void setIdResolver(IdResolver resolver) {
//        this.resolver = resolver;
//    }

    public IdTransport getIdTransport() {
        return transport;
    }

    public IdResolverIf getIdResolver() {
        return transport.getIdResolver();
    }

    public PeerId getPeerId() {
        return transport.getPeerId();
    }

    public String getPeerName() {
        return transport.getPeerName();
    }

    public PeerLocator getLocator() {
        return transport.getLocatorTransport().getLocator();
    }

//    public boolean isActive() {
//        return getIdResolver().isActive();
//    }
//    
//    public boolean activate() throws Exception {
//        return getIdResolver().activate();
//    }
//
//    public boolean activate(PeerLocator relay) throws Exception {
//        return getIdResolver().activate(Collections.singleton(relay));
//    }
//    
//    public boolean inactivate() throws Exception {
//        return getIdResolver().inactivate();
//    }
    
    public void fin() {
//        try {
//            inactivate();
//        } catch (Exception ignore) {
//        }
        getIdResolver().fin();
        transport.fin();
    }
}
