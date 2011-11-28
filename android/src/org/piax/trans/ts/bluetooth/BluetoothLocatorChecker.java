package org.piax.trans.ts.bluetooth;

import org.piax.ov.ovs.dtn.impl.LocatorChecker;
import org.piax.ov.ovs.dtn.impl.LocatorCheckerDelegate;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

public class BluetoothLocatorChecker extends LocatorChecker {
    Context ctxt;
    LocatorCheckerDelegate delegate;
    BluetoothLocator lastLocator;
    
    public BluetoothLocatorChecker(Context ctxt, LocatorCheckerDelegate delegate) {
        this.ctxt = ctxt;
        this.delegate = delegate;
        lastLocator = null;
        notifyLocator();
    }
    
    @Override
    public void notifyLocator() {
        BluetoothLocator l = BluetoothLocator.getDefaultLocator();
        if (l == null) {
            if (lastLocator != null) {
                delegate.locatorUnavailable(lastLocator);
                lastLocator = null;
            }
        }
        else {
            if (lastLocator == null) {
                lastLocator = l;
                delegate.locatorAvailable(lastLocator);
            }
        }
    }
}
