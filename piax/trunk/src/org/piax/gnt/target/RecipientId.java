package org.piax.gnt.target;

import org.piax.gnt.Target;
import org.piax.trans.common.PeerId;

public class RecipientId extends Target {
    static RecipientId BROADCAST_PEER_ID = new RecipientId(PeerId.zeroId());
    public RecipientId(PeerId id) {
        super(id);
    }
    static public RecipientId broadcastId() {
        return BROADCAST_PEER_ID;
    }
    public boolean isBroadcast(RecipientId target) {
        return target.getId().isZero();
    }
    public PeerId getId() {
        return (PeerId) condition;
    }

}