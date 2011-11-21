package org.piax.ov;

import org.piax.trans.SecurityManager;
import org.piax.trans.Transport;
import org.piax.trans.target.Range;

public abstract class RangeBaseOverlay extends Overlay {
    public RangeBaseOverlay(Transport trans, String overlayId, SecurityManager smgr) {
        super(trans, overlayId, smgr);
    }
    public abstract void registerRange(Range range);
    public abstract void unregisterRange(Range range);
}
