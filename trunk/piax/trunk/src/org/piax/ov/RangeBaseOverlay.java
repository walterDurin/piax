package org.piax.ov;

import org.piax.gnt.SecurityManager;
import org.piax.gnt.Transport;
import org.piax.gnt.target.Range;

public abstract class RangeBaseOverlay extends Overlay {
    public RangeBaseOverlay(Transport trans, String overlayId, SecurityManager smgr) {
        super(trans, overlayId, smgr);
    }
    public abstract void registerRange(Range range);
    public abstract void unregisterRange(Range range);
}
