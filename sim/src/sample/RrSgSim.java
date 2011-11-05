package sample;

import org.piax.ov.OverlayManager;
import org.piax.ov.common.Range;
import org.piax.trans.Node;
import org.piax.trans.Transport;
import org.piax.trans.sim.SimTransport;

public class RrSgSim {
    static public void main(String args[]) {
        //    OverlayManager.setOverlay("org.piax.ov.ovs.rrsg.RRSkipGraph");
        OverlayManager.setOverlay("org.piax.ov.ovs.srrsg.SRRSkipGraph");

        SimTransport seedTrans = new SimTransport();
        OverlayManager seedOv = new OverlayManager(seedTrans);
        seedOv.putKey(0);

        OverlayManager start = null;
        double searchKey = 900;
        double startKey = 105;
        
        for (int i = 1000; i > 0; i--) {
            SimTransport trans = new SimTransport();
            OverlayManager ov = new OverlayManager(trans);
            // Following is the way to get a Node instance on SimTransport.
            Node seed = trans.getRemoteNode(seedTrans);
            ov.setSeed(seed);
            ov.putKey((double)i);
            if (i == startKey) {
                start = ov;
            }
        }
        //System.out.println("--- dump ---");
        //trans.dump();
        System.out.println("--- search " + searchKey + " from " + startKey + "---");
        Range r = new Range((double)searchKey, (double)searchKey + 100);
        System.out.println("search result=" + start.search(r));
        System.out.println("--- send " + searchKey + " from " + startKey + "---");
        start.send(r, "Hello, " + r);
        //System.out.println("search result=" + start.search(new Range((double)10, (double)11.6)));
        //System.out.println("--- delete " + startKey + "---");
        //start.delete();

        //trans.dump();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        seedTrans.fin();
        System.out.println("Erapsed: " + ((SimTransport)seedTrans).getElapsedTime() + "ms");
    }
}
