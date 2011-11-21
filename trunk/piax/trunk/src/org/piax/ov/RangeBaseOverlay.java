package org.piax.ov;

import org.piax.ov.target.Range;

public abstract class RangeBaseOverlay extends Overlay {
    public RangeBaseOverlay(String overlayId, SecurityManager smgr) {
        super(overlayId, smgr);
    }
    public abstract void registerRange(Range range);
    public abstract void unregisterRange(Range range);
}
