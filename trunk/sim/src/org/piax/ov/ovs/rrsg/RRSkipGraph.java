package org.piax.ov.ovs.rrsg;

import static org.piax.trans.Literals.map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.piax.ov.OverlayManager;
import org.piax.ov.common.KeyComparator;
import org.piax.ov.common.Range;
import org.piax.ov.ovs.skipgraph.SkipGraph;

import org.piax.trans.Node;
import org.piax.trans.common.Id;
import org.piax.trans.sim.SimTransportOracle;

// range retrieval extension of skipgraph
public class RRSkipGraph extends SkipGraph {
	static public enum Arg {
            RANGE, FOUND
        }
	static public enum Op {
	    RANGE_SEARCH, FOUND_IN_RANGE, NOT_FOUND_IN_RANGE
	}
	public RRSkipGraph(Node self) {
		super(self);
	}
	
	protected class SearchResult {
	    public List<Node> nodes;
	    public List<List<Id>> vias;
	    public SearchResult() {
	        nodes = new ArrayList<Node>();
	        vias = new ArrayList<List<Id>>();
	    }
	    public void addNode(Node node) {
	        nodes.add(node);
	    }
	    public void addVia(List<Id> via) {
	        vias.add(via);
	    }
	}
	SearchResult searchResult;
	
    protected Map<Object,Object> foundInRangeOp(Node v) {
        return map((Object)SkipGraph.Arg.OP, (Object)Op.FOUND_IN_RANGE).map(SkipGraph.Arg.NODE, v);
    }
    
    protected Map<Object,Object> notFoundInRangeOp(Node v) {
        return map((Object)SkipGraph.Arg.OP, (Object)Op.NOT_FOUND_IN_RANGE).map(SkipGraph.Arg.NODE, v);
    }
	
	protected Map<Object,Object> rangeSearchOp(Node startNode, Range searchRange, int level, boolean found) {
	    return map((Object)SkipGraph.Arg.OP, (Object)Op.RANGE_SEARCH).map(SkipGraph.Arg.NODE, startNode).map(Arg.RANGE, searchRange).map(SkipGraph.Arg.LEVEL, level).map(Arg.FOUND, found);
	}
	
	protected Map<Object,Object> rangeSearchOp(Map<Object,Object> mes, Range searchRange, int level, boolean found) {
	    Map<Object,Object> newMes = new HashMap<Object,Object>();
	    newMes.putAll(mes);
	    newMes.put(Arg.RANGE, searchRange);
	    newMes.put(SkipGraph.Arg.LEVEL, level);
	    newMes.put(Arg.FOUND, found);
	    return newMes;
	}
	
	protected void onReceiveRangeSearchResult(Node sender, Map<Object,Object> arg) {
        Op op = (Op) arg.get(SkipGraph.Arg.OP);
        List<Id> via = getVia(arg);
        Node node = (Node) arg.get(SkipGraph.Arg.NODE);
        if (op == Op.FOUND_IN_RANGE){
            searchResult.addNode(node);
            searchResult.addVia(via);
        }
        else {
            System.out.println("not found=" + node);
        }
    }
	
	public class KeySortComparator implements Comparator {  
        public int compare(Object arg0, Object arg1) {  
            return KeyComparator.getInstance().compare((Comparable<?>)((Node)arg0).getAttr(OverlayManager.KEY), (Comparable<?>)((Node)arg1).getAttr(OverlayManager.KEY));
        }  
    }
	
	public List<Node> search(Range range) {
		self.trans.setParameter(SimTransportOracle.Param.NestedWait, Boolean.FALSE); // for better performance
        searchResult = new SearchResult();
        onReceiveRangeSearchOp(self, rangeSearchOp(self, range, getMaxLevel(), false));
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
	
	protected int compare(Comparable<?> a, Comparable<?>b) {
        return KeyComparator.getInstance().compare(a, b);
    }
	
	public void onReceive(Node sender, Map<Object,Object> mes) {
	    Object op = mes.get(SkipGraph.Arg.OP);
	    if (op == Op.RANGE_SEARCH) {
            onReceiveRangeSearchOp(sender, mes);
        }
	    else if (op == Op.FOUND_IN_RANGE || op == Op.NOT_FOUND_IN_RANGE)  {
	        onReceiveRangeSearchResult(sender, mes);
	    }
	    else {
	        super.onReceive(sender, mes);
	    }
	}
	
	protected void onRangeMatch(Node sender, Map<Object,Object> args) {
	    Node startNode = (Node) args.get(SkipGraph.Arg.NODE);
	    List<Id> via = getVia(args);
	    try {
	        startNode.send(setVia(foundInRangeOp(self), via));
	    }
	    catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	public void onReceiveRangeSearchOp(Node sender, Map<Object,Object> mes) {
	    Node startNode = (Node) mes.get(SkipGraph.Arg.NODE);
        Range searchRange = (Range) mes.get(Arg.RANGE);
        int level = (int)((Integer)mes.get(SkipGraph.Arg.LEVEL));
        boolean found = (boolean)((Boolean)mes.get(Arg.FOUND));

        try {
            if (searchRange.includes(key)) {
                onRangeMatch(sender, mes);
                found = true;
            }
            if (compare(searchRange.min, key) < 0 && compare(key, searchRange.max) < 0) {
                // self is included. divide into two ranges.
                List<Range> divided = searchRange.divideC(key);
                self.send(rangeSearchOp(mes, divided.get(0), level, found));
                self.send(rangeSearchOp(mes, divided.get(1), level, found));
            }
            if (compare(key, searchRange.min) <= 0) {
                // self is less than the range.
                while (level >= 0) {
                    Comparable<?> rightKey = neighbors.getKey(R, level);
                    if (rightKey != null && compare(rightKey, searchRange.min) <= 0) {
                        System.out.println(String.format("R: KEY:%4s, LEVEL:%2d, MV:%s", key, level, m.toString()));
                        neighbors.get(R, level).send(rangeSearchOp(mes, searchRange, level, found));
                        break;
                    }
                    else {
                        if (rightKey != null && searchRange.includes(rightKey)) {
                            List<Range> divided = searchRange.divideR(rightKey);
                            System.out.println(String.format("R: KEY:%4s, LEVEL:%2d, MV:%s, Range: %s", key, level, m.toString(), divided.get(1)));
                            neighbors.get(R, level).send(rangeSearchOp(mes, divided.get(1), level, found));
                            searchRange = divided.get(0);
                        }
                        level--;
                    }
                }
            }
            if (compare(searchRange.max, key) <= 0) {
                // self is greater than the range.
                while (level >= 0) {
                    Comparable<?> leftKey = neighbors.getKey(L, level);
                    if (leftKey != null && compare(searchRange.max, leftKey) <= 0) {
                        neighbors.get(L, level).send(rangeSearchOp(mes, searchRange, level, found));
                        break;
                    }
                    else {
                        if (leftKey != null && searchRange.includes(leftKey)) {
                            List<Range> divided = searchRange.divideL(leftKey);
                            neighbors.get(L, level).send(rangeSearchOp(mes, divided.get(0), level, found));
                            searchRange = divided.get(1); 
                        }
                        level--;
                    }
                }
            }
            if (level < 0 && !found) {
                startNode.send(setVia(notFoundInRangeOp(self), getVia(mes)));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	private void dumpMes(Map<Object,Object> mes) {
	    for (Object key: mes.keySet()) {
	        System.out.println(key + ": " + mes.get(key));
	    }
	}
	
}