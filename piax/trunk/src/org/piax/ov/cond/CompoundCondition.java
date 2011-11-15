package org.piax.ov.cond;

import java.io.Serializable;

public class CompoundCondition extends Condition {
    public CompoundCondition(Serializable constraint) {
        super(constraint);
    }
}
