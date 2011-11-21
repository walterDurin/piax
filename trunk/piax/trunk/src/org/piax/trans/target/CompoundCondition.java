package org.piax.trans.target;

import java.io.Serializable;

import org.piax.trans.Target;

public class CompoundCondition extends Target {
    public CompoundCondition(Serializable condition) {
        super(condition);
    }
}
