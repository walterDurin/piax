package org.piax.trans;

import org.piax.trans.target.Broadcast;

public class Target {
    protected Object condition;
    protected Object filter;
    static public Broadcast BROADCAST = new Broadcast();

    public Target(Object condition) {
        this.condition = condition;
        this.filter = null;
    }
    public Target(Object condition, Object filter) {
        this.condition = condition;
        this.filter = filter;
    }
}
