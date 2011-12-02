package org.piax.ov.ovs.itsg;

import java.util.ArrayList;
import java.util.List;

import org.piax.ov.OverlayManager;
import org.piax.trans.Node;
import org.piax.trans.common.Id;
import org.piax.ov.ovs.skipgraph.SkipGraph;

public class MaxTable {
    
    public class LevelEntry {
        public Comparable<?> leftMax;
        public Comparable<?> leftNeighborMax;
        public Node leftMaxNode;
    }
    
    public ArrayList<LevelEntry> maxTable;
    
    public MaxTable () {
        maxTable = new ArrayList<LevelEntry>();
    }
    
    public void put(int kind, int level, Object value) {
        int maxTableSize = maxTable.size();
        for (int i = level + 1; i > maxTableSize; i--) {
            LevelEntry kc = new LevelEntry();
            kc.leftMax = kc.leftNeighborMax = null;
            kc.leftMaxNode = null;
            maxTable.add(kc);
        }
        LevelEntry kc = maxTable.get(level);
        if (kind == ITSkipGraph.LEFT_MAX) {
            kc.leftMax = (Comparable<?>) value;
        }
        else if (kind == ITSkipGraph.LEFT_NEIGHBOR_MAX) {
            kc.leftNeighborMax = (Comparable<?>) value;
        }
        else {
            kc.leftMaxNode = (Node) value;
        }
       
    }

    public Object get(int kind, int level) {
        if (maxTable.size() < level + 1) {
            return null;
        }
        LevelEntry kc = maxTable.get(level);
        if (kind == ITSkipGraph.LEFT_MAX) {
            return kc.leftMax;
        }
        else if (kind == ITSkipGraph.LEFT_NEIGHBOR_MAX) {
            return kc.leftNeighborMax;
        }
        else {
            return kc.leftMaxNode;
        }
        
    }
    
    public String toString() {
        String ret = "";
        for (int i = maxTable.size() - 1; i >= 0; i--) {
            LevelEntry kc = maxTable.get(i);
            //ret += kc.left + "<-" + i + "->" + kc.right + "\n";
            ret += "(" + i + "):lmax=" + kc.leftMax + ",lnmax=" + kc.leftNeighborMax + ",lmnode=" + kc.leftMaxNode + "\n";
        }
        return ret;
    }
}
