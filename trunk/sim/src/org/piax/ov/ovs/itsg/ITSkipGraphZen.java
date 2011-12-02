package org.piax.ov.ovs.itsg;

import static org.piax.trans.Literals.map;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.grlea.log.SimpleLogger;
import org.piax.ov.OverlayManager;
import org.piax.ov.common.KeyComparator;
import org.piax.ov.ovs.risg.RISkipGraph.Op;
import org.piax.ov.ovs.rrsg.RRSkipGraph;
import org.piax.ov.ovs.skipgraph.MembershipVector;
import org.piax.ov.ovs.skipgraph.SkipGraph;
import org.piax.ov.ovs.skipgraph.SkipGraph.Arg;

import org.piax.trans.Node;
import org.piax.trans.ResponseChecker;
import org.piax.trans.common.Id;
import org.piax.trans.sim.SimTransportOracle;

public class ITSkipGraphZen extends SkipGraph {
    SimpleLogger log = new SimpleLogger(ITSkipGraph.class);
    public Comparable<?> rangeEnd;
    MaxTable maxes;
    
    static public enum Arg {
        MAX, OPTIONAL_NODE, TOP_MAX, ONOFF, DIRECTION
    }
    static public enum Op {
        BUDDY_WITH_MAX, SET_LINK_WITH_MAX, GET_LINK_WITH_MAX, UPDATE_MAX, UPDATE_LEFT_NEIGHBOR_MAX, UPDATE_LEVEL_MAX_NODE
    }
    
    public static int LEFT_MAX = 0;
    public static int LEFT_NEIGHBOR_MAX = 1;
    public static int LEFT_MAX_NODE = 2;
    
    public static int ON = 1;
    public static int OFF = 0;
    public static int DOWN = 1;
    public static int UP = 0;
      
    public ITSkipGraphZen(Node self) {
        super(self);
        this.rangeEnd = (Comparable<?>)self.getAttr(OverlayManager.RANGE_END);
        maxes = new MaxTable();//System.out.println("Size = "+maxes.size());
    }

    public Comparable<?> getRangeEnd() {
        return rangeEnd;
    }
    
    protected Map<Object,Object> getLinkWithMaxOp(Node u, int side, int level, Comparable<?> max, Node optionalNode) {
        return map((Object)SkipGraph.Arg.OP, (Object)Op.GET_LINK_WITH_MAX).map(SkipGraph.Arg.NODE, u).map(SkipGraph.Arg.SIDE, side).map(SkipGraph.Arg.LEVEL, level).map(Arg.MAX, max).map(Arg.OPTIONAL_NODE, optionalNode);
    }
    
    protected Map<Object,Object> buddyWithMaxOp(Node node, int side, int level, MembershipVector m, Comparable<?> currentMax, Node optionalNode) {
        return map((Object)SkipGraph.Arg.OP, (Object)Op.BUDDY_WITH_MAX).map(SkipGraph.Arg.NODE, node).map(SkipGraph.Arg.SIDE, side).map(SkipGraph.Arg.LEVEL, level).map(SkipGraph.Arg.VAL, m).map(Arg.MAX, currentMax).map(Arg.OPTIONAL_NODE, optionalNode);
    }
    
    protected Map<Object,Object> setLinkWithMaxOp(Node node, int level, Comparable<?> max, Node optionalNode) {
        return map((Object)SkipGraph.Arg.OP, (Object)Op.SET_LINK_WITH_MAX).map(SkipGraph.Arg.NODE, node).map(SkipGraph.Arg.LEVEL, level).map(Arg.MAX, max).map(Arg.OPTIONAL_NODE, optionalNode);
    }
    protected Map<Object,Object> updateLevelMaxNodeOp(Node node, int level, int OnOff) {
        return map((Object)SkipGraph.Arg.OP, (Object)Op.UPDATE_LEVEL_MAX_NODE).map(SkipGraph.Arg.NODE, node).map(SkipGraph.Arg.LEVEL, level).map(Arg.ONOFF, OnOff);
    }
    protected Map<Object,Object> updateMaxOp(Node node, Comparable<?> max) {
        return map((Object)SkipGraph.Arg.OP, (Object)Op.UPDATE_MAX).map(SkipGraph.Arg.NODE, node).map(Arg.MAX, max);
    }
    
    protected Map<Object,Object> updateLeftNeighborMaxOp(int level, Comparable<?> max, Comparable<?> topMax, int direction) {
        return map((Object)SkipGraph.Arg.OP, (Object)Op.UPDATE_LEFT_NEIGHBOR_MAX).map(SkipGraph.Arg.LEVEL, level).map(Arg.MAX, max).map(Arg.TOP_MAX, topMax).map(Arg.DIRECTION, direction);
    }
    
    private Comparable<?> max (Comparable<?> val1, Comparable<?> val2) {
        if (val1 == null && val2 != null) {
            return val2;
        }
        else if (val1 != null && val2 == null) {
            return val1;
        }
        else if (val1 == null && val2 == null) {
            return null;
        }
        else if (compare(val1,val2)<0) {
        	return val2;
        }
        else return val1;
    }
    
    @Override
    public void insert(Node introducer) {
        int side, otherSide;
        self.trans.setParameter(SimTransportOracle.Param.NestedWait, Boolean.FALSE);
        if (introducer.equals(self)) {
            neighbors.put(L, 0, null);
            neighbors.put(R, 0, null);
            //maxLevel = 0;
        }
        else {
        	
            Comparable<?> introducerKey = getKey(introducer); 
            if (compare(introducerKey, key) < 0) {
                side = R;
                otherSide = L;
            }
            else {
                side = L;
                otherSide = R;
            }
            try {
                Map<Object,Object> mr = introducer.sendAndWait(getMaxLevelOp(), new CheckOp(SkipGraph.Op.RET_MAX_LEVEL));
                int maxLevel = (Integer)mr.get(SkipGraph.Arg.LEVEL);
              //  Map<Object, Object> sr = introducer.sendAndWait(searchOp(self, key, maxLevel - 1), new CheckOp(SkipGraph.Op.NOT_FOUND, SkipGraph.Op.FOUND));
                Map<Object, Object> sr = introducer.sendAndWait(searchOp(self, key, maxLevel), new CheckOp(SkipGraph.Op.NOT_FOUND, SkipGraph.Op.FOUND));
                if (sr.get(SkipGraph.Arg.OP) == SkipGraph.Op.FOUND) {
                    System.out.println("FOUND.");
                    return;
                }
                Node otherSideNeighbor = (Node)sr.get(SkipGraph.Arg.NODE);
                Map<Object, Object> nr = otherSideNeighbor.sendAndWait(getNeighborOp(side, 0), new CheckOp(SkipGraph.Op.RET_NEIGHBOR));
                Node sideNeighbor = (Node)nr.get(SkipGraph.Arg.NODE); 
                if (otherSideNeighbor.equals(sideNeighbor)) {
                    sideNeighbor = null;
                }
                if (otherSideNeighbor != null) {
                    Map<Object, Object> r = otherSideNeighbor.sendAndWait(getLinkOp(self, side, 0), new CheckOp(SkipGraph.Op.SET_LINK));
                    Node newNeighbor = (Node) r.get(SkipGraph.Arg.NODE);
                    int newLevel = (int)((Integer)r.get(SkipGraph.Arg.LEVEL));
                    neighbors.put(otherSide, newLevel, newNeighbor);
                }
                if (sideNeighbor != null) {
                    Map<Object, Object> r = sideNeighbor.sendAndWait(getLinkOp(self, otherSide, 0), new CheckOp(SkipGraph.Op.SET_LINK));
                    Node newNeighbor = (Node) r.get(SkipGraph.Arg.NODE);
                    int newLevel = (int)((Integer)r.get(SkipGraph.Arg.LEVEL));
                    neighbors.put(side, newLevel, newNeighbor);
                }
                maxes.put(LEFT_MAX, 0, rangeEnd);
                int l = 0;
                    while (true) {
                    l++;
                    m.randomElement(l);
                    Node optionalNode = null;
                    if (neighbors.get(L, l - 1) != null) {
                        Comparable<?> currentMax = null;
                        
                        Map<Object, Object> r = neighbors.get(L, l - 1).sendAndWait(buddyWithMaxOp(self, L, l - 1, m, currentMax,null), new CheckOp(Op.SET_LINK_WITH_MAX));
                        Node newNeighbor = (Node) r.get(SkipGraph.Arg.NODE);
                        neighbors.put(L, l, newNeighbor);
                        Comparable<?> max = (Comparable<?>) r.get(Arg.MAX);
                        maxes.put(LEFT_NEIGHBOR_MAX, l - 1 , max);
                        
                    }
                    else {
                        neighbors.put(L, l, null);
                    }
                    maxes.put(LEFT_MAX, l, max((Comparable<?>)maxes.get(LEFT_MAX, l-1),(Comparable<?>)maxes.get(LEFT_NEIGHBOR_MAX,l-1)));
                    if (neighbors.get(R, l - 1) != null) {
                        Comparable<?> currentMax = null;
                        Map<Object,Object> r = neighbors.get(R, l - 1).sendAndWait(buddyWithMaxOp(self, R, l - 1, m, currentMax,null), new CheckOp(Op.SET_LINK_WITH_MAX));
                        Node newNeighbor = (Node) r.get(SkipGraph.Arg.NODE);
                        neighbors.put(R, l, newNeighbor);
                        optionalNode = (Node) r.get(Arg.OPTIONAL_NODE);
                        if (optionalNode != null) {
                            maxes.put(LEFT_MAX_NODE, l-1, optionalNode);
                        }
                    }
                    else {
                        neighbors.put(R, l, null);
                    }
                    if (neighbors.get(R, l) == null && neighbors.get(L, l) == null) break;
                }
               
                maxLevel = l; if (maxLevel > 0 && neighbors.get(L, maxLevel-1)!= null){
                	neighbors.get(L,maxLevel-1).send(updateLevelMaxNodeOp(self,maxLevel-1,ON));	
                }
                
               self.send(updateMaxOp(self,(Comparable<?>)maxes.get(LEFT_MAX,maxLevel)));
               
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void change_neighbor_with_max(Node u, int side, int l, Comparable<?> max, Node optionalNode) {
        Comparable<?> sideKey = neighbors.getKey(side, l);
        Comparable<?> uKey = getKey(u);
//        System.out.println("Side= "+sideKey+"   u=  "+uKey);
        int cmp = 0;
        if (sideKey != null) {
            if (side == R) {
                cmp = compare(sideKey, uKey);
            }
            else {
                cmp = compare(uKey, sideKey);
            }
        }
        Node to;
        Map<Object,Object> arg;
        if (cmp < 0) {
            to = neighbors.get(side, l);
            arg = getLinkWithMaxOp(u, side, l, max,optionalNode);
        }
        else {
        	to = u;
            arg = setLinkWithMaxOp(self, l, max, optionalNode);
        }
        try {
            to.send(arg);
            neighbors.put(side, l, u);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    protected void onReceiveGetLinkWithMaxOp(Node sender, Map<Object, Object> args) {
        Node u = (Node) args.get(SkipGraph.Arg.NODE);
        int side = (int)((Integer)args.get(SkipGraph.Arg.SIDE));
        int l = (int)((Integer)args.get(SkipGraph.Arg.LEVEL));
        Comparable<?> max = (Comparable<?>) args.get(Arg.MAX);
        Node optionalNode = (Node) args.get(Arg.OPTIONAL_NODE);
        change_neighbor_with_max(u, side, l, max,optionalNode);
    }
    
   
    
    protected void onReceiveBuddyWithMaxOp(Node sender, Map<Object,Object> args) {
        Node u = (Node) args.get(SkipGraph.Arg.NODE);
        Comparable<?> uKey = getKey(u);
        Node optionalNode = (Node) args.get(Arg.OPTIONAL_NODE);
        int l = (int)((Integer)args.get(SkipGraph.Arg.LEVEL));
        MembershipVector val = (MembershipVector) args.get(SkipGraph.Arg.VAL);
        Comparable<?> max = (Comparable<?>) args.get(Arg.MAX);
        List<Id> via = getVia(args);
        int side = (int)((Integer)args.get(SkipGraph.Arg.SIDE));
       
        if (!m.existsElementAt(l+1)) {
        
        	if (getMaxLevel()>0)
        	try {
        		if (neighbors.get(L,getMaxLevel()-1) != null) {
        			neighbors.get(L, getMaxLevel()-1).send(updateLevelMaxNodeOp(self,getMaxLevel()-1,OFF));	
        		}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            m.randomElement(l+1);
            neighbors.put(L, l+1, null);
            neighbors.put(R, l+1, null);
            maxes.put(LEFT_MAX, l + 1, max((Comparable<?>)maxes.get(LEFT_MAX, l), (Comparable<?>)maxes.get(LEFT_NEIGHBOR_MAX, l)));
            
        }
       
        if (side == L) {
        	if (compare((Comparable<?>)neighbors.getKey(R, l),uKey)==0) {
        		maxes.put(LEFT_MAX_NODE, l, null);
        	}
            Comparable<?> currentMax = null;
            if (m.equals(l+1, val)) {  //this node: turn back
                currentMax = max;
                    change_neighbor_with_max(u,R,l+1,currentMax,null);
            }									
            else {  //intermediate nodes
            	try {
                    currentMax = max((Comparable<?>) maxes.get(LEFT_MAX, l), max);
                    if (neighbors.get(L, l) != null) { // go on
                        neighbors.get(L, l).send(setVia(buddyWithMaxOp(u, L, l, val, currentMax,null), via));
                    }
                    else {  // the end: turn back
                        u.send(setVia(setLinkWithMaxOp(null, l+1, currentMax, null), via));
                        }
                    }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        // side == R (buddyRight)
        else {
        	if (m.equals(l+1, val)) { // this node: turn back
                maxes.put(LEFT_NEIGHBOR_MAX, l, max);
                maxes.put(LEFT_MAX, l + 1, max((Comparable<?>)maxes.get(LEFT_MAX, l), (Comparable<?>)maxes.get(LEFT_NEIGHBOR_MAX, l)));
               // System.out.println("levelMax = "+(l+1)+" ------------"+ maxes.get(LEFT_MAX, l+1));
                    change_neighbor_with_max(u,L,l+1,null,optionalNode);
                
            }
            else { //intermediate nodes
            	if (compare((Comparable<?>)neighbors.getKey(L, l),(Comparable<?>)uKey)==0 &&(l==getMaxLevel()-1)){
            		optionalNode = self;
            	}
                Comparable<?> currentMax = null;
                currentMax = max((Comparable<?>)maxes.get(LEFT_MAX, l), max);
       
                try {
                    if (neighbors.get(R, l) != null) { //go on
                        neighbors.get(R, l).send(buddyWithMaxOp(u, R, l, val, currentMax,optionalNode));
                    }
                    else { // the end: turn back
                        u.send(setVia(setLinkWithMaxOp(null, l+1, null, optionalNode),via));
                        
                        	
                    }
                    
                }
                catch (IOException e) {
                }
            }
        }
    }
    protected void onReceiveUpdateLevelMaxNodeOp(Node sender, Map<Object,Object> args) {
    	 Node u = (Node) args.get(SkipGraph.Arg.NODE);
    	 int l = (int)((Integer)args.get(SkipGraph.Arg.LEVEL));
    	 int onOff = (int) ((Integer)args.get(Arg.ONOFF));
    	 if (onOff == ON) {
    		 maxes.put(LEFT_MAX_NODE, l, u);	 
    	 }
    	 else {
    		 maxes.put(LEFT_MAX_NODE,l,null);
    	 }
    }
    protected void onReceiveUpdateMaxOp(Node sender, Map<Object,Object> args) {
        Node u = (Node) args.get(SkipGraph.Arg.NODE);
        Comparable<?> uKey = getKey(u);
        Comparable<?> max = (Comparable<?>) args.get(Arg.MAX);
        int l = getMaxLevel();
        try {
            if (u.equals(self)) {
            	
            	while (l > 0) {
            		boolean sendingFlag = true;
                    l--;
                    if (neighbors.get(R, l + 1) != null && neighbors.get(R, l + 1).equals(neighbors.get(R, l))) {
                    	sendingFlag = false;
                    }
                    if (sendingFlag == true){ 
                    	if (neighbors.get(R,l)!= null){
                    	neighbors.get(R, l).send(updateLeftNeighborMaxOp(l, (Comparable<?>)maxes.get(LEFT_MAX, l), (Comparable<?>)maxes.get(LEFT_MAX, getMaxLevel()),DOWN));
                    	}
                    		
                    }
                    // send updateLNM to all the right side neighbors
                }
                if (neighbors.get(R, 0) != null && max != null && compare(rangeEnd, max) == 0) {
                    neighbors.get(R, 0).send(updateMaxOp(u, max));
                }  // forward the update messages to neighboring node.
            }
            else {
                boolean changeFlag = false;
                boolean sendingFlag = false;
                if ((compare((Comparable<?>)maxes.get(LEFT_MAX, getMaxLevel()), max) < 0)) {
                    maxes.put(LEFT_MAX, getMaxLevel(), max);
                    changeFlag = true;
                    sendingFlag = true;
                    for (int i=0;i<getMaxLevel();i++) {                          // send updateLNM to all nodes in LevelMax List
                    	if(maxes.get(LEFT_MAX_NODE, i)!= null) {
                    		System.out.println("44444444444"+maxes.get(LEFT_MAX_NODE, i));
                    		Node to = (Node)maxes.get(LEFT_MAX_NODE, i);
                    		to.send(updateLeftNeighborMaxOp(l,(Comparable<?>)maxes.get(LEFT_MAX, l), (Comparable<?>)maxes.get(LEFT_MAX, getMaxLevel()),DOWN));
                    	}
                    }
                }
                while (l > 0 && changeFlag) {
                    l--;
                    if (compare(neighbors.getKey(L, l), uKey) >= 0) {
                        if (maxes.get(LEFT_NEIGHBOR_MAX, l) != null && compare((Comparable<?>)maxes.get(LEFT_NEIGHBOR_MAX, l), (Comparable<?>)maxes.get(LEFT_MAX, l + 1)) < 0) {
                            maxes.put(LEFT_NEIGHBOR_MAX, l, maxes.get(LEFT_MAX, l + 1));
                            changeFlag = true;
                        }
                        else {
                            changeFlag = false;
                        }
                    }
                    else {
                        if (compare((Comparable<?>)maxes.get(LEFT_MAX, l), (Comparable<?>)maxes.get(LEFT_MAX, l + 1)) < 0) {
                            maxes.put(LEFT_MAX, l, maxes.get(LEFT_MAX, l + 1));
                            if (neighbors.get(R, l)!=null){
                            	neighbors.get(R, l).send(updateLeftNeighborMaxOp(l, (Comparable<?>)maxes.get(LEFT_MAX, l), (Comparable<?>)maxes.get(LEFT_MAX, getMaxLevel()),DOWN));	
                            }
                            changeFlag = true;
                        }
                        else {
                            changeFlag = false;
                        }
                    }
                    if (sendingFlag) {
                    	if (neighbors.get(R,0)!=null){
                    		neighbors.get(R, 0).send(updateMaxOp(u, (Comparable<?>)maxes.get(LEFT_MAX, getMaxLevel())));	
                    	}
                        
                    }
                }
            }
        }
        catch (IOException e) {
        }
    }
    
    protected void onReceiveUpdateLeftNeighborMaxOp(Node sender, Map<Object,Object> args) {
        int l = (Integer)args.get(SkipGraph.Arg.LEVEL);
        int direction = (Integer)args.get(Arg.DIRECTION);
        Comparable<?> max = (Comparable<?>) args.get(Arg.MAX);
        Comparable<?> topMax = (Comparable<?>) args.get(Arg.TOP_MAX);
//        if ((l == getMaxLevel() - 1) && compare((Comparable<?>)maxes.get(LEFT_NEIGHBOR_MAX, l), topMax) < 0) {
//            maxes.put(LEFT_NEIGHBOR_MAX, l, topMax);
//        }
        if (l==getMaxLevel()-1){
        	if (maxes.get(LEFT_NEIGHBOR_MAX,l)!=null&& topMax!=null) {
        		if (compare((Comparable<?>)maxes.get(LEFT_NEIGHBOR_MAX, l), topMax) < 0) {
                  maxes.put(LEFT_NEIGHBOR_MAX, l, topMax);
        		}
        	}
        	else {
        		
        		maxes.put(LEFT_NEIGHBOR_MAX, l, topMax);
        	}
        		

        }
        boolean changeFlag = false;
        if (l < getMaxLevel() - 1) {
            if ((maxes.get(LEFT_NEIGHBOR_MAX, l)!= null && compare((Comparable<?>)maxes.get(LEFT_NEIGHBOR_MAX, l),(Comparable<?>) max) < 0)||(direction==DOWN && maxes.get(LEFT_NEIGHBOR_MAX, l)== null)) {
                changeFlag = true;
            }
        }
        while (changeFlag && l < getMaxLevel() - 1) {
            if (maxes.get(LEFT_NEIGHBOR_MAX, l)!= null && compare((Comparable<?>)maxes.get(LEFT_NEIGHBOR_MAX, l),(Comparable<?>) max) < 0) {
                
                if (compare((Comparable<?>)maxes.get(LEFT_NEIGHBOR_MAX, l), (Comparable<?>)maxes.get(LEFT_MAX, l + 1)) > 0) {
                    maxes.put(LEFT_MAX, l + 1, maxes.get(LEFT_NEIGHBOR_MAX, l));
                    try { if (neighbors.get(R, l+1)!=null)
                        neighbors.get(R, l + 1).send(updateLeftNeighborMaxOp(l + 1, (Comparable<?>)maxes.get(LEFT_MAX, l + 1), null,UP));
                    } catch (IOException e) {
                    }
                }
                else {
                    changeFlag = false;
                }
                l++;
            }
            else if (maxes.get(LEFT_NEIGHBOR_MAX, l)== null && direction== DOWN){
            	maxes.put(LEFT_NEIGHBOR_MAX, l, max);
            	}
            else {
                changeFlag = false;
            }
        }
    }
    
    public void onReceive(Node sender, Map<Object,Object> mes) {
        Object op = mes.get(SkipGraph.Arg.OP);
        // handle BUDDY_WITH_MAX, (SET_LINK_WITH_MAX,) GET_LINK_WITH_MAX, UPDATE_MAX, UPDATE_LEFT_NEIGHBOR_MAX
        if (op == Op.BUDDY_WITH_MAX) {
            onReceiveBuddyWithMaxOp(sender, mes);
        }
        else if (op == Op.GET_LINK_WITH_MAX) {
            onReceiveGetLinkWithMaxOp(sender, mes);
        }
        else if (op == Op.UPDATE_MAX) {
            onReceiveUpdateMaxOp(sender, mes);
        }
        else if (op == Op.UPDATE_LEFT_NEIGHBOR_MAX) {
            onReceiveUpdateLeftNeighborMaxOp(sender, mes);
        }
        else if (op == Op.UPDATE_LEVEL_MAX_NODE) {
            onReceiveUpdateLevelMaxNodeOp(sender, mes);
        }
        else {
            super.onReceive(sender, mes);
        }
    }
    
    public String toString() {
    	System.out.println();
        return "[" + getKey() + "," + getRangeEnd() + "]\n" + neighbors.toString() + maxes.toString() + "max Level= " + getMaxLevel() + " maxTable size= "+ maxes.size() ;
    }
}

