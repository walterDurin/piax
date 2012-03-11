package org.piax.gnt.handover;

import org.piax.trans.common.PeerId;
import org.piax.trans.common.PeerLocator;

public interface AcceptanceDelegate {
    boolean checkAcceptFrom(PeerId id, PeerLocator locator);
    void acceptedFrom(PeerId id, PeerLocator locator);
    void rejectedFrom(PeerId id, PeerLocator locator);
}