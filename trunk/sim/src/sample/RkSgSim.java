package sample;

import java.util.ArrayList;
import java.util.Collections;

import org.piax.ov.OverlayManager;
import org.piax.ov.common.Range;
import org.piax.trans.Node;
import org.piax.trans.sim.SimTransport;

public class RkSgSim {
    static public void main(String args[]) {
        OverlayManager.setOverlay("org.piax.ov.ovs.rksg.RKSkipGraph");

        SimTransport seedTrans = new SimTransport();
        OverlayManager seedOv = new OverlayManager(seedTrans);
        seedOv.putRange(new Range((double)0, (double)25.0));

        OverlayManager start = null;
        double searchKey = 900;
        double startKey = 890.0;
        int NUM = 1000;
        ArrayList<Double> keys = new ArrayList<Double>();
        
        for (int i = 1; i < NUM; i++) {
            keys.add((double)i);
        }
        Collections.shuffle(keys);
        
        for (double i : keys) {
            SimTransport trans = new SimTransport();
            OverlayManager ov = new OverlayManager(trans);
            Node seed = trans.getRemoteNode(seedTrans);
            ov.setSeed(seed);
            ov.putRange(new Range(i, i + Math.random() * 20));
            if (i == startKey) {
                start = ov;
            }
        }
//        System.out.println("--- start dump ---");
        //SimTransport.getOracle().dump();
//        System.out.println("--- end dump ---");
//        System.out.println("--- search " + searchKey + " from " + startKey + "---");
        System.out.println("search result=" + start.overlapSearch((double)searchKey));
//        System.out.println("--- delete " + startKey + "---");
//        start.delete();

        seedTrans.fin();
        System.out.println("Erapsed: " + ((SimTransport)seedTrans).getElapsedTime() + "ms");
    }
}
