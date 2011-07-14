package sample;

import org.piax.ov.OverlayManager;
import org.piax.trans.Node;
import org.piax.trans.SimTransport;
import org.piax.trans.Transport;

public class SgSim {
    static public void main(String args[]) {
        Transport trans = new SimTransport();
        
        OverlayManager.setOverlay("org.piax.ov.ovs.skipgraph.SkipGraph");
        
        OverlayManager seedOv = new OverlayManager(trans);
        seedOv.putKey(0);

        OverlayManager start = null;
        int searchKey = 1;
        int startKey = 890;
        
        for (int i = 1000; i > 0; i--) {
            OverlayManager ov = new OverlayManager(trans);
            // Following is the way to get a Node instance on SimTransport.
            Node seed = ((SimTransport)trans).getNode(ov, seedOv);
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
        trans.fin();
        System.out.println("Erapsed: " + ((SimTransport)trans).getElapsedTime() + "ms");
    }
}
