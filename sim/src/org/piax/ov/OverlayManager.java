package org.piax.ov;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.lang.reflect.Constructor;

import org.piax.ov.common.Range;
import org.piax.trans.ReceiveListener;
import org.piax.trans.Node;
import org.piax.trans.Transport;
import org.piax.trans.common.Id;

public class OverlayManager implements ReceiveListener {
    Transport trans;
    public Node seed;      // Introducer
    
    public Id id;                // Peer Id
    //public Comparable<?> key;
    public Overlay o;
    static String ovClass;
    
    public static String KEY = "key";
    public static String RANGE_END = "rangeEnd";
    public static String MAX = "max";

    static public void setOverlay(String name){
        ovClass = name;
    }

    private void setupOverlay() {
        Class clazz;
        try {
            clazz = Class.forName(ovClass);
            Class[] types = {org.piax.trans.Node.class};
            Constructor constructor = clazz.getConstructor(types);
            Object[] args = {trans.getSelfNode()};
            o = (Overlay)constructor.newInstance(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public OverlayManager(Transport trans) {
        this.trans = trans;
        this.id = trans.getId();
        this.seed = trans.getSelfNode();
        trans.addReceiveListener(this);
    }
    
    public OverlayManager(Transport trans, Node seed) {
        this.trans = trans;
        this.id = trans.getId();
        this.seed = seed;
        trans.addReceiveListener(this);
    }

    public void setOverlay(Overlay o) {
        this.o = o;
    }
    
    // this can be used as a sender info.
//  public Node getNode() {
//	Node n = new Node(trans, id, null, trans.getAttrs());
//	return n;
//}
    
    public Id getId() {
        return id;
    }
    
    public void onReceive(Node sender, Map<Object,Object> mes) {
        o.onReceive(sender, mes);
    }
    
    public void setSeed(Node seed) {
        this.seed = seed;
    }
    
    public Comparable<?> getKey() {
        return (Comparable<?>)trans.getAttr(KEY);
    }
    
    public void putKey(Comparable<?> key) {
        trans.putAttr(KEY, key);
        setupOverlay();
        o.insert(seed);
    }
    
    public Range getRange() {
        return new Range((Comparable<?>)trans.getAttr(KEY), (Comparable<?>)trans.getAttr(RANGE_END));
    }

    public void putRange(Range range) {
        trans.putAttr(KEY, range.min);
        trans.putAttr(RANGE_END, range.max);
        setupOverlay();
        o.insert(seed);
    }
    
    public Node search(Comparable<?> searchKey) {
        return o.search(searchKey);
    }
    
    public List<Node> search(Range searchRange) {
        return o.search(searchRange);
    }
    
    public void delete() {
        o.delete();
    }
}