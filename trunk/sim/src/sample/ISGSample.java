package sample;

import org.piax.ov.OverlayManager;
import org.piax.ov.common.Range;
import org.piax.trans.Node;
import org.piax.trans.sim.SimTransport;

public class ISGSample {
    static public void main(String args[]) {
        OverlayManager.setOverlay("org.piax.ov.ovs.rksg.RKSkipGraph");

        SimTransport seedTrans = new SimTransport();
        OverlayManager seedOv = new OverlayManager(seedTrans);
        seedOv.putRange(new Range((double)2, (double)5));

        OverlayManager start = null;
        double searchKey = 6;
        double startKey = 7;
        int[] nums = {7, 2, 6, 5, 4, 3, 1};
        
        for (int i : nums) {
            SimTransport trans = new SimTransport();
            OverlayManager ov = new OverlayManager(trans);
            Node seed = trans.getRemoteNode(seedTrans);
            ov.setSeed(seed);
            if (i == 1) {
                ov.putRange(new Range((double) 6, (double) 14));
            }
            if (i == 2) {
                ov.putRange(new Range((double) 9, (double) 12));
            }
            if (i == 3) {
                ov.putRange(new Range((double) 14, (double) 16));
            }
            if (i == 4) {
                ov.putRange(new Range((double) 15, (double) 23));
            }
            if (i == 5) {
                ov.putRange(new Range((double) 18, (double) 19));
            }
            if (i == 6) {
                ov.putRange(new Range((double) 20, (double) 27));
            }
            if (i == 7) {
                ov.putRange(new Range((double) 21, (double) 30));
            }
            if (i == startKey) {
                start = ov;
            }
        }
        System.out.println("--- start dump ---");
        SimTransport.getOracle().dump();
        System.out.println("--- end dump ---");
        
        System.out.println("--- search " + searchKey + " from " + startKey + "---");
        System.out.println("search result=" + start.overlapSearch((double)13));
        System.out.println("--- delete " + startKey + "---");
        start.delete();

        seedTrans.fin();
        System.out.println("Erapsed: " + ((SimTransport)seedTrans).getElapsedTime() + "ms");
    }
}
