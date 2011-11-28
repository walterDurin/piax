/*
 * IdTransport.java
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
 * 2007/10/03 designed and implemented by M. Yoshida.
 * 
 * $Id: IdTransport.java 225 2010-06-20 12:34:07Z teranisi $
 */

package org.piax.trans;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.grlea.log.SimpleLogger;
import org.piax.trans.common.PeerId;
import org.piax.trans.common.PeerLocator;
import org.piax.trans.msgframe.MagicNumberConflictException;
import org.piax.trans.msgframe.MessageReachable;
import org.piax.trans.msgframe.MessagingBranch;
import org.piax.trans.msgframe.NoSuchPeerException;

//-- I am waiting for someone to translate the following doc into English. :-)

/**
 * @author     Mikio Yoshida
 * @version    2.0.0
 */
public class IdTransport extends MessagingBranch {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(IdTransport.class);

    private static final byte[] MAGIC_NUM = new byte[] {(byte) 0x81};

    private PeerId peerId;
    private final String peerName;
    private IdResolverIf resolver;

    public IdTransport(String peerName, PeerLocator myLocator)
    throws IOException, MagicNumberConflictException {
        this(null, peerName, myLocator, null);
    }

    public IdTransport(String peerName,
            PeerLocator myLocator, PeerLocator relay)
    throws IOException, MagicNumberConflictException {
        this(null, peerName, myLocator, Collections.singleton(relay));
    }
    
    public IdTransport(String peerName,
            PeerLocator myLocator, Collection<PeerLocator> relays)
    throws IOException, MagicNumberConflictException {
        this(null, peerName, myLocator, relays);
    }
    
    public IdTransport(PeerId peerId, PeerLocator myLocator)
    throws IOException, MagicNumberConflictException {
        this(peerId, myLocator.getPeerNameCandidate(), myLocator, null);
    }

    public IdTransport(PeerId peerId, PeerLocator myLocator, PeerLocator relay)
    throws IOException, MagicNumberConflictException {
        this(peerId, myLocator.getPeerNameCandidate(), myLocator, Collections
                .singleton(relay));
    }

    public IdTransport(PeerId peerId, PeerLocator myLocator,
            Collection<PeerLocator> relays) 
    throws IOException, MagicNumberConflictException {
        this(peerId, myLocator.getPeerNameCandidate(), myLocator, relays);
    }

    /**
     * IdTransportオブジェクトを生成する。
     * ここで、relays の指定は、下位のLocatorTransportServiceにて使用される。
     * 現時点では、NAT越えを行う UdpXLocator のみが relays を使用する。
     * relaysは、join時に使うseedピアとは別の意味を持つので注意。
     * 
     * @param peerId
     * @param peerName
     * @param me
     * @param relays
     * @throws IOException
     */
    public IdTransport(PeerId peerId, String peerName,
            PeerLocator myLocator, Collection<PeerLocator> relays)
            throws IOException, MagicNumberConflictException {
        super(MAGIC_NUM, new LocatorTransport(myLocator, relays));
        log.entry("new");
        
        this.peerId = (peerId != null) ? peerId : PeerId.newId();
        this.peerName = peerName;
    }
    
    /**
     * IdResolverをセットする。
     * 通常は、MSkipGraphを使用する。
     * LocatorTranspportを使用するOverlayを複数起動するのは問題ない。
     * その1つとして、resolve に使えるOverlayをここにセットしておく。
     * 
     * @param resolver
     */
    public void setIdResolver(IdResolverIf resolver) {
        this.resolver = resolver;
        // TODO should be moved to in IdResolver
        // XXX for resolving myself.        
        //resolver.put(peerId, getLocator());
    }
    
    public IdResolverIf getIdResolver() {
        return resolver;
    }

    public void setPeerId(PeerId peerId) {
        this.peerId = peerId;
    }
    
    public final PeerId getPeerId() {
        return peerId;
    }

    @Override
    public MessageReachable getLocalPeer() {
        return peerId;
    }
    
    public final String getPeerName() {
        return peerName;
    }

    public LocatorTransport getLocatorTransport() {
        return (LocatorTransport) getParent();
    }

    public PeerLocator getLocator(Class<? extends PeerLocator> locatorType) {
        return ((LocatorTransport) getParent()).getLocator(locatorType);
    }
    
    public boolean addLocator(PeerLocator myLocator, Collection<PeerLocator> relays)
    throws IOException {
        return ((LocatorTransport) getParent()).addLocator(myLocator, relays);
    }

    public void clearLocators () {
        ((LocatorTransport) getParent()).clearLocators();
    }

    public List<PeerLocator> getLocators() {
        return ((LocatorTransport) getParent()).getLocators();
    }

    public void clearLocator(PeerLocator locator) {
        ((LocatorTransport) getParent()).clearLocator(locator);
    }

    @Override
    public void fin() {
        ((LocatorTransport) getParent()).fin();
        super.fin();
    }
    
    @Override
    protected MessageReachable translate(MessageReachable toPeer)
            throws NoSuchPeerException {
        PeerLocator loc = resolver.getLocator((PeerId) toPeer);
        if (loc == null) {
            throw new NoSuchPeerException();
        }
        return loc;
    }
    
}
