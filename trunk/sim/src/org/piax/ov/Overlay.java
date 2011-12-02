package org.piax.ov;

import java.util.List;
import java.util.Map;

import org.piax.ov.common.Range;
import org.piax.trans.Node;

public interface Overlay {
    // Returns true if insert was succeeded.
    public boolean insert(Node introducer);
    
    public Node search(Comparable<?> key);
    public List<Node> search(Range key);
    public List<Node> search(Range key, int k);
    public List<Node> overlapSearch(Comparable<?> key);
    public List<Node> overlapSearch(Range key);
    public List<SearchResult> overlapSearchWithRoute(Range key);
    
    public void send(Comparable<?> key, Object body);
    public void send(Range key, Object body);
    public void send(Range key, int k, Object body);
    public void overlapSend(Comparable<?> key, Object body);
    public void overlapSend(Range key, Object body);

    public void delete();
    public void onReceive(Node sender, Map<Object,Object> mes);
}
