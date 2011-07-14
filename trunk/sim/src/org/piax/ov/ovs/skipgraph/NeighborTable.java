package org.piax.ov.ovs.skipgraph;

import java.util.ArrayList;

import org.piax.ov.OverlayManager;
import org.piax.trans.Node;
import org.piax.trans.common.Id;

public class NeighborTable {
    
    class LevelPair {
        public Node left;
        public Node right;
    }
    
    ArrayList<LevelPair> skipTable;
    Comparable<?> key;
    public NeighborTable (Comparable<?> key) {
        this.key = key;
        skipTable = new ArrayList<LevelPair>();
    }
    public void put(int side, int level, Node node) {
        for (int i = level + 1; i > skipTable.size(); i--) {
            LevelPair kc = new LevelPair();
            kc.left = kc.right = null;
            skipTable.add(kc);
        }
        LevelPair kc = skipTable.get(level);
        if (side == SkipGraph.R) {
            kc.right = node;
        }
        else {
            kc.left = node;
        }
    }

    public Node get(int side, int level) {
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
            if (skipTable.get(level).right == null) {
                return null;
            }
            return (Comparable<?>)skipTable.get(level).right.getAttr(OverlayManager.KEY);
        }
        else {
            if (skipTable.get(level).left == null) {
                return null;
            }
            return (Comparable<?>)skipTable.get(level).left.getAttr(OverlayManager.KEY);
        }
    }
    
    public String toString() {
        String ret = "";
        for (int i = skipTable.size() - 1; i >= 0; i--) {
            LevelPair kc = skipTable.get(i);
            //ret += kc.left + "<-" + i + "->" + kc.right + "\n";
            ret += kc.left + "<-(" + i + ")-->" + kc.right + "\n";
        }
        return ret;
    }
}