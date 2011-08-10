package sample;

import java.util.ArrayList;
import java.util.Collections;

import org.piax.ov.OverlayManager;
import org.piax.ov.common.Range;
import org.piax.trans.Node;
import org.piax.trans.sim.SimTransport;
import org.piax.trans.sim.SimTransportOracle;

public class EvalRkSg {
    
    
    static public void eval(int num, double searchKey, double startKey, double yure, boolean skewed) {
        //OverlayManager.setOverlay("org.piax.ov.ovs.rksg.RKSkipGraph");
        OverlayManager.setOverlay("org.piax.ov.ovs.isg.ISkipGraph");

        System.out.println("searchKey=" + searchKey + ",startKey=" + startKey);
        SimTransport seedTrans = new SimTransport();
        OverlayManager seedOv = new OverlayManager(seedTrans);
        seedOv.putRange(new Range((double)0, (double)0));

        OverlayManager start = seedOv;
        ArrayList<Double> keys = new ArrayList<Double>();
        Zipf zipf = new Zipf(num, 1);
        
        for (int i = 1; i < num; i++) {
            keys.add((double)i);
        }
        Collections.shuffle(keys);
        
        for (double i : keys) {
            SimTransport trans = new SimTransport();
            OverlayManager ov = new OverlayManager(trans);
            Node seed = trans.getRemoteNode(seedTrans);
            ov.setSeed(seed);
            if (skewed) { 
                ov.putRange(new Range(i, i + yure * (1 + zipf.nextInt()) / 1000)); 
            }
            else {
                ov.putRange(new Range(i, i + Math.random() * yure));
            }
            
            if (i == startKey) {
                start = ov;
            }
        }
//        System.out.println("--- start dump ---");
        //SimTransport.getOracle().dump();
//        System.out.println("--- end dump ---");
//        System.out.println("--- search " + searchKey + " from " + startKey + "---");
        System.out.println("insert message count=" + SimTransportOracle.messageCount());
        SimTransportOracle.clearMessageCount();
        start.overlapSearch((double)searchKey);
//        System.out.println("--- delete " + startKey + "---");
//        start.delete();
        System.out.println("search message count=" + SimTransportOracle.messageCount());
        seedTrans.fin();
        System.out.println("Erapsed: " + ((SimTransport)seedTrans).getElapsedTime() + "ms");
    }
    
    static public void main(String[] args) {
        double searchKey = 900;
        double startKey = 100.0;

        if (false) {
            for (int i = 0; i <= 20; i++) {
                System.out.println("--\nrange=" + (i * 10));
                eval(1000, searchKey, startKey, i * 10, true);
                System.out.println("--");
                eval(1000, searchKey, startKey, i * 10, false);
            }
        }
        
        for (int i = 1; i <= 10; i++) {
            System.out.println("--\nNUM=" + (i * 10));
            eval(i * 10 , i * 10 * 9 / 10, 0, 10, true);
            System.out.println("--");
            eval(i * 10 , i * 10 * 9 / 10, 0, 10, false);
        }
        for (int i = 1; i <= 10; i++) {
            System.out.println("--\nNUM=" + (i * 100));
            eval(i * 100, i * 100 * 9 / 10, 0, 10, true);
            System.out.println("--");
            eval(i * 100, i * 100 * 9 / 10, 0, 10, false);
        }
        
    }


}
