package org.piax.ov.ovs.rrsg;

import static org.piax.trans.Literals.map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.grlea.log.SimpleLogger;
import org.piax.ov.common.KeyComparator;
import org.piax.ov.common.Range;
import org.piax.ov.ovs.skipgraph.SkipGraph;

import org.piax.trans.Node;
import org.piax.trans.common.Id;
import org.piax.trans.sim.SimTransportOracle;

// range retrieval extension of skipgraph
public class RRSkipGraph extends SkipGraph {
    SimpleLogger log = new SimpleLogger(RRSkipGraph.class);
	static public enum Arg {
            RANGE
        }
	static public enum Op {
	    RANGE_SEARCH, FOUND_IN_RANGE, NOT_FOUND_IN_RANGE
	}
	public RRSkipGraph(Node self) {
		super(self);
	}
	
	protected class SearchResult {
	    public List<Node> matches;
	    public List<Node> unmatches;
	    public List<List<Id>> mVias;
	    public List<List<Id>> uVias;
	    public SearchResult() {
	        matches = new ArrayList<Node>();
	        unmatches = new ArrayList<Node>();
	        mVias = new ArrayList<List<Id>>();
	        uVias = new ArrayList<List<Id>>();
	    }
	    public void addMatch(Node node) {
            matches.add(node);
        }
	    public void addUnmatch(Node node) {
            unmatches.add(node);
        }
	    public void addMatchVia(List<Id> via) {
            mVias.add(via);
        }
	    public void addUnmatchVia(List<Id> via) {
            uVias.add(via);
        }
	}
	SearchResult searchResult;
	
    protected Map<Object,Object> foundInRangeOp(Node v) {
        return map((Object)SkipGraph.Arg.OP, (Object)Op.FOUND_IN_RANGE).map(SkipGraph.Arg.NODE, v);
    }
    
    protected Map<Object,Object> notFoundInRangeOp(Node v) {
        return map((Object)SkipGraph.Arg.OP, (Object)Op.NOT_FOUND_IN_RANGE).map(SkipGraph.Arg.NODE, v);
    }
	
	protected Map<Object,Object> rangeSearchOp(Node startNode, Range searchRange, int level) {
	    return map((Object)SkipGraph.Arg.OP, (Object)Op.RANGE_SEARCH).map(SkipGraph.Arg.NODE, startNode).map(Arg.RANGE, searchRange).map(SkipGraph.Arg.LEVEL, level);
	}
	
	protected Map<Object,Object> rangeSendOp(Range searchRange, int level, Object body) {
        return map((Object)SkipGraph.Arg.OP, (Object)Op.RANGE_SEARCH).map(Arg.RANGE, searchRange).map(SkipGraph.Arg.LEVEL, level).map(SkipGraph.Arg.BODY, body);
    }
	
	protected Map<Object,Object> opUpdate(Map<Object,Object> mes, Range searchRange, int level) {
	    Map<Object,Object> newMes = new HashMap<Object,Object>();
	    newMes.putAll(mes);
	    newMes.put(Arg.RANGE, searchRange);
	    newMes.put(SkipGraph.Arg.LEVEL, level);
	    return newMes;
	}

	protected void onReceiveRangeSearchResult(Node sender, Map<Object,Object> arg) {
        Op op = (Op) arg.get(SkipGraph.Arg.OP);
        List<Id> via = getVia(arg);
        Node node = (Node) arg.get(SkipGraph.Arg.NODE);
        if (op == Op.FOUND_IN_RANGE){
            searchResult.addMatch(node);
            searchResult.addMatchVia(via);
        }
        else {
            searchResult.addUnmatch(node);
            searchResult.addUnmatchVia(via);
        }
    }
	
	public List<Node> search(Range range) {
		self.trans.setParameter(SimTransportOracle.Param.NestedWait, Boolean.FALSE); // for better performance
        searchResult = new SearchResult();
        onReceiveRangeSearchOp(self, rangeSearchOp(self, range, getMaxLevel()));
        synchronized(searchResult) {
            try {
                searchResult.wait(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Collections.sort(searchResult.matches, new KeySortComparator());
        return searchResult.matches;
	}
	
	@Override
    public void send(Range range, Object body) {
	    self.trans.setParameter(SimTransportOracle.Param.NestedWait, Boolean.FALSE); // for better performance
        onReceiveRangeSearchOp(self, rangeSendOp(range, getMaxLevel(), body));
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
	    Object body = args.get(SkipGraph.Arg.BODY);
	    try {
	        if (body != null) {
                // Received body
	            inspectObject(body, self, args);
            }
	        else {
	            startNode.send(setVia(foundInRangeOp(self), via));	            
	        }
	    }
	    catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	public void onReceiveRangeSearchOp(Node sender, Map<Object,Object> mes) {
	    Node startNode = (Node) mes.get(SkipGraph.Arg.NODE);
        Range searchRange = (Range) mes.get(Arg.RANGE);
        int level = (int)((Integer)mes.get(SkipGraph.Arg.LEVEL));
        boolean selfMatched = false;
        
        try {
            if (searchRange.includes(key)) {
                onRangeMatch(sender, mes);
                selfMatched = true;
            }
            if (compare(searchRange.min, key) < 0 && compare(key, searchRange.max) < 0) {
                // self is included. divide into two ranges.
                List<Range> divided = searchRange.divideC(key);
                self.send(opUpdate(mes, divided.get(0), level));
                self.send(opUpdate(mes, divided.get(1), level));
            }
            if (compare(key, searchRange.min) <= 0) {
                // self is less than the range.
                while (level >= 0) {
                    Comparable<?> rightKey = neighbors.getKey(R, level);
                    if (rightKey != null && compare(rightKey, searchRange.min) <= 0) {
                        //System.out.println(String.format("R: KEY:%4s, LEVEL:%2d, MV:%s", key, level, m.toString()));
                        neighbors.get(R, level).send(opUpdate(mes, searchRange, level));
                        break;
                    }
                    else {
                        if (rightKey != null && searchRange.includes(rightKey)) {
                            List<Range> divided = searchRange.divideR(rightKey);
                            //System.out.println(String.format("R: KEY:%4s, LEVEL:%2d, MV:%s, Range: %s", key, level, m.toString(), divided.get(1)));
                            neighbors.get(R, level).send(opUpdate(mes, divided.get(1), level));
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
                        neighbors.get(L, level).send(opUpdate(mes, searchRange, level));
                        break;
                    }
                    else {
                        if (leftKey != null && searchRange.includes(leftKey)) {
                            List<Range> divided = searchRange.divideL(leftKey);
                            neighbors.get(L, level).send(opUpdate(mes, divided.get(0), level));
                            searchRange = divided.get(1); 
                        }
                        level--;
                    }
                }
            }
            if (level < 0 && !selfMatched) {
                if (startNode != null) {
                    startNode.send(setVia(notFoundInRangeOp(self), getVia(mes)));
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	protected void dumpMes(Map<Object,Object> mes) {
	    for (Object key: mes.keySet()) {
	        System.out.println(key + ": " + mes.get(key));
	    }
	}
	
}