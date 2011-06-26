package org.piax.trans;

import java.util.ArrayList;
import java.util.Map;

import org.piax.trans.common.Id;

public class Message {
    public Id to;
    public Id from;
    public Map args;
    public ArrayList<Id> via;
    public Message(Id to, Id from, Map args) {
        this.to = to;
        this.from = from;
        this.args = args;
        via = new ArrayList<Id>();
    }
    public void addVia(Id id) {
        via.add(id);
    }
    public Message(Id to, Id from, Map args, ArrayList<Id> via) {
        this.to = to;
        this.from = from;
        this.args = args;
        this.via = via;
        via.add(from);
    }
}