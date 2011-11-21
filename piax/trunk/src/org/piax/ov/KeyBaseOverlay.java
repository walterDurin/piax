package org.piax.ov;

import org.piax.trans.SecurityManager;
import org.piax.trans.Transport;

public abstract class KeyBaseOverlay extends Overlay {
    public KeyBaseOverlay(Transport trans, String overlayId, SecurityManager smgr) {
        super(trans, overlayId, smgr);
    }
    public abstract void registerKey(Comparable<?> key);
    public abstract void unregisterKey(Comparable<?> key);
}
