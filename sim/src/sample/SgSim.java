package sample;

import org.piax.ov.Node;
import org.piax.trans.SimTransport;

public class SgSim {
    static public void main(String args[]) {
        SimTransport trans = new SimTransport();
        Node seed = new Node(trans);
        seed.addKey(0);
        Node start = null;
        int searchKey = 1;
        int startKey = 800;
        
        for (int i = 1000; i > 0; i--) {
            Node n = new Node(trans, seed.id);
            n.addKey(i);
            if (i == startKey) {
                start = n;
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
    }
}
