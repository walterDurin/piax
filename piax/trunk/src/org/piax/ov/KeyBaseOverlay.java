package org.piax.ov;

import org.piax.trans.SecurityManager;

public abstract class KeyBaseOverlay extends Overlay {
    public KeyBaseOverlay(String overlayId, SecurityManager smgr) {
        super(overlayId, smgr);
    }
    public abstract void registerKey(Comparable<?> key);
    public abstract void unregisterKey(Comparable<?> key);
}
