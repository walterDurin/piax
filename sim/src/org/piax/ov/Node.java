package org.piax.ov;

import org.piax.ov.ovs.skipgraph.SkipGraph;
import org.piax.trans.Message;
import org.piax.trans.SimTransport;
import org.piax.trans.common.Id;

public class Node {

    SimTransport trans;
    public    Message receivedMessage;
    public    boolean isWaiting;

    public Id id;          // Peer Id
    public Id seedId;      // Introducer Id
    
    public Comparable<?> key;

    public SkipGraph sg;

    public Node(SimTransport trans) {
        this.trans = trans;
        this.id = trans.getId();
        this.seedId = id;
        trans.addReceiveListener(this);
    }
    
    public Node(SimTransport trans, Id seedId) {
        this.trans = trans;
        this.id = trans.getId();
        this.seedId = seedId;
        trans.addReceiveListener(this);
    }
    
    public void onReceive(Message message) {
        sg.onReceive(message);
    }
    
    public void setSeed(Id seedId) {
        this.seedId = seedId;
    }
    
    public void addKey(Comparable<?> key) {
        this.key = key;
        sg = new SkipGraph(trans, id, key);
        sg.insert(seedId);
    }

    public Id search(Comparable<?> searchKey) {
        return sg.search(searchKey);
    }
    
    public void delete() {
        sg.delete();
    }
}