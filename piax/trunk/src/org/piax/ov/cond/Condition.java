package org.piax.ov.cond;

public class Condition {
    Object constraint;
    Object filter;
    public Condition(Object constraint) {
        this.constraint = constraint;
        this.filter = null;
    }
    public Condition(Object constraint, Object filter) {
        this.constraint = constraint;
        this.filter = filter;
    }
}
