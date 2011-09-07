package sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.piax.ov.OverlayManager;
import org.piax.ov.common.Range;
import org.piax.trans.Node;
import org.piax.trans.sim.SimTransport;
import org.piax.trans.sim.SimTransportOracle;

public class EvalPubSub {
    static ArrayList<Range> subscribers;
    static ArrayList<Integer> publishers;
    static ArrayList<Double> events;
    // prepare dataset;
    static public void prepareRandomDataSet() {
        int numberOfNodes = 100;
        int numberOfEvents = 10;
        int maxValue = 100;
        int maxRangeWidth = 10;
        // subscribers;
        subscribers = new ArrayList<Range>(numberOfNodes);
        ArrayList<Double> widths = new ArrayList<Double>(numberOfNodes);
        for (int i = 0; i < numberOfNodes; i++) {
            double width = ((double)i * (double)maxRangeWidth) / (double)numberOfNodes;
            widths.add(width);
        }
        Collections.shuffle(widths);
        for (int i = 0; i < numberOfNodes; i++) {
            double min = Math.random() * maxValue;
            subscribers.add(new Range(min, min + widths.get(i)));
        }
    
        // publishers, events
        publishers = new ArrayList<Integer>(numberOfEvents);
        events = new ArrayList<Double>(numberOfEvents);
        for (int i = 0; i < numberOfEvents; i++) {
            publishers.add((int)(Math.random() * numberOfNodes));
        }
        Collections.shuffle(publishers);
        for (int i = 0; i < numberOfEvents; i++) {
            events.add(Math.random() * maxValue);
        }
    
        for (Range range : subscribers) {
            System.out.println("subscribe:" + range);
        }
        for (int i = 0; i < numberOfEvents; i++) {
            System.out.println("publisher:" + publishers.get(i) + "->event:" + events.get(i));
        }
    }

    static public void eval() {
        SimTransport seedTrans = new SimTransport();
        OverlayManager seedOv = new OverlayManager(seedTrans);
        seedOv.putRange(new Range((double)0, (double)0));
        List<OverlayManager> ovs = new ArrayList<OverlayManager>();

        System.out.println(OverlayManager.getOverlay());        
        for (Range range : subscribers) {
            SimTransport trans = new SimTransport();
            OverlayManager ov = new OverlayManager(trans);
            Node seed = trans.getRemoteNode(seedTrans);
            ov.setSeed(seed);
            ov.putRange(range);
            ovs.add(ov);
        }
        System.out.println("insert message count=" + SimTransportOracle.messageCount());
        SimTransportOracle.clearMessageCount();
//        SimTransport.getOracle().dump();
        for (int i = 0; i < publishers.size(); i++) {
            ovs.get(publishers.get(i)).overlapSearch(events.get(i));
        }
        System.out.println("search message count=" + SimTransportOracle.messageCount());
        seedTrans.fin();
    }
    
    static public void main(String[] args) {
        prepareRandomDataSet();
        OverlayManager.setOverlay("org.piax.ov.ovs.rksg.RKSkipGraph");
        eval();
        OverlayManager.setOverlay("org.piax.ov.ovs.risg.RISkipGraph");
        eval();
        OverlayManager.setOverlay("org.piax.ov.ovs.isg.ISkipGraph");
        eval();
    }
}
