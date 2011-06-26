package org.piax.ov.ovs.skipgraph;

import java.util.ArrayList;

import org.piax.trans.common.Id;

public class NeighborTable {
    ArrayList<KeyCell> skipTable;
    Comparable<?> key;
    public NeighborTable (Comparable<?> key) {
        this.key = key;
        skipTable = new ArrayList<KeyCell>();
    }
    public void put(int side, int level, Id id, Comparable<?> key) {
        for (int i = level + 1; i > skipTable.size(); i--) {
            KeyCell kc = new KeyCell();
            kc.left = kc.right = null;
            kc.leftKey = kc.rightKey = null;
            skipTable.add(kc);
        }
        KeyCell kc = skipTable.get(level);
        if (side == SkipGraph.R) {
            kc.right = id;
            kc.rightKey = key;
        }
        else {
            kc.left = id;
            kc.leftKey = key;
        }
    }

    public Id get(int side, int level) {
        if (skipTable.size() < level + 1) {
            return null;
        }
        if (side == SkipGraph.R) {
            return skipTable.get(level).right;
        }
        else {
            return skipTable.get(level).left;
        }
    }

    public Comparable<?> getKey(int side, int level) {
        if (skipTable.size() < level + 1) {
            return null;
        }
        if (side == SkipGraph.R) {
            return skipTable.get(level).rightKey;
        }
        else {
            return skipTable.get(level).leftKey;
        }
    }
    
    public String toString() {
        String ret = "";
        for (int i = skipTable.size() - 1; i >= 0; i--) {
            KeyCell kc = skipTable.get(i);
            //ret += kc.left + "<-" + i + "->" + kc.right + "\n";
            ret += kc.leftKey + "<-(" + i + ")-->" + kc.rightKey + "\n";
        }
        return ret;
    }
}