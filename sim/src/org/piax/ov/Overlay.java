package org.piax.ov;

import java.util.List;
import java.util.Map;

import org.piax.ov.common.Range;
import org.piax.trans.Node;

public interface Overlay {
    public void insert(Node introducer);
    public Node search(Comparable<?> key);
    public List<Node> search(Range key);
    public List<Node> overlapSearch(Range key);
    List<Node> overlapSearch(Comparable<?> key);
    public void delete();
    public void onReceive(Node sender, Map<Object,Object> mes);
}
