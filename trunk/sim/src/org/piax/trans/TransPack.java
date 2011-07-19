package org.piax.trans;

import java.util.ArrayList;
import java.util.Map;
import org.piax.trans.common.Id;

public class TransPack {
    public Node sender;
    public Node receiver;
    public Map<Object,Object> body;
    public ArrayList<Id> via; 
    
    public TransPack(Node receiver, Node sender, Map<Object,Object> body) {
        this.sender = sender;
        this.receiver = receiver;
        this.body = body;
    }
    
    public void addVia(Node node) {
        via.add(node.getId());
    }
    
}