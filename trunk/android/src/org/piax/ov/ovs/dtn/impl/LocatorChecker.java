package org.piax.ov.ovs.dtn.impl;

public abstract class LocatorChecker implements Runnable{
    
    public void notifyLocator() {}

    @Override
    public void run() {
        notifyLocator();
    }
}
