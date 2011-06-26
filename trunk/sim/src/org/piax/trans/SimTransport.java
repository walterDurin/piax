package org.piax.trans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.piax.ov.Node;
import org.piax.ov.common.KeyComparator;
import org.piax.trans.common.Id;

public class SimTransport implements Runnable {
    HashMap<Id,Node> nodeMap;
    BlockingQueue<Message> queue;
    // 
    public boolean nestedWait = true;
    //Id id;
    Thread t;
    int ID_LENGTH = 16;

    class Pair {
        ResponseChecker checker;
        Object lock;
        public Pair(ResponseChecker checker, Object lock) {
            this.checker = checker;
            this.lock = lock;
        }
    }
    ArrayList<Pair> waits;
    Date startTime;

    private Message endMessage() {
        return new Message(null, null, null);
    }
    
    private boolean isEndMessage(Message mes) {
        return mes.to == null;
    }

    public SimTransport() {
        //id = Id.newId(ID_LENGTH);
        queue = new LinkedBlockingQueue<Message>();
        nodeMap = new HashMap<Id,Node> ();
        waits = new ArrayList<Pair>();
        startTime = new Date();
        t = new Thread(this);
        t.start();

    }
    public void addReceiveListener(Node node) {
        nodeMap.put(node.id, node);
    }
    public Id getId() {
        // This is the trick to mimic node's transport.
        return Id.newId(ID_LENGTH);
    }
    
    Pair getWait(Message mes) {
        Pair p = null;
        for (Pair pair : waits) {
            if (pair.checker.isWaitingFor(mes)) {
                p = pair;
                break;
            }
        }
        if (p != null) {
            waits.remove(p);
        }
        return p;
    }

    public void run() {
        while(true) {
            Message message = null;
            try {
                message = queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (message != null) {
                if (isEndMessage(message)) {
                    break;
                }
                Node node = nodeMap.get(message.to);
                Pair pair = getWait(message);
                if (pair != null) {
                    synchronized(pair.lock) {
                        node.receivedMessage = message;
                        pair.lock.notify();
                    }
                    Thread.yield();
                }
                else {
                    // for better performance...
                    // if it will use sync method in onReceive method, it must start receiver thread. 
                    if (nestedWait) {
                        new Thread(new ReceiverThread(node, message)).start();
                    }
                    else {
                        node.receivedMessage = message;
                        node.onReceive(message);
                        Thread.yield();
                    }
                }
            }
        }
    }
    
    public void fin() {
        queue.add(endMessage());
    }
    
    class ReceiverThread implements Runnable {
        Node node;
        Message mes;
        
        public ReceiverThread(Node node, Message mes) {
            this.node = node;
            this.mes = mes;
        }
        public void run() {
            node.receivedMessage = mes;
            node.onReceive(mes);
        }
    }
    
    void addWait(ResponseChecker checker, Object lock) {
        waits.add(new Pair(checker, lock));
    }

    public Message sendAndWait(Message mes, ResponseChecker checker) throws IOException {
        Node node = nodeMap.get(mes.from);
        Object lock = new Object();
        addWait(checker, lock);
        synchronized(lock) {
            try {
                queue.add(mes);
                node.isWaiting = true;
                node.receivedMessage = null;
                lock.wait();
                Thread.yield();
            } catch (InterruptedException e) {
                throw new IOException("interrupted");
            }
        }
        node.isWaiting = false;
        Message received = node.receivedMessage;
        if (received == null) {
            System.err.println("timed out receiving.");
        }
        return received;
    }

    public void send(Message mes) {
        queue.add(mes);
        Thread.yield();
    }
    
    // these methods should be strictly implemented.
    public Comparable<?> getKey(Id target) {
        Node node = nodeMap.get(target);
        return node.sg.getKey();
    }

    public int getMaxLevelOp(Id target) {
        Node node = nodeMap.get(target);
        return node.sg.getMaxLevel();
    }

    public Id getNeighborOp(Id target, int side, int level) {
        Node node = nodeMap.get(target);
        return node.sg.getNeighborOp(side, level);
    }
    
    public class NodeComparator implements Comparator {  
        public int compare(Object arg0, Object arg1) {  
            return KeyComparator.getInstance().compare(nodeMap.get((Id)arg0).sg.key,nodeMap.get((Id)arg1).sg.key);
        }  
    }
    
    public void dump() {
        Object[] objs = nodeMap.keySet().toArray();
        ArrayList<Object> o = new ArrayList<Object>();
        for (Object obj : objs) {
            o.add(obj);
        }
        Collections.sort(o, new NodeComparator());
        
        for (Object obj : o) {
            Id id = (Id) obj;
            Node node = nodeMap.get(id);
            String s = "";
            s += "ID=" + node.id + (node.sg.deleteFlag? "(DELETED)" : "") + "\n";
            s += "Key=" + node.sg.getKey() + "\n";
            s += "MV=" + node.sg.m.toString() + "\n";
            s += "Table:" + node.sg.neighbors.toString() + "\n";
            System.out.println(s);
        }

    }
    
    public String toString() {
        Object[] objs = nodeMap.keySet().toArray();
        StringBuilder sb = new StringBuilder();

        ArrayList<Object> o = new ArrayList<Object>();
        for (Object obj : objs) {
            o.add(obj);
        }
        Collections.sort(o, new NodeComparator());
        
        for (Object obj : o) {
            Id id = (Id) obj;
            Node node = nodeMap.get(id);
            sb.append("ID=" + node.id + "\n");
            sb.append("Key=" + node.sg.getKey() + "\n");
            sb.append("MV=" + node.sg.m.toString() + "\n");
            sb.append("Table:" + node.sg.neighbors.toString() + "\n");
        }
        return sb.toString();
    }
}