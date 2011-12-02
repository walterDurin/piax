package sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.piax.ov.OverlayManager;
import org.piax.ov.common.KeyComparator;
import org.piax.ov.common.Range;
import org.piax.ov.ovs.skipgraph.SkipGraph;
import org.piax.ov.Executable;

import org.piax.trans.Node;
import org.piax.trans.ReceiveListener;
import org.piax.trans.common.Id;
import org.piax.trans.sim.SimTransport;
import org.piax.trans.sim.SimTransportOracle;
import org.piax.trans.sim.SimTransportOracle.NodeComparator;
import org.piax.trans.util.MersenneTwister;

public class EvalPubSub {
    static ArrayList<Range> subscribers;
    static ArrayList<Integer> publishers;
    static ArrayList<Double> events;
    // prepare dataset;
    private static MersenneTwister rand;
    static {
        long seed = System.nanoTime();
        seed += new Object().hashCode();
        rand = new MersenneTwister(seed);
    } 
    
    static public class OvComparator implements Comparator {  
        public int compare(Object arg0, Object arg1) {  
            return KeyComparator.getInstance().compare((Comparable<?>)((OverlayManager)arg0).getKey(), (Comparable<?>)((OverlayManager)arg1).getKey());
        }  
    }
    
    static public void prepareSampleDataSet() {
        subscribers = new ArrayList<Range>(8);
        subscribers.add(new Range(2, 7));
        subscribers.add(new Range(5, 15));
        subscribers.add(new Range(8, 10));
        subscribers.add(new Range(9, 14));
        subscribers.add(new Range(13, 16));
        subscribers.add(new Range(17, 22));
        subscribers.add(new Range(18, 20));
        subscribers.add(new Range(20, 25));
        publishers = new ArrayList<Integer>();
        publishers.add(0);
        events = new ArrayList<Double>();
        events.add(10.0);
    }
    
    static public void prepareRandomDataSet() {
        int numberOfNodes = 8;
        int numberOfEvents = 1;
        int maxValue = 30;
        int maxRangeWidth = 15;
        // subscribers;
        subscribers = new ArrayList<Range>(numberOfNodes);
        ArrayList<Double> widths = new ArrayList<Double>(numberOfNodes);
        for (int i = 0; i < numberOfNodes; i++) {
            double width = ((double)i * (double)maxRangeWidth) / (double)numberOfNodes;
            widths.add(width);
        }
        Collections.shuffle(widths);
        for (int i = 0; i < numberOfNodes; i++) {
            double min = rand.nextDouble() * maxValue;
            subscribers.add(new Range(min, min + widths.get(i)));
        }
    
        // publishers, events
        publishers = new ArrayList<Integer>(numberOfEvents);
        events = new ArrayList<Double>(numberOfEvents);
        for (int i = 0; i < numberOfEvents; i++) {
            publishers.add((int)(rand.nextDouble() * numberOfNodes));
        }
        Collections.shuffle(publishers);
        for (int i = 0; i < numberOfEvents; i++) {
            events.add(rand.nextDouble() * maxValue);
        }
    
        for (Range range : subscribers) {
            System.out.println("subscribe:" + range);
        }
        for (int i = 0; i < numberOfEvents; i++) {
            System.out.println("publisher:" + publishers.get(i) + "->event:" + events.get(i));
        }
    }
    static double count = 0;
    static double sumHops = 0;
    
    static public void eval() {
        SimTransport seedTrans = new SimTransport();
        OverlayManager seedOv = new OverlayManager(seedTrans);
        seedOv.putRange(new Range((double)0, (double)0));
        List<OverlayManager> ovs = new ArrayList<OverlayManager>();

        for (Range range : subscribers) {
            SimTransport trans = new SimTransport();
            OverlayManager ov = new OverlayManager(trans);
            Node seed = trans.getRemoteNode(seedTrans);
            ov.setSeed(seed);
            ov.putRange(range);
            ovs.add(ov);
        }
        
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        System.out.println("insert message count=" + SimTransportOracle.messageCount());
        SimTransportOracle.clearMessageCount();
        
        // XXX dump all nodes
        List<ReceiveListener> rls = SimTransport.getOracle().getAllReceivers();
        Collections.sort(rls, new OvComparator());
        for (ReceiveListener rl : rls) {
            System.out.println(((OverlayManager)rl).toString());
        }
        
        
        count = 0;
        sumHops = 0;
        Executable ex = new Executable() {
            public void onArrival(Node node, Map<Object, Object> args) {
                Object body = args.get(SkipGraph.Arg.BODY);
                List<Id> via = (List<Id>)args.get(Node.VIA);
                synchronized(this) {
                    count++;
                    sumHops += (via == null ? 0 : via.size());
                }
                //System.out.println("send hops =" + (via == null ? 0 : via.size()));
                //System.out.println("sub=[" + node.getAttr(OverlayManager.KEY) + "," + node.getAttr(OverlayManager.RANGE_END) + "]");
            }
        };
        double sumFairness = 0;
        int countFairness = 0;
        for (int i = 0; i < publishers.size(); i++) {

            double fsum = 0;
            double fssum = 0;
            double fcount = 0;
            
            for (OverlayManager ov : ovs) {
                SimTransport st = (SimTransport)ov.getTransport();
                st.clearCount();
            }
            
            ovs.get(publishers.get(i)).overlapSend(events.get(i), ex);
            
            for (OverlayManager ov : ovs) {
                SimTransport st = (SimTransport)ov.getTransport();
                int load = st.in + st.out; // XXX Is this OK?
                //int load = st.out; // XXX Is this OK?
                if (load > 0) {
                    fsum += load;
                    fssum += (load * load);
                    fcount += 1;
                }
            }            
            sumFairness += ((fsum * fsum) / (fcount * fssum));
            countFairness++;

        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.out.println("ave. fairness index=" + (sumFairness / countFairness));
        System.out.println("ave. hops=" + (sumHops / count));
        System.out.println("publish message count=" + SimTransportOracle.messageCount());
        
        seedTrans.fin();
    }
    
    static public void main(String[] args) {
//        for (int i = 0; i < 10; i++) {
        // prepareRandomDataSet();
        prepareSampleDataSet();
        //OverlayManager.setOverlay("org.piax.ov.ovs.itsg.ITSkipGraphZen");
        //System.out.println("-- ITSG");
        //eval();
        
        OverlayManager.setOverlay("org.piax.ov.ovs.isg.ISkipGraph");
        System.out.println("-- ISG");
        eval();
//        OverlayManager.setOverlay("org.piax.ov.ovs.rksg.RKSkipGraph");
//        System.out.println("-- RKSG");
//        eval();
//        OverlayManager.setOverlay("org.piax.ov.ovs.risg.RISkipGraph");
//        System.out.println("-- RISG");
//        eval();
//        }
    }
}
