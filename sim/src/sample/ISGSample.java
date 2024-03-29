package sample;

import org.piax.ov.OverlayManager;
import org.piax.ov.common.Range;
import org.piax.trans.Node;
import org.piax.trans.sim.SimTransport;
import org.piax.trans.sim.SimTransportOracle;

public class ISGSample {
    static public void main(String args[]) {
        OverlayManager.setOverlay("org.piax.ov.ovs.rksg.RKSkipGraph");
        //OverlayManager.setOverlay("org.piax.ov.ovs.isg.ISkipGraph");
        //OverlayManager.setOverlay("org.piax.ov.ovs.risg.RISkipGraph");

        SimTransport seedTrans = new SimTransport();
        OverlayManager seedOv = new OverlayManager(seedTrans);
        seedOv.putRange(new Range((double)2, (double)5));

        OverlayManager start = null;
        double searchKey = 6;
        double startKey = 7;
        //int[] nums = {7, 2, 6, 5, 4, 3, 1};
        int[] nums = {1, 2, 3, 4, 5, 6, 7};
        
        for (int i : nums) {
            SimTransport trans = new SimTransport();
            OverlayManager ov = new OverlayManager(trans);
            Node seed = trans.getRemoteNode(seedTrans);
            ov.setSeed(seed);
            if (i == 1) {
                ov.putRange(new Range((double) 6, (double) 14));
                System.out.println("insert message count1=" + SimTransportOracle.messageCount());
            }
            if (i == 2) {
                ov.putRange(new Range((double) 9, (double) 12));
                System.out.println("insert message count2=" + SimTransportOracle.messageCount());
            }
            if (i == 3) {
                ov.putRange(new Range((double) 14, (double) 16));
                System.out.println("insert message count3=" + SimTransportOracle.messageCount());
            }
            if (i == 4) {
                ov.putRange(new Range((double) 15, (double) 23));
                System.out.println("insert message count4=" + SimTransportOracle.messageCount());
            }
            if (i == 5) {
                ov.putRange(new Range((double) 18, (double) 19));
                System.out.println("insert message count5=" + SimTransportOracle.messageCount());
            }
            if (i == 6) {
                ov.putRange(new Range((double) 20, (double) 27));
                System.out.println("insert message count6=" + SimTransportOracle.messageCount());
            }
            if (i == 7) {
                ov.putRange(new Range((double) 21, (double) 30));
                System.out.println("insert message count7=" + SimTransportOracle.messageCount());
            }
            if (i == startKey) {
                start = ov;
            }
        }
        System.out.println("--- start dump ---");
        SimTransport.getOracle().dump();
        System.out.println("--- end dump ---");
        
        System.out.println("--- search " + searchKey + " from " + startKey + "---");
        SimTransportOracle.clearMessageCount();
        System.out.println("search result=" + start.overlapSearch((double)20));
        System.out.println("search message count=" + SimTransportOracle.messageCount());
        System.out.println("--- delete " + startKey + "---");
        start.delete();

        seedTrans.fin();
        System.out.println("Erapsed: " + ((SimTransport)seedTrans).getElapsedTime() + "ms");
    }
}
