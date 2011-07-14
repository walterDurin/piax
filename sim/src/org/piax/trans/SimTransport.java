package org.piax.trans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.piax.ov.OverlayManager;
import org.piax.ov.common.KeyComparator;
import org.piax.trans.common.Id;

public class SimTransport implements Transport,Runnable {
    HashMap<Id,OverlayManager> nodeMap;
    BlockingQueue<TransPack> queue;
    
    static public enum Param {
       NestedWait 
    };
    
    // 
    public boolean nestedWait = true;
    //Id id;
    Thread t;
    int ID_LENGTH = 16;

    class Monitor {
        Id id;
        ResponseChecker checker;
        Object lock;
        public TransPack receivedMessage;
        public Monitor(Id id, ResponseChecker checker, Object lock) {
            this.id = id;
            this.checker = checker;
            this.lock = lock;
            this.receivedMessage = null;
        }
    }
    ArrayList<Monitor> waits;
    Date startTime;

    private TransPack endMessage() {
        return new TransPack(null, null, null);
    }
    
    private boolean isEndMessage(TransPack mes) {
        return mes.receiver == null;
    }

    public SimTransport() {
        //id = Id.newId(ID_LENGTH);
        queue = new LinkedBlockingQueue<TransPack>();
        nodeMap = new HashMap<Id,OverlayManager> ();
        waits = new ArrayList<Monitor>();
        startTime = new Date();
        t = new Thread(this);
        t.start();
    }
    
    public void setParameter(Object key, Object value) {
        if (key == Param.NestedWait) {
            nestedWait = ((Boolean)value).booleanValue();
        }
    }
    
    public void addReceiveListener(ReceiveListener listener) {
        // this is a trick to support each listener. Only applicable for ov.OverlayManager
        nodeMap.put(((OverlayManager)listener).id, (OverlayManager)listener);
        
    }
    public Id getId() {
        // This is a trick to mimic node's transport.
        return Id.newId(ID_LENGTH);
    }
    
    Monitor getWait(TransPack mes) {
        Monitor p = null;
        for (Monitor pair : waits) {
            if (pair.checker.isWaitingFor(mes.body)) {
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
            TransPack tp = null;
            try {
                tp = queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (tp != null) {
                if (isEndMessage(tp)) {
                    System.out.println("END");
                    break;
                }
                OverlayManager node = nodeMap.get(tp.receiver.getId());
                
                // replace self of remote node accessor
                for (Object key : tp.body.keySet()) {
                    Object value = tp.body.get(key);
                    if (value instanceof Node) {
                        ((Node)value).self = node.getNode(); 
                    }
                }
                tp.sender.self = node.getNode();
                // tp.receiver.self = node;
                
                Monitor monotor = getWait(tp);
                if (monotor != null) {
                    synchronized(monotor.lock) {
                        monotor.receivedMessage = tp;
                        monotor.lock.notify();
                    }
                    Thread.yield();
                }
                else {
                    // for better performance...
                    // if it will use sync method in onReceive method, it must start receiver thread. 
                    if (nestedWait) {
                        new Thread(new ReceiverThread(node, tp)).start();
                    }
                    else {
                        // node.receivedMessage = tp;
                        node.onReceive(tp.sender, tp.body);
                        Thread.yield();
                    }
                }
            }
        }
    }
    
    public void fin() {
        queue.add(endMessage());
    }
    
    public long getElapsedTime() {
        return new Date().getTime() - startTime.getTime();
    }
    
    class ReceiverThread implements Runnable {
        OverlayManager node;
        TransPack mes;
        
        public ReceiverThread(OverlayManager node, TransPack mes) {
            this.node = node;
            this.mes = mes;
        }
        public void run() {
            //node.receivedMessage = mes;
            node.onReceive(mes.sender, mes.body);
        }
    }
    
    public Node getNode(OverlayManager ov, OverlayManager targetOv) {
        Node ret = targetOv.getNode();
        ret.self = ov.getNode();
        return ret;
    }
    
    Monitor addWait(Id id, ResponseChecker checker, Object lock) {
        Monitor ret = new Monitor(id, checker, lock);
        waits.add(ret);
        return ret;
    }

    public TransPack sendAndWait(TransPack pack, ResponseChecker checker) throws IOException {
        Object lock = new Object();
        Monitor pair = addWait(pack.sender.getId(), checker, lock);
        synchronized(lock) {
            try {
                queue.add(pack);
                lock.wait();
                Thread.yield();
            } catch (InterruptedException e) {
                throw new IOException("interrupted");
            }
        }
        TransPack received = pair.receivedMessage;
        if (received == null) {
            System.err.println("timed out receiving.");
        }
        return received;
    }

    public void send(TransPack pack) {
        queue.add(pack);
        Thread.yield();
    }
    
    public class NodeComparator implements Comparator {  
        public int compare(Object arg0, Object arg1) {  
            return KeyComparator.getInstance().compare(nodeMap.get((Id)arg0).getKey(), nodeMap.get((Id)arg1).getKey());
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
            OverlayManager ov = nodeMap.get(id);
            System.out.println(ov.o.toString());
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
            OverlayManager ov = nodeMap.get(id);
            sb.append("ID=" + ov.id + "\n");
            sb.append("Key=" + ov.getKey() + "\n");
            sb.append("MV=" + ov.o.toString() + "\n");
        }
        return sb.toString();
    }
}