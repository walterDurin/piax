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
    
    static public void prepareSampleDataSet2() {
        subscribers = new ArrayList<Range>(8);
        subscribers.add(new Range((double)2,(double)50));
        subscribers.add(new Range((double)20,(double)76));
        subscribers.add(new Range((double)31,(double)47));
        subscribers.add(new Range((double)35,(double)43));
        subscribers.add(new Range((double)55,(double)127));
        subscribers.add(new Range((double)60,(double)124));
        subscribers.add(new Range((double)71,(double)111));
        subscribers.add(new Range((double)72,(double)96));
        subscribers.add(new Range((double)92,(double)124));
        publishers = new ArrayList<Integer>();
        publishers.add(0);
        events = new ArrayList<Double>();
        events.add(50.0);
    }
    static public void prepareSampleDataSet() {
        subscribers = new ArrayList<Range>(8);
        subscribers.add(new Range((double)2, (double)20));
        subscribers.add(new Range((double)3, (double)21));
        subscribers.add(new Range((double)4, (double)20));
        subscribers.add(new Range((double)5, (double)7));
        subscribers.add(new Range((double)6, (double)8));
        subscribers.add(new Range((double)7, (double)9));
        subscribers.add(new Range((double)8, (double)10));
        subscribers.add(new Range((double)9, (double)11));
        subscribers.add(new Range((double)10, (double)26));
        subscribers.add(new Range((double)11, (double)27));
        subscribers.add(new Range((double)12, (double)28));
        subscribers.add(new Range((double)13, (double)29));
        subscribers.add(new Range((double)14, (double)30));
        subscribers.add(new Range((double)15, (double)31));
        subscribers.add(new Range((double)16, (double)31));
        subscribers.add(new Range((double)17, (double)31));
        subscribers.add(new Range((double)18, (double)31));
        subscribers.add(new Range((double)19, (double)31));
        subscribers.add(new Range((double)20, (double)31));
        publishers = new ArrayList<Integer>();
        publishers.add(0);
        events = new ArrayList<Double>();
        events.add(20.0);
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
    
    static public int eval() {
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
                System.out.println("sub=[" + node.getAttr(OverlayManager.KEY) + "," + node.getAttr(OverlayManager.RANGE_END) + "]");
            }
        };
        double sumFairness = 0;
        int countFairness = 0;
        for (int i = 0; i < publishers.size(); i++) {

            double fsum = 0;
            double fssum = 0;
            double fcount = 0;

            ovs.get(publishers.get(i)).overlapSend(events.get(i), ex);
            
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            for (OverlayManager ov : ovs) {
                SimTransport st = (SimTransport)ov.getTransport();
                int load = st.in + st.out; // XXX Is this OK?
                //int load = st.out; // XXX Is this OK?
                //System.out.println("load=" + load);
                if (load > 0) {
                    fsum += load;
                    fssum += (load * load);
                    fcount += 1;
                }
            }            
            sumFairness += ((fsum * fsum) / (fcount * fssum));
            System.out.println("count/fcount=" + count + "/" + fcount);
            countFairness++;
        }
        System.out.println("ave. fairness index=" + (sumFairness / countFairness));
        System.out.println("ave. hops=" + (sumHops / count));
        System.out.println("publish message count=" + SimTransportOracle.messageCount());
        
        seedTrans.fin();
        return (int)count;
    }
    
    static public void main(String[] args) {
//        for (int i = 0; i < 10; i++) {
        // prepareRandomDataSet();
        prepareSampleDataSet2();
        //OverlayManager.setOverlay("org.piax.ov.ovs.itsg.ITSkipGraphZen");
        //System.out.println("-- ITSG");
        
        OverlayManager.setOverlay("org.piax.ov.ovs.isg.ISkipGraph");
        System.out.println("-- ISG");
        while (eval() == 2);

//        OverlayManager.setOverlay("org.piax.ov.ovs.rksg.RKSkipGraph");
//        System.out.println("-- RKSG");
//        eval();
//        OverlayManager.setOverlay("org.piax.ov.ovs.risg.RISkipGraph");
//        System.out.println("-- RISG");
//        eval();
//        }
    }
}
