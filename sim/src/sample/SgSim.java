package sample;

import org.piax.ov.OverlayManager;
import org.piax.trans.Node;
import org.piax.trans.Transport;
import org.piax.trans.sim.SimTransport;

public class SgSim {
    static public void main(String args[]) {
        OverlayManager.setOverlay("org.piax.ov.ovs.skipgraph.SkipGraph");

        SimTransport seedTrans = new SimTransport();
        OverlayManager seedOv = new OverlayManager(seedTrans);
        seedOv.putKey(0);

        OverlayManager start = null;
        int searchKey = 1;
        int startKey = 890;
        
        for (int i = 1000; i > 0; i--) {
            SimTransport trans = new SimTransport();
            OverlayManager ov = new OverlayManager(trans);
            // Following is the way to get a Node instance on SimTransport.
            Node seed = trans.getRemoteNode(seedTrans);
            ov.setSeed(seed);
            ov.putKey(i);
            if (i == startKey) {
                start = ov;
            }
        }
        //System.out.println("--- dump ---");
        //trans.dump();
        System.out.println("--- search " + searchKey + " from " + startKey + "---");
        System.out.println("search result=" + start.search(searchKey));
        System.out.println("--- delete " + startKey + "---");
        start.delete();
        //System.out.println("--- dump ---");
        //trans.dump();
        seedTrans.fin();
        System.out.println("Erapsed: " + ((SimTransport)seedTrans).getElapsedTime() + "ms");
    }
}
