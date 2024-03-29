package org.piax.ov.ovs.rksg;

import static org.piax.trans.Literals.map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.grlea.log.SimpleLogger;
import org.piax.ov.OverlayManager;
import org.piax.ov.common.Range;
import org.piax.ov.ovs.rrsg.RRSkipGraph;
import org.piax.ov.ovs.skipgraph.SkipGraph;
import org.piax.ov.ovs.skipgraph.SkipGraph.Arg;
import org.piax.ov.ovs.skipgraph.SkipGraph.CheckOp;
import org.piax.ov.ovs.skipgraph.SkipGraph.Op;

import org.piax.trans.Node;
import org.piax.trans.common.Id;
import org.piax.trans.sim.SimTransportOracle;

public class RKSkipGraph extends RRSkipGraph {
    SimpleLogger log = new SimpleLogger(RKSkipGraph.class);
    public Comparable<?> rangeEnd;
    public List<Node> containings;
    
    static public enum Arg {
        CONTAINED, CONTAININGS, RANGE,
    }
    static public enum Op {
        GET_CONTAININGS, FOUND_CONTAININGS, NOT_FOUND_CONTAININGS, GET_LEFT, RET_LEFT
    }
    
    public RKSkipGraph(Node self) {
        super(self);
        this.rangeEnd = (Comparable<?>)self.getAttr(OverlayManager.RANGE_END);
        this.containings = new ArrayList<Node>();
    }
    
    public Comparable<?> getRangeEnd() {
        return rangeEnd;
    }
    
    public List<Node> getContainingList() {
        return containings;
    }
    
    public void setContainingList(List<Node> containings) {
        this.containings = containings;
    }
    
    public Comparable<?> getRangeEnd(Node n) {
        if (n != null) {
            return (Comparable<?>)n.getAttr(OverlayManager.RANGE_END);
        }
        return null;
    }
    
    private Map<Object,Object> getContainingsOp(Node v, Range range) {
        return map((Object)SkipGraph.Arg.OP, (Object)Op.GET_CONTAININGS).map(SkipGraph.Arg.NODE, v).map(Arg.RANGE, range);
    }
    
    private Map<Object,Object> updateContainingsOp(Node startNode, Range range, int level, Node containing) {
        return map((Object)SkipGraph.Arg.OP, (Object)RRSkipGraph.Op.RANGE_SEARCH).map(SkipGraph.Arg.NODE, startNode).map(RRSkipGraph.Arg.RANGE, range).map(SkipGraph.Arg.LEVEL, level).map(Arg.CONTAINED, containing);
    }
    
    private Map<Object,Object> foundContainingsOp(List<Node> containings) {
        return map((Object)SkipGraph.Arg.OP, (Object)Op.FOUND_CONTAININGS).map(Arg.CONTAININGS, containings);
    }
    
    private Map<Object,Object> notFoundContainingsOp() {
        return map((Object)SkipGraph.Arg.OP, (Object)Op.NOT_FOUND_CONTAININGS);
    }
    
    private Map<Object,Object> findOverlapOp(Node startNode, Range range, int level, Range searchRange) {
        return map((Object)SkipGraph.Arg.OP, (Object)RRSkipGraph.Op.RANGE_SEARCH).map(SkipGraph.Arg.NODE, startNode).map(RRSkipGraph.Arg.RANGE, range).map(SkipGraph.Arg.LEVEL, level).map(Arg.RANGE, searchRange);
    }
    
    private Map<Object,Object> getLeftOp() {
        return map((Object)SkipGraph.Arg.OP, (Object)Op.GET_LEFT);
    }
    
    private Map<Object,Object> retLeftOp() {
        return map((Object)SkipGraph.Arg.OP, (Object)Op.RET_LEFT).map(SkipGraph.Arg.NODE, neighbors.get(L, 0));
    }
    
