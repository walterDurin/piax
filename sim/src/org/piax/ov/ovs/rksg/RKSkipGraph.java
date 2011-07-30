package org.piax.ov.ovs.rksg;

import static org.piax.trans.Literals.map;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.piax.ov.OverlayManager;
import org.piax.ov.common.KeyComparator;
import org.piax.ov.common.Range;
import org.piax.ov.ovs.rrsg.RRSkipGraph;
import org.piax.ov.ovs.skipgraph.SkipGraph;
import org.piax.trans.Node;
import org.piax.trans.common.Id;
import org.piax.trans.sim.SimTransportOracle;

public class RKSkipGraph extends RRSkipGraph {
    public Comparable<?> rangeEnd;
    
    static public enum Arg {
        MAX
    }
    static public enum Op {
        FIND_MAX, FOUND_MAX, UPDATE_MAX
    }
    
    public RKSkipGraph(Node self) {
        super(self);
        this.rangeEnd = (Comparable<?>)self.getAttr(OverlayManager.RANGE_END);
    }
    
    public Comparable<?> getRangeEnd() {
        return rangeEnd;
    }
    
    public Comparable<?> getMax() {
        return (Comparable<?>)self.getAttr(OverlayManager.MAX);
    }
    
    public void setMax(Comparable<?> max) {
        self.putAttr(OverlayManager.MAX, max);
    }
    
    public Comparable<?> getRangeEnd(Node n) {
        if (n != null) {
            return (Comparable<?>)n.getAttr(OverlayManager.RANGE_END);
        }
        return null;
    }
    
    public Comparable<?> getMax(Node n) {
        Comparable<?> ret = null;
        if (n != null) {
            ret = (Comparable<?>)n.getAttr(OverlayManager.MAX);
        }
        if (ret == null) {
            return KeyComparator.BOTTOM_VALUE;
        }
        return ret;
    }
    
    public void setMaxRangeEnd(Node n, Comparable<?> rangeEnd) {
        n.putAttr(OverlayManager.MAX, rangeEnd);
    }
    
    private Map<Object,Object> findMaxOp(Node v, Comparable<?> max, int level) {
        return map((Object)SkipGraph.Arg.OP, (Object)Op.FIND_MAX).map(SkipGraph.Arg.NODE, v).map(SkipGraph.Arg.LEVEL, level).map(Arg.MAX, max);
    }
    
    private Map<Object,Object> updateMaxOp(Node startNode, Range range, int level, boolean found, Comparable<?> max) {
        return map((Object)SkipGraph.Arg.OP, (Object)RRSkipGraph.Op.RANGE_SEARCH).map(SkipGraph.Arg.NODE, startNode).map(RRSkipGraph.Arg.RANGE, range).map(SkipGraph.Arg.LEVEL, level).map(RRSkipGraph.Arg.FOUND, found).map(Arg.MAX, max);
    }
    
    private Map<Object,Object> foundMaxOp(Node v) {
        return map((Object)SkipGraph.Arg.OP, (Object)Op.FOUND_MAX).map(SkipGraph.Arg.NODE, v);
    }
    
    public void onReceive(Node sender, Map<Object,Object> mes) {
        Object op = mes.get(SkipGraph.Arg.OP);
        if (op == Op.FIND_MAX) {
            onReceiveFindMaxOp(sender, mes);
        }
        else {
            super.onReceive(sender, mes);
        }
    }
    
    public void onReceiveFindMaxOp(Node sender, Map<Object,Object> args) {
        Node startNode = (Node) args.get(SkipGraph.Arg.NODE);
        int level = (int)((Integer)args.get(SkipGraph.Arg.LEVEL));
        Comparable<?> max = (Comparable<?>)args.get(Arg.MAX);
        List<Id> via = getVia(args);
        try {
            if (compare(max, getMax(neighbors.get(R, 0))) < 0) {
                startNode.send(setVia(foundMaxOp(self), via));
            }
            else {
                while (level >= 0) {
                    Node rightNode = neighbors.get(R, level);
                    if (rightNode != null && compare(max, getMax(rightNode)) >= 0) {
                        neighbors.get(R, level).send(setVia(findMaxOp(startNode, max, level), via));
                        break;
                    }
                    else {
                        level--;
                    }
                }
            }
            if (level < 0) {
                startNode.send(setVia(foundMaxOp(self), via));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    protected void onRangeMatch(Node sender, Map<Object,Object> args) {
        Node startNode = (Node) args.get(SkipGraph.Arg.NODE);
        List<Id> via = getVia(args);
        Comparable<?> max = (Comparable<?>) args.get(Arg.MAX);
        try {
            if (max != null) {
                System.out.println("set max of " + key + ":" + getMax() + "->" + max);
                setMax(max);
            }
            startNode.send(setVia(foundInRangeOp(self), via));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    protected void onReceiveRangeSearchResult(Node sender, Map<Object,Object> arg) {
        RRSkipGraph.Op op = (RRSkipGraph.Op) arg.get(SkipGraph.Arg.OP);
        List<Id> via = getVia(arg);
        Node node = (Node) arg.get(SkipGraph.Arg.NODE);
        if (op == RRSkipGraph.Op.FOUND_IN_RANGE){
            searchResult.addNode(node);
            searchResult.addVia(via);
        }
        else {
            System.out.println("not found=" + node);
        }
    }
    
    SearchResult searchResult;
    public List<Node> updateMax(Range range, Comparable<?> max, List<Id> via) {
        searchResult = new SearchResult();
        onReceiveRangeSearchOp(self, setVia(updateMaxOp(self, range, getMaxLevel(), false, max), via));
        synchronized(searchResult) {
            try {
                searchResult.wait(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Collections.sort(searchResult.nodes, new KeySortComparator());
        return searchResult.nodes;
    }

    public void insert(Node introducer) {
        self.trans.setParameter(SimTransportOracle.Param.NestedWait, Boolean.FALSE); // for better performance
        super.insert(introducer);
        // seed.
        if (neighbors.get(L, 0) == null && neighbors.get(R, 0) == null) {
            return;
        }
        if (compare(rangeEnd, getMax(neighbors.get(L, 0))) > 0) {
            setMax(rangeEnd);
            // left side is less than self. update right side.
            try {
                Map<Object, Object> found = self.sendAndWait(findMaxOp(self, rangeEnd, getMaxLevel()), new CheckOp(Op.FOUND_MAX));
                Node x = (Node) found.get(SkipGraph.Arg.NODE);
                List<Id> via = getVia(found);
                if (self != x) {
                    System.out.println("range: (" + key +"," +  getKey(x) + ")");
                    List<Node> updated = updateMax(new Range(key, getKey(x)), getMax(), via);
                    System.out.println("updated:" + updated);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            // left side is greater than self. Just update self.
            setMax(getMax(neighbors.get(L, 0)));
        }
    }

}
