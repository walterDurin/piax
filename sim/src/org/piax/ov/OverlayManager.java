package org.piax.ov;

import java.util.HashMap;
import java.util.Map;

import java.lang.reflect.Constructor;

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
    
    public Map<Object,Object> attrs;
    
    public static String KEY = "key";

    static public void setOverlay(String name){
        ovClass = name;
    }

    private void setupOverlay() {
        Class clazz;
        try {
            clazz = Class.forName(ovClass);
            Class[] types = {org.piax.trans.Node.class};
            Constructor constructor = clazz.getConstructor(types);
            Object[] args = {getNode()};
            o = (Overlay)constructor.newInstance(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public OverlayManager(Transport trans) {
        this.trans = trans;
        this.id = trans.getId();
        this.seed = getNode();
        attrs = new HashMap<Object,Object>();
        trans.addReceiveListener(this);
    }
    
    public OverlayManager(Transport trans, Node seed) {
        this.trans = trans;
        this.id = trans.getId();
        this.seed = seed;
        attrs = new HashMap<Object,Object>();
        trans.addReceiveListener(this);
    }

    public void setOverlay(Overlay o) {
        this.o = o;
    }
    
    // this can be used as a sender info.
    public Node getNode() {
        Node n = new Node(trans, id, null, getAttrs());
        return n;
    }
    
    public Map<Object,Object> getAttrs() {
        //Map<Object,Object> attrs = new HashMap<Object,Object>();
        //attrs.put(KEY, key);
        return attrs;
    }

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
        return (Comparable<?>)attrs.get(KEY);
    }
    
    public void putKey(Comparable<?> key) {
        attrs.put(KEY, key);
        setupOverlay();
        o.insert(seed);
    }

    public Node search(Comparable<?> searchKey) {
        return o.search(searchKey);
    }
    
    public void delete() {
        o.delete();
    }
}