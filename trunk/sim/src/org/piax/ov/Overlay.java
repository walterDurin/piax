package org.piax.ov;

import java.util.Map;

import org.piax.trans.Node;

public interface Overlay {
    public void insert(Node introducer);
    public Node search(Comparable<?> key);
    public void delete();
    public void onReceive(Node sender, Map<Object,Object> mes);
}
