package org.piax.trans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.piax.ov.OverlayManager;
import org.piax.trans.common.Id;

public class Node {
    public Transport trans;
    private Map<Object,Object> attrs;
    private Id id; 
    public Node self;
    static public final String VIA = "via"; 
    
    public Node(Transport trans, Id id, Node self, Map<Object,Object> attrs) {
        this.trans = trans;
        this.id = id;
        this.self = self;
        this.attrs = attrs;
    }
    
    @SuppressWarnings("unchecked")
    private List<Id> getVia(Map<Object,Object>args) {
        return (List<Id>)args.get(VIA);
    }
    
    private void updateVia(Map<Object,Object>args) {
        List<Id> via = getVia(args);
        if (via == null) {
            via = new ArrayList<Id>();
        }
        else {
            via = new ArrayList<Id>(via);
        }
        via.add(self.id);
        args.put(VIA, via);
    }
    
    public Id getId() {
        return id;
    }
    
    public boolean equals(Node remote) {
        return id.equals(remote.getId());
    }
    
    public Map<Object,Object> sendAndWait(Map<Object,Object> mes, ResponseChecker checker) throws IOException {
        updateVia(mes);
        TransPack ret = trans.sendAndWait(new TransPack(this, self, mes), checker);
        return ret.body;
    }
    
    public void send(Map<Object,Object> mes) throws IOException {
        updateVia(mes);
        trans.send(new TransPack(this, self, mes));
    }
    
    void send(TransPack mes) throws IOException {
        trans.send(mes);
    }
    
    public Object getAttr(Object attrKey) {
        return attrs.get(attrKey);
    }
    
    public void putAttr(Object attrKey, Object attr) {
        attrs.put(attrKey, attr);
    }
    
    public String toString() {
        String ret = "";
        for (Object key: attrs.keySet()) {
            ret += "|" + attrs.get(key);
        }
        ret += "|";
        return ret;
    }
    
}