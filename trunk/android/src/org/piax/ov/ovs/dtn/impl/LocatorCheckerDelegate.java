package org.piax.ov.ovs.dtn.impl;

import org.piax.trans.common.PeerLocator;

public interface LocatorCheckerDelegate {
    public void locatorAvailable(PeerLocator locator);
    public void locatorUnavailable(PeerLocator locator);
}