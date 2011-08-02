package org.piax.trans.sim;

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
import org.piax.trans.Node;
import org.piax.trans.ReceiveListener;
import org.piax.trans.ResponseChecker;
import org.piax.trans.TransPack;
import org.piax.trans.Transport;
import org.piax.trans.common.Id;

public class SimTransportOracle implements Runnable {
    HashMap<Id, ReceiveListener> nodeMap;
    BlockingQueue<TransPack> queue;
    
    static public enum Param {
       NestedWait 
    };
    
    // 
    public boolean nestedWait = true;
    //Id id;
    Thread t;
    int ID_LENGTH = 16;
    
    public static int messageCount;

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
    static Date startTime;

    private TransPack endMessage() {
        return new TransPack(null, null, null);
    }
    
    private boolean isEndMessage(TransPack mes) {
        return mes.receiver == null;
    }

    public SimTransportOracle() {
        //id = Id.newId(ID_LENGTH);
        queue = new LinkedBlockingQueue<TransPack>();
        nodeMap = new HashMap<Id,ReceiveListener> ();
        waits = new ArrayList<Monitor>();
        startTime = new Date();
        clearMessageCount();
        t = new Thread(this);
        t.start();
    }
    
    public static void clearMessageCount() {
        messageCount = 0;
    }
    
    public static int messageCount() {
        return messageCount;
    }
    
    public static void countMessage() {
        messageCount++;
    }
    
    public void setParameter(Object key, Object value) {
        if (key == Param.NestedWait) {
            nestedWait = ((Boolean)value).booleanValue();
        }
    }
    
    public void addReceiveListener(ReceiveListener listener) {
        // this is a trick to support each listener. Only applicable for ov.OverlayManager
        nodeMap.put(((SimTransport)listener).id, listener);
        
    }
    public Id getId() {
        // This is a trick to mimic node's transport.
        return Id.newId(ID_LENGTH);
    }
    
    Monitor getWait(TransPack mes) {
        synchronized(waits) {
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
                    break;
                }
                ReceiveListener ov = nodeMap.get(tp.receiver.getId());
                
                // replace self of remote node accessor
                for (Object key : tp.body.keySet()) {
                    Object value = tp.body.get(key);
                    if (value instanceof Node) {
                        ((Node)value).self = ((Transport)ov).getSelfNode(); 
                    }
                }
                tp.sender.self = ((Transport)ov).getSelfNode();
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
                        new Thread(new ReceiverThread(ov, tp)).start();
                    }
                    else {
                        // node.receivedMessage = tp;
                        ov.onReceive(tp.sender, tp.body);
                        Thread.yield();
                    }
                }
            }
        }
    }
    
    public void fin() {
        queue.add(endMessage());
    }
    
    static public long getElapsedTime() {
        return new Date().getTime() - startTime.getTime();
    }
    
    class ReceiverThread implements Runnable {
        ReceiveListener node;
        TransPack mes;
        
        public ReceiverThread(ReceiveListener node, TransPack mes) {
            this.node = node;
            this.mes = mes;
        }
        public void run() {
            //node.receivedMessage = mes;
            node.onReceive(mes.sender, mes.body);
        }
    }
    
    public Node getRemoteNode(Node self, Node targetNode) {
    	Node ret = new Node(targetNode.trans, targetNode.getId(), null, targetNode.trans.getAttrs());
    	ret.self = self;
    	return ret;
    }
    
//    public Node getNode(OverlayManager ov, OverlayManager targetOv) {
//        Node ret = targetOv.getNode();
//        ret.self = ov.getNode();
//        return ret;
//    }
    
    Monitor addWait(Id id, ResponseChecker checker, Object lock) {
        synchronized(waits) {
        Monitor ret = new Monitor(id, checker, lock);
        waits.add(ret);
        return ret;
        }
    }

    public TransPack sendAndWait(TransPack pack, ResponseChecker checker) throws IOException {
        Object lock = new Object();
        Monitor pair = addWait(pack.sender.getId(), checker, lock);
        synchronized(lock) {
            try {
                queue.add(pack);
                countMessage();
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
        countMessage();
        Thread.yield();
    }
    
    public class NodeComparator implements Comparator {  
        public int compare(Object arg0, Object arg1) {  
            return KeyComparator.getInstance().compare((Comparable<?>)((SimTransport)(nodeMap.get((Id)arg0))).getAttr(OverlayManager.KEY), (Comparable<?>)((SimTransport)(nodeMap.get((Id)arg1))).getAttr(OverlayManager.KEY));
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
            System.out.println(nodeMap.get(id));
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
            sb.append(nodeMap.get(id));
            //sb.append("ID=" + ov.id + "\n");
            //sb.append("Key=" + ov.getKey() + "\n");
            //sb.append("MV=" + ov.o.toString() + "\n");
        }
        return sb.toString();
    }
}