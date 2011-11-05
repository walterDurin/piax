package sample;

import org.grlea.log.SimpleLogger;
import org.piax.ov.OverlayManager;
import org.piax.ov.ovs.skipgraph.MembershipVector;
import org.piax.trans.Node;
import org.piax.trans.sim.SimTransport;

public class SgSim {
    static public void main(String args[]) {
        OverlayManager.setOverlay("org.piax.ov.ovs.skipgraph.SkipGraph");
        MembershipVector.ALPHABET = 3;
        SimpleLogger log = new SimpleLogger(SgSim.class);

        for (int j = 0; j < 10; j++) {
        SimTransport seedTrans = new SimTransport();
        OverlayManager seedOv = new OverlayManager(seedTrans);
        seedOv.putKey(0);

        OverlayManager start = null;
        int searchKey = 10;
        int startKey = 990;
        int numberOfNodes = 1000;
        log.info("start");
        for (int i = numberOfNodes; i > 0; i--) {
            SimTransport trans = new SimTransport();
            OverlayManager ov = new OverlayManager(trans);
            // Following is the way to get a Node instance on SimTransport.
            Node seed = trans.getRemoteNode(seedTrans);
            ov.setSeed(seed);
            ov.putKey(i);
            if (i == startKey) {
                start = ov;
            }
            if (i % 100000 == 0) {
            System.out.println(i);
            System.gc();
            }
        }
        
        org.piax.ov.ovs.skipgraph.SkipGraph.BOUNCE = true;
        start.send(searchKey, "Hello " + searchKey);
        log.info("search result=" + start.search(searchKey));
        //ps.println("--- delete " + startKey + "---");
        //start.delete();
        //System.out.println("--- dump ---");
        //trans.dump();
        seedTrans.fin();
        log.info("Erapsed: " + ((SimTransport)seedTrans).getElapsedTime() + "ms");
        }
    }
}