    public void onReceive(Node sender, Map<Object,Object> mes) {
        Object op = mes.get(SkipGraph.Arg.OP);
        if (op == Op.GET_CONTAININGS) {
            onReceiveGetContainingsOp(sender, mes);
        }
        else if (op == Op.GET_LEFT) {
            onReceiveGetLeftOp(sender, mes);
        }
        else if (op == SkipGraph.Op.FOUND) {
            onReceiveSearchResult(sender, mes);
        }
        else if (op == RRSkipGraph.Op.FOUND_IN_RANGE || op == RRSkipGraph.Op.NOT_FOUND_IN_RANGE || op == Op.FOUND_CONTAININGS)  {
            onReceiveRangeSearchResult(sender, mes);
        }
        else {
            super.onReceive(sender, mes);
        }
    }
    
    protected void onReceiveGetLeftOp(Node sender, Map<Object, Object> args) {
        try {
            sender.send(retLeftOp());
        } catch (IOException e) {
            e.printStackTrace();
        }     
    }
    
    protected void onReceiveGetContainingsOp(Node sender, Map<Object, Object> args) {
        try {
            Range range = (Range) args.get(Arg.RANGE);
            List<Id> via = getVia(args);
            List<Node> ret = new ArrayList<Node>();
            for (Node node : containings) {
                if (rangeOverlaps(range, new Range(getKey(node), getRangeEnd(node)))) {
                    ret.add(node);
                }
            }
            if (rangeOverlaps(range, new Range(getKey(), getRangeEnd()))) {
                ret.add(self);
            }
            sender.send(setVia(foundContainingsOp(ret), via));
        } catch (IOException e) {
            e.printStackTrace();
        } 
    }
    
    private boolean rangeOverlaps(Range range1, Range range2) {
        return ((compare(range2.min, range1.min) <= 0 && compare(range1.min, range2.max) <= 0) ||
                (compare(range2.min, range1.max) <= 0 && compare(range1.max, range2.max) <= 0) ||
                (compare(range1.min, range2.min) <= 0 && compare(range2.min, range1.max) <= 0) ||
                (compare(range1.min, range2.max) <= 0 && compare(range2.max, range1.max) <= 0));
    }
    
