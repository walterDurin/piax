package org.piax.trans.target;

public class Anycast extends RangeCondition {
    public Anycast(Range range, int k) {
        super(range);
        filter = k;
    }
}