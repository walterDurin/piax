package org.piax.gnt.target;

import org.piax.trans.common.PeerId;
import org.piax.trans.common.PeerLocator;

public class RecipientIdWithLocator extends RecipientId {
    PeerLocator locator;
    public RecipientIdWithLocator(PeerId id, PeerLocator locator) {
        super(id);
        this.locator = locator;
    }
    public PeerLocator getLocator() {
        return locator;
    }
}