    protected void onRangeMatch(Node sender, Map<Object,Object> args) {
        Node startNode = (Node) args.get(SkipGraph.Arg.NODE);
        List<Id> via = getVia(args);
        Node contained = (Node) args.get(Arg.CONTAINED);
        Range searchRange = (Range) args.get(Arg.RANGE);
        Object body = args.get(SkipGraph.Arg.BODY);
        try {
            if (contained != null) {
                //System.out.println("set max of " + key + ":" + getMax() + "->" + max);
                containings.add(contained);
            }
            if (searchRange != null) {
                if (rangeOverlaps(searchRange, new Range(key, rangeEnd))) {
                    if (contained != null) {
                        startNode.send(setVia(foundContainingsOp(containings), via));
                    }
                    else {
                        if (body != null) {
                            inspectObject(body, self, args);
                            // log.info("send hops =" + (via == null ? 0 : via.size()));
                            // System.out.println("key=" + key + " received -> " + body);
                        }
                        else {
                            startNode.send(setVia(foundInRangeOp(self), via));
                        }
                    }
                }
                else {
                    if (contained != null) {
                        startNode.send(setVia(notFoundContainingsOp(), via));
                    }
                    else {
                        if (body == null) {
                            startNode.send(setVia(notFoundInRangeOp(self), via));
                        }
                    }
                }
                return;
            }
            
            if (body != null) {
                inspectObject(body, self, args);
                //log.info("send hops =" + (via == null ? 0 : via.size()));
                //System.out.println("key=" + key + " received -> " + body);
            }
            else {
                startNode.send(setVia(foundInRangeOp(self), via));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    protected class RKSearchResult extends SearchResult {
        public List<Node> containings;
        public List<List<Id>> cVias;
        public RKSearchResult() {
            super();
            containings = new ArrayList<Node>();
            cVias = new ArrayList<List<Id>>();
        }
        public void addContainings(List<Node> nodes) {
            for (Node node : nodes) {
                if (!containings.contains(node)) {
                    containings.add(node);
                }
            }
        }
        public void addContainingVia(List<Id> via) {
            cVias.add(via);
        }
    }
    RKSearchResult containingsResult;
    
    protected void onReceiveRangeSearchResult(Node sender, Map<Object,Object> arg) {
        Object op = arg.get(SkipGraph.Arg.OP);
        List<Id> via = getVia(arg);
        Node node = (Node) arg.get(SkipGraph.Arg.NODE);
        List<Node> containings = (List<Node>) arg.get(Arg.CONTAININGS);
        if (op == RRSkipGraph.Op.FOUND_IN_RANGE){
            rangeSearchResult.addMatch(node);
            rangeSearchResult.addMatchVia(via);
        }
        if (op == Op.FOUND_CONTAININGS) {
            rangeSearchResult.addContainings(containings);
        }
        else {
            rangeSearchResult.addUnmatch(node);
            rangeSearchResult.addUnmatchVia(via);
        }
    }
    
    SkipGraph.ResultPool searchResult;
    RKSearchResult rangeSearchResult;
    
    @Override
    public List<Node> overlapSearch(Comparable<?> key) {
        return overlapSearch(new Range(key, key));
    }
    
    @Override
    public List<Node> overlapSearch(Range range) {
        self.trans.setParameter(SimTransportOracle.Param.NestedWait, Boolean.FALSE); // for better performance
        rangeSearchResult = new RKSearchResult();
        onReceiveRangeSearchOp(self, rangeSearchOp(self, range, getMaxLevel()));
        synchronized(rangeSearchResult) {
            try {
                rangeSearchResult.wait(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Map<Object,Object> ret = findLeftMax(self, range.min);
        Node leftMax = (Node) ret.get(SkipGraph.Arg.NODE);
        List<Id> via = getVia(ret);
        if (leftMax != null) {
            Map<Object, Object> mes;
            try {
                mes = leftMax.sendAndWait(setVia(getContainingsOp(self, range), via), new CheckOp(Op.FOUND_CONTAININGS));
                if (mes != null) {
                    // XXX not reached.
                    via = getVia(mes);
                    for (Node containing : (List<Node>)mes.get(Arg.CONTAININGS)) {
                        if (rangeOverlaps(new Range(getKey(containing), getRangeEnd(containing)), range)) {
                            if (!rangeSearchResult.matches.contains(containing)) {
                                containing.send(setVia(rangeSearchOp(self, new Range(getKey(containing), getKey(containing)), getMaxLevel()), via));
                                //rangeSearchResult.matches.add(containing);
                                //rangeSearchResult.mVias.add(getVia(mes));
                                synchronized(rangeSearchResult) {
                                    try {
                                        rangeSearchResult.wait(100);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }
                int hopSum = 0;
                for (List<Id> v : rangeSearchResult.mVias) {
                    hopSum += v.size();
                }
                if (rangeSearchResult.mVias.size() != 0) { 
                    System.out.println("Matches= " + rangeSearchResult.mVias.size() + ", Ave. hops=" + (hopSum / (double)rangeSearchResult.mVias.size()));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Collections.sort(rangeSearchResult.matches, new KeySortComparator());
        return rangeSearchResult.matches;
    }
    
    @Override
    public void overlapSend(Comparable<?> key, Object body) {
        overlapSend(new Range(key, key), body);
    }
    
    @Override
    public void overlapSend(Range range, Object body) {
        self.trans.setParameter(SimTransportOracle.Param.NestedWait, Boolean.FALSE); // for better performance
        onReceiveRangeSearchOp(self, rangeSearchOp(self, range, getMaxLevel()));
        synchronized(rangeSearchResult) {
            try {
                rangeSearchResult.wait(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Map<Object,Object> ret = findLeftMax(self, range.min);
        Node leftMax = (Node) ret.get(SkipGraph.Arg.NODE);
        List<Id> via = getVia(ret);
        if (leftMax != null) {
            Map<Object, Object> mes;
            try {
                mes = leftMax.sendAndWait(setVia(getContainingsOp(self, range), via), new CheckOp(Op.FOUND_CONTAININGS));
                if (mes != null) {
                    via = getVia(mes);
                    for (Node containing : (List<Node>)mes.get(Arg.CONTAININGS)) {
                        if (rangeOverlaps(new Range(getKey(containing), getRangeEnd(containing)), range)) {
                            containing.send(setVia(rangeSendOp(new Range(getKey(containing), getKey(containing)), getMaxLevel(), body), via));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private Map<Object,Object> findLeftMax(Node introducer, Comparable<?> key) {
        try {
            Map<Object,Object> mr = introducer.sendAndWait(getMaxLevelOp(), new CheckOp(SkipGraph.Op.RET_MAX_LEVEL));
            int maxLevel = (Integer)mr.get(SkipGraph.Arg.LEVEL);
            Map<Object, Object> sr = introducer.sendAndWait(searchOp(self, key, maxLevel - 1), new CheckOp(SkipGraph.Op.NOT_FOUND, SkipGraph.Op.FOUND));
            Node neighbor = (Node)sr.get(SkipGraph.Arg.NODE);
            //System.out.println("NEIGHBOR=" + neighbor);
            if (neighbor != null && compare(key, getKey(neighbor)) < 0) {
                Map<Object, Object> mes = neighbor.sendAndWait(getLeftOp(), new CheckOp(Op.RET_LEFT));
                Node leftMax = (Node) mes.get(SkipGraph.Arg.NODE);
                if (leftMax != null) { // If null, left most node.
                    return mes;//(Node)mes.get(SkipGraph.Arg.NODE);
                }
            }
            return sr;//neighbor;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    @Override
    public Node search(Comparable<?> key) {
        // not implemented in this class.
        return null;
    }

    @Override
    public List<Node> search(Range range) {
        self.trans.setParameter(SimTransportOracle.Param.NestedWait, Boolean.FALSE); // for better performance
        rangeSearchResult = new RKSearchResult();
        onReceiveRangeSearchOp(self, rangeSearchOp(self, range, getMaxLevel()));
        synchronized(rangeSearchResult) {
            try {
                rangeSearchResult.wait(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Collections.sort(rangeSearchResult.matches, new KeySortComparator());
        return rangeSearchResult.matches;
    }
    
    @Override
    public boolean insert(Node introducer) {
        self.trans.setParameter(SimTransportOracle.Param.NestedWait, Boolean.FALSE); // for better performance
        // seed.
        //super.insert(introducer);
        rangeSearchResult = new RKSearchResult();
        Range range = new Range(getKey(), getRangeEnd());
        try {
            if (!introducer.equals(self)) {
                Map<Object,Object> ret = findLeftMax(introducer, getKey());
                Node leftMax = (Node) ret.get(SkipGraph.Arg.NODE);
                //System.out.println("KEY=" + getKey() + ", LEFT MAX=" + leftMax);
                if (leftMax != null) {
                    Map<Object, Object> mes = leftMax.sendAndWait(getContainingsOp(self, range), new CheckOp(Op.FOUND_CONTAININGS));
                    rangeSearchResult.addContainings((List<Node>)mes.get(Arg.CONTAININGS));
                    rangeSearchResult.mVias.add(getVia(mes));
                }
            }
            if (!super.insert(introducer)) {
                return false;
            }
            if (!introducer.equals(self)) {
                self.send(updateContainingsOp(self, range, getMaxLevel(), self));
                synchronized(rangeSearchResult) {
                    try {
                        rangeSearchResult.wait(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return false;
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        containings = rangeSearchResult.containings;
        //System.out.println(range + ", CONTAININGS=" + containings);
        return true;
    }
    
    @Override
    public void delete() {
        super.delete();
        // XXX not implemented yet.
    }
    
}
