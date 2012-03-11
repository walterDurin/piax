package org.piax.gnt;

public class Target {
    protected Object condition;
    protected Object filter;

    public Target(Object condition) {
        this.condition = condition;
        this.filter = null;
    }
    public Target(Object condition, Object filter) {
        this.condition = condition;
        this.filter = filter;
    }
}
