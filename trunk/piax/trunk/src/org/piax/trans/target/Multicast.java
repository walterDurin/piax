package org.piax.trans.target;

import java.util.List;

import org.piax.trans.Target;
import org.piax.trans.common.PeerId;

public class Multicast extends Target {
    public Multicast(List<PeerId> ids) {
        super(ids);
    }
}