package org.piax.ov;

import java.util.Map;

import org.piax.trans.Node;

public interface Executable {
    public void onArrival(Node node, Map<Object,Object> args);
}
