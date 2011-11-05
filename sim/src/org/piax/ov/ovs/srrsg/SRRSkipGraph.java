package org.piax.ov.ovs.srrsg;

import static org.piax.trans.Literals.map;

import java.io.IOException;
import java.util.ArrayList;
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

// serial range retrieval extension of skipgraph
public class SRRSkipGraph extends SkipGraph {
    SimpleLogger log = new SimpleLogger(SRRSkipGraph.class);
	static public enum Arg {
	    RANGE, MATCHES
	}
	static public enum Op {
	    SEEK, SCAN, END
	}
	public SRRSkipGraph(Node self) {
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
	
	protected Map<Object,Object> seekOp(Node startNode, Range searchRange, int level) {
        return map((Object)SkipGraph.Arg.OP, (Object)Op.SEEK).map(SkipGraph.Arg.NODE, startNode).map(Arg.RANGE, searchRange).map(SkipGraph.Arg.LEVEL, level);
    }
	
	protected Map<Object,Object> seekWithBodyOp(Range searchRange, int level, Object body) {
        return map((Object)SkipGraph.Arg.OP, (Object)Op.SEEK).map(Arg.RANGE, searchRange).map(SkipGraph.Arg.LEVEL, level).map(SkipGraph.Arg.BODY, body);
    }
    
//    protected Map<Object,Object> scanOp(Node startNode, Range searchRange) {
//        return map((Object)SkipGraph.Arg.OP, (Object)Op.SCAN).map(SkipGraph.Arg.NODE, startNode).map(Arg.RANGE, searchRange);
//    }
    
    protected Map<Object,Object> seekOp(Map<Object,Object> mes, Range searchRange, int level) {
        Map<Object,Object> newMes = new HashMap<Object,Object>();
        newMes.putAll(mes);
        newMes.put(SkipGraph.Arg.LEVEL, level);
        newMes.put(Arg.RANGE, searchRange);
        return newMes;
    }
    
    protected Map<Object,Object> scanOp(Map<Object,Object> mes) {
        Map<Object,Object> newMes = new HashMap<Object,Object>();
        newMes.putAll(mes);
        newMes.put((Object)SkipGraph.Arg.OP, (Object)Op.SCAN);
//        newMes.put(Arg.RANGE, searchRange);
        return newMes;
    }
    
    protected Map<Object,Object> endOp(Map<Object,Object> mes) {
        Map<Object,Object> newMes = new HashMap<Object,Object>();
        newMes.putAll(mes);
        newMes.put(SkipGraph.Arg.OP, Op.END);
        return newMes;
    }    

//    protected Map<Object,Object> endOp(ArrayList<Node> matches) {
//        return map((Object)SkipGraph.Arg.OP, (Object)Op.END).map(Arg.MATCHES, matches);
//    }
    
	public List<Node> search(Range range) {
        self.trans.setParameter(SimTransportOracle.Param.NestedWait, Boolean.FALSE); // for better performance
        Map<Object, Object> ret = null;
        try {
            ret = self.sendAndWait(seekOp(self, range, getMaxLevel()), new CheckOp(Op.END));
            List<Id> via = this.getVia(ret);
            System.out.println("hops:" + via.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret == null ? null : (List<Node>)ret.get(Arg.MATCHES);
	}
	
	@Override
    public void send(Range range, Object body) {
        self.trans.setParameter(SimTransportOracle.Param.NestedWait, Boolean.FALSE); // for better performance
        try {
            self.send(seekWithBodyOp(range, getMaxLevel(), body));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	protected int compare(Comparable<?> a, Comparable<?>b) {
        return KeyComparator.getInstance().compare(a, b);
    }
	
	public void onReceive(Node sender, Map<Object,Object> mes) {
	    Object op = mes.get(SkipGraph.Arg.OP);
	    if (op == Op.SEEK) {
            onReceiveSeekOp(sender, mes);
        }
	    else if (op == Op.SCAN)  {
	        onReceiveScanOp(sender, mes);
	    }
	    else {
	        super.onReceive(sender, mes);
	    }
	}
	protected void onRangeMatch(Node sender, Map<Object,Object> args) {
        ArrayList<Node> matches = (ArrayList<Node>)args.get(Arg.MATCHES);
        Object body = args.get(SkipGraph.Arg.BODY);
        List<Id> via = getVia(args);
        if (body != null) {
            inspectObject(body, self, args);
            // log.info("send hops =" + (via == null ? 0 : via.size()));
            //System.out.println("key=" + key + " received -> " + body);
        }
        else {
            if (matches == null) {
                matches = new ArrayList<Node>();
            }
            matches.add(self);
            args.put(Arg.MATCHES, matches);
        }
    }
	
	protected void onReceiveScanOp(Node sender, Map<Object,Object> args) {
	    Node startNode = (Node) args.get(SkipGraph.Arg.NODE);
        Range searchRange = (Range) args.get(Arg.RANGE);
        Object body = args.get(SkipGraph.Arg.BODY);
        //ArrayList<Node> matches = (ArrayList<Node>)args.get(Arg.MATCHES);
        //List<Id> via = getVia(args);
        if (searchRange.includes(key)) {
            onRangeMatch(sender, args);
        }
        if (neighbors.get(R, 0) != null && searchRange.includes(neighbors.getKey(R, 0))) {
            try {
                neighbors.get(R, 0).send(scanOp(args));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            if (body == null) {
                try {
//                System.out.println("END:" + getKey() + "->" + neighbors.getKey(R, 0)  + ",searchRange=" + searchRange);
                    if (startNode != null) {
                        startNode.send(endOp(args));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
	}
	
	protected void onReceiveSeekOp(Node sender, Map<Object,Object> args) {
        Range searchRange = (Range) args.get(Arg.RANGE);
        int level = (int)((Integer)args.get(SkipGraph.Arg.LEVEL));
        List<Id> via = getVia(args);
        try {
            if (compare(key, searchRange.min) == 0) {
                self.send(setVia(scanOp(args), via));
            }
            else if (compare(key, searchRange.min) < 0) {
                while (level >= 0) {
                    Comparable<?> rightKey = neighbors.getKey(R, level);
                    if (rightKey != null && compare(rightKey, searchRange.min) <= 0) {
                        //System.out.println(String.format("R: KEY:%4d, LEVEL:%2d, MV:%s", key, level, m.toString()));
                        neighbors.get(R, level).send(seekOp(args, searchRange, level));
                        break;
                    }
                    else {
                        level--;
                    }
                }
            }
            else {
                while (level >= 0) {
                    Comparable<?> leftKey = neighbors.getKey(L, level);
                    if (leftKey != null && compare(searchRange.min, leftKey) <= 0) {
                        //System.out.println(String.format("L: KEY:%4s, LEVEL:%2d, MV:%s", key, level, m.toString()));
                        neighbors.get(L, level).send(seekOp(args, searchRange, level));
                        break;
                    }
                    else {
                        level--;
                    }
                }
            }
            if (level < 0) {
                self.send(setVia(scanOp(args), via));
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