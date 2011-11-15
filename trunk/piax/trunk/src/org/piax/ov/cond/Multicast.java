package org.piax.ov.cond;

import java.util.List;

import org.piax.trans.common.PeerId;

public class Multicast extends Condition {
    public Multicast(List<PeerId> ids) {
        super(ids);
    }
}