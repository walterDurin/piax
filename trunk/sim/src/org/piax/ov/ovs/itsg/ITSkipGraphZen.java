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
        MAX, OPTIONAL_NODE, TOP_MAX
    }
    static public enum Op {
        BUDDY_WITH_MAX, SET_LINK_WITH_MAX, GET_LINK_WITH_MAX, UPDATE_MAX, UPDATE_LEFT_NEIGHBOR_MAX
    }
    
    public static int LEFT_MAX = 0;
    public static int LEFT_NEIGHBOR_MAX = 1;
    public static int LEFT_MAX_NODE = 2;
    
    
    public ITSkipGraphZen(Node self) {
        super(self);
        this.rangeEnd = (Comparable<?>)self.getAttr(OverlayManager.RANGE_END);
        maxes = new MaxTable();
    }

    public Comparable<?> getRangeEnd() {
        return rangeEnd;
    }
    
    protected Map<Object,Object> getLinkWithMaxOp(Node u, int side, int level, Comparable<?> max) {
        return map((Object)SkipGraph.Arg.OP, (Object)Op.GET_LINK_WITH_MAX).map(SkipGraph.Arg.NODE, u).map(SkipGraph.Arg.SIDE, side).map(SkipGraph.Arg.LEVEL, level).map(Arg.MAX, max);
    }
    
    protected Map<Object,Object> buddyWithMaxOp(Node node, int side, int level, MembershipVector m, Comparable<?> currentMax) {
        return map((Object)SkipGraph.Arg.OP, (Object)Op.BUDDY_WITH_MAX).map(SkipGraph.Arg.NODE, node).map(SkipGraph.Arg.SIDE, side).map(SkipGraph.Arg.LEVEL, level).map(SkipGraph.Arg.VAL, m).map(Arg.MAX, currentMax);
    }
    
    protected Map<Object,Object> setLinkWithMaxOp(Node node, int level, Comparable<?> max, Node optionalNode) {
        return map((Object)SkipGraph.Arg.OP, (Object)Op.SET_LINK_WITH_MAX).map(SkipGraph.Arg.NODE, node).map(SkipGraph.Arg.LEVEL, level).map(Arg.MAX, max).map(Arg.OPTIONAL_NODE, optionalNode);
    }
    
    protected Map<Object,Object> updateMaxOp(Node node, Comparable<?> max) {
        return map((Object)SkipGraph.Arg.OP, (Object)Op.UPDATE_MAX).map(SkipGraph.Arg.NODE, node).map(Arg.MAX, max);
    }
    
    protected Map<Object,Object> updateLeftNeighborMaxOp(int level, Comparable<?> max, Comparable<?> topMax) {
        return map((Object)SkipGraph.Arg.OP, (Object)Op.UPDATE_LEFT_NEIGHBOR_MAX).map(SkipGraph.Arg.LEVEL, level).map(Arg.MAX, max).map(Arg.TOP_MAX, topMax);
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
        return (compare(val1, val2) > 0)? val1 : val2; 
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
                
                int l = 0;
                maxes.put(LEFT_MAX, 0, rangeEnd);
                while (true) {
                    l++;
                    m.randomElement(l);
                    Node optionalNode = null;
                    if (neighbors.get(L, l - 1) != null) {
                        Comparable<?> currentMax = null;
                        Map<Object, Object> r = neighbors.get(L, l - 1).sendAndWait(buddyWithMaxOp(self, L, l - 1, m, currentMax), new CheckOp(Op.SET_LINK_WITH_MAX));
                        Node newNeighbor = (Node) r.get(SkipGraph.Arg.NODE);
                        neighbors.put(L, l, newNeighbor);
                        Comparable<?> max = (Comparable<?>) r.get(Arg.MAX);
                        maxes.put(LEFT_NEIGHBOR_MAX, l - 1 , max);
                        optionalNode = (Node) r.get(Arg.OPTIONAL_NODE);
                    }
                    else {
                        neighbors.put(L, l, null);
                    }
                   
                    
                    maxes.put(LEFT_MAX, l, max((Comparable<?>)maxes.get(LEFT_MAX, l-1),(Comparable<?>)maxes.get(LEFT_NEIGHBOR_MAX,l-1)));
                  
                    if (neighbors.get(R, l - 1) != null) {
                        Comparable<?> currentMax = null;
                        Map<Object,Object> r = neighbors.get(R, l - 1).sendAndWait(buddyWithMaxOp(self, R, l - 1, m, currentMax), new CheckOp(Op.SET_LINK_WITH_MAX));
                        Node newNeighbor = (Node) r.get(SkipGraph.Arg.NODE);
                        neighbors.put(R, l, newNeighbor);
                        if (optionalNode != null) {
                            maxes.put(LEFT_MAX_NODE, l, optionalNode);
                        }
                    }
                    else {
                        neighbors.put(R, l, null);
                    }
                    if (neighbors.get(R, l) == null && neighbors.get(L, l) == null) break;
                }
               
                maxLevel = l;
          //      self.send(updateMaxOp(self,(Comparable<?>)maxes.get(LEFT_MAX,maxLevel)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void change_neighbor_with_max(Node u, int side, int l, Comparable<?> max) {
        Node optionalNode = null;
   
        Comparable<?> sideKey = neighbors.getKey(side, l);
        Comparable<?> uKey = getKey(u);
        if (side== R && neighbors.get(R, l) != null && compare(neighbors.getKey(R, l), uKey) == 0  ) {
        	 maxes.put(LEFT_MAX_NODE, l, u);
        }
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
            arg = getLinkWithMaxOp(u, side, l, max);
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
        change_neighbor_with_max(u, side, l, max);
    }
    
    protected void onReceiveBuddyWithMaxOp(Node sender, Map<Object,Object> args) {
        Node u = (Node) args.get(SkipGraph.Arg.NODE);
        Comparable<?> uKey = getKey(u); 
        int l = (int)((Integer)args.get(SkipGraph.Arg.LEVEL));
        MembershipVector val = (MembershipVector) args.get(SkipGraph.Arg.VAL);
        Comparable<?> max = (Comparable<?>) args.get(Arg.MAX);
        List<Id> via = getVia(args);
        int side = (int)((Integer)args.get(SkipGraph.Arg.SIDE));
        if (!m.existsElementAt(l+1)) {
            m.randomElement(l+1);
            neighbors.put(L, l+1, null);
            neighbors.put(R, l+1, null);
            
        }
        if (side == L) {
            Comparable<?> currentMax = null;
            if (m.equals(l+1, val)) {  //this node: turn back
                //change_neighbor(u, side, l);
            	//neighbors.put(R,l+1,u);
                currentMax = max;
               
                    change_neighbor_with_max(u,R,l+1,currentMax);
               
            }
            else {  //intermediate nodes
                try {
                    currentMax = max((Comparable<?>) maxes.get(LEFT_MAX, l), max);
                    if (neighbors.get(L, l) != null) { // go on
                        neighbors.get(L, l).send(setVia(buddyWithMaxOp(u, L, l, val, currentMax), via));
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
                
                    change_neighbor_with_max(u,L,l+1,null);
                
            }
            else { //intermediate nodes
                Comparable<?> currentMax = null;
                currentMax = max((Comparable<?>)maxes.get(LEFT_MAX, l), max);
                try {
                    if (neighbors.get(R, l) != null) { //go on
                        neighbors.get(R, l).send(buddyWithMaxOp(u, R, l, val, currentMax));
                    }
                    else { // the end: turn back
                        if(compare(neighbors.getKey(L, l),uKey)==0) {
                        	u.send(setLinkWithMaxOp(null,l+1,null,self));
                        }
                        else {
                        	u.send(setVia(setLinkWithMaxOp(null, l+1, null, null),via));
                        }
                        	
                    }
                    
                }
                catch (IOException e) {
                }
            }
        }
    }
    
    protected void onReceiveUpdateMaxOp(Node sender, Map<Object,Object> args) {
        Node u = (Node) args.get(SkipGraph.Arg.NODE);
        Comparable<?> uKey = getKey(u);
        Comparable<?> max = (Comparable<?>) args.get(Arg.MAX);
        int l = getMaxLevel();
        try {
            if (u.equals(self)) {
            	l = l-1; if (neighbors.get(R, l) != null) System.out.println("asgasgasgasggggggggggggggggggggggggggggggggggggggggg");
//                neighbors.get(R, l).send(updateLeftNeighborMaxOp(l, (Comparable<?>)maxes.get(LEFT_MAX, l), null));
                while (l > 0) {
                    l--;System.out.println("asgasgasgasgggggggg" + maxes.get(LEFT_MAX, l));
                    if (neighbors.get(R, l + 1) != null && neighbors.get(R, l + 1).equals(neighbors.get(R, l))) {
                        neighbors.get(R, l).send(updateLeftNeighborMaxOp(l, (Comparable<?>)maxes.get(LEFT_MAX, l), null));
                    }
                }
                if (neighbors.get(R, 0) != null && max != null && compare(rangeEnd, max) == 0) {
                    neighbors.get(R, 0).send(updateMaxOp(u, max));
                }
            }
            else {
                boolean changeFlag = false;
                boolean sendingFlag = false;
                if ((compare((Comparable<?>)maxes.get(LEFT_MAX, getMaxLevel()), max) < 0)) {
                    maxes.put(LEFT_MAX, getMaxLevel(), max);
                    changeFlag = true;
                    sendingFlag = true;
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
                            neighbors.get(R, l).send(updateLeftNeighborMaxOp(l, (Comparable<?>)maxes.get(LEFT_MAX, l), (Comparable<?>)maxes.get(LEFT_MAX, getMaxLevel())));
                            changeFlag = true;
                        }
                        else {
                            changeFlag = false;
                        }
                    }
                    if (sendingFlag) {
                        neighbors.get(R, 0).send(updateMaxOp(u, (Comparable<?>)maxes.get(LEFT_MAX, getMaxLevel())));
                    }
                }
            }
        }
        catch (IOException e) {
        }
    }
    
    protected void onReceiveUpdateLeftNeighborMaxOp(Node sender, Map<Object,Object> args) {
        int l = (Integer)args.get(SkipGraph.Arg.LEVEL);
        Comparable<?> max = (Comparable<?>) args.get(Arg.MAX);
        Comparable<?> topMax = (Comparable<?>) args.get(Arg.TOP_MAX);
        if ((l == getMaxLevel() - 1) && compare((Comparable<?>)maxes.get(LEFT_NEIGHBOR_MAX, l), topMax) < 0) {
            maxes.put(LEFT_NEIGHBOR_MAX, l, topMax);
        }
        boolean changeFlag = false;
        if (l < getMaxLevel() - 1) {
            if (compare((Comparable<?>)maxes.get(LEFT_NEIGHBOR_MAX, l), max) < 0) {
                changeFlag = true;
            }
        }
        while (!changeFlag && l < getMaxLevel() - 1) {
            if (compare((Comparable<?>)maxes.get(LEFT_NEIGHBOR_MAX, l), max) < 0) {
                maxes.put(LEFT_NEIGHBOR_MAX, l, max);
                if (compare((Comparable<?>)maxes.get(LEFT_NEIGHBOR_MAX, l), (Comparable<?>)maxes.get(LEFT_MAX, l + 1)) > 0) {
                    maxes.put(LEFT_MAX, l + 1, maxes.get(LEFT_NEIGHBOR_MAX, l));
                    try {
                        neighbors.get(R, l + 1).send(updateLeftNeighborMaxOp(l + 1, (Comparable<?>)maxes.get(LEFT_MAX, l + 1), null));
                    } catch (IOException e) {
                    }
                }
                else {
                    changeFlag = false;
                }
                l++;
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
        else {
            super.onReceive(sender, mes);
        }
    }
    
    public String toString() {
        return "[" + getKey() + "," + getRangeEnd() + "]\n" + neighbors.toString() + maxes.toString();
    }
}
