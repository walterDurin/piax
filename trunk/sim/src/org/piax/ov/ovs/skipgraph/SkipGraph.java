// Pure Skip Graph implementation.
package org.piax.ov.ovs.skipgraph;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.piax.ov.Overlay;
import org.piax.ov.OverlayManager;
import org.piax.ov.common.KeyComparator;
import org.piax.ov.common.Range;
import org.piax.trans.Node;
import org.piax.trans.ResponseChecker;
import org.piax.trans.common.Id;
import org.piax.trans.sim.SimTransportOracle;

import static org.piax.trans.Literals.map;

public class SkipGraph implements Overlay {

    public MembershipVector m;  // Membership vector
    public Node self;

    // For faster access;
    public Id id;               // Node Id
    public Comparable<?> key;   // Key
    
    //Transport trans;

    public NeighborTable neighbors;
    public boolean deleteFlag;

    static public enum Op {
        SEARCH, FOUND, NOT_FOUND, NEIGHBOR, BUDDY, 
        GET_MAX_LEVEL, RET_MAX_LEVEL,
        GET_NEIGHBOR, RET_NEIGHBOR,
        GET_LINK, SET_LINK, 
        DELETE, CONFIRM_DELETE,
        NO_NEIGHBOR, SET_NEIGHBOR_NIL,
        FIND_NEIGHBOR, FOUND_NEIGHBOR
    }
    
    static public enum Arg {
        OP, SIDE, LEVEL, NODE, VAL
    }
    
    static final public int R = 0;
    static final public int L = 1;
    
    public class SearchResult {
        public Op result;
        public Node node;
    }

    private SearchResult searchResult;
    
    public class CheckOp implements ResponseChecker {
        Object op;
        Object op2;
        public CheckOp(Object op) {
            this.op = op;
            this.op2 = null;
        }
        
        public CheckOp(Object op, Object op2) {
            this.op = op;
            this.op2 = op2;
        }
        @Override
        public boolean isWaitingFor(Map<Object,Object> mes) {
            if (op2 == null) {
                return mes.get(Arg.OP) == op;
            }
            else {
                return (mes.get(Arg.OP) == op || mes.get(Arg.OP) == op2);
            }
        }
    }
     
    public SkipGraph(Node self) {
        this.self = self;
        this.id = self.getId();
        this.key = (Comparable<?>)self.getAttr(OverlayManager.KEY);
        this.m = new MembershipVector();
        neighbors = new NeighborTable(key);
    }

    // Send/Receive

    public Comparable<?> getKey() {
        return key;
    }
    
    public Comparable<?> getKey(Node n) {
        return (Comparable<?>)n.getAttr(OverlayManager.KEY);
    }

    public Node getNeighbor(int side, int l) {
        Node n = neighbors.get(side, l);
        if (n == null) {
            return self; 
        }
        else {
            return n;
        }
    }

    @SuppressWarnings("unchecked")
    protected List<Id> getVia(Map<Object,Object>args) {
        return (List<Id>)args.get(Node.VIA);
    }
    
    protected Map<Object,Object> setVia(Map<Object,Object>args, List<Id> via) {
        args.put(Node.VIA, via);
        return args;
    }
    
    protected void onReceiveSearchResult(Node sender, Map<Object,Object> arg) {
        searchResult.result = (Op) arg.get(Arg.OP);
        List<Id> via = getVia(arg);
        System.out.println("search hops =" + (via == null ? 0 : via.size()));
        if (searchResult.result == Op.FOUND){
            searchResult.node = (Node) arg.get(Arg.NODE);
        }
        else {
            searchResult.node = (Node) arg.get(Arg.NODE);
        }
        synchronized(searchResult) {
            searchResult.notify();
        }
    }

    public void onReceive(Node sender, Map<Object,Object> mes) {
        Op op = (Op)mes.get(Arg.OP);
        if (op == Op.SEARCH) {
            onReceiveSearchOp(sender, mes);
        }
        else if (op == Op.BUDDY) {
            onReceiveBuddyOp(sender, mes);
        }
        else if (op == Op.FOUND){
            onReceiveSearchResult(sender, mes);
        }
        else if (op == Op.GET_LINK){
            onReceiveGetLinkOp(sender, mes);
        }
        else if (op == Op.NOT_FOUND) {
            onReceiveSearchResult(sender, mes);
        }
        else if (op == Op.DELETE) {
            onReceiveDeleteOp(sender, mes);
        }
        else if (op == Op.FIND_NEIGHBOR) {
            onReceiveFindNeighborOp(sender, mes);
        }
        else if (op == Op.SET_NEIGHBOR_NIL) {
            onReceiveSetNeighborNilOp(sender, mes);
        }
        else if (op == Op.GET_MAX_LEVEL) {
            onReceiveGetMaxLevelOp(sender, mes);
        }
        else if (op == Op.GET_NEIGHBOR) {
            onReceiveGetNeighborOp(sender, mes);
        }
    }

    Map<Object,Object> inheritVia(Map<Object,Object> message, List<Id> via) {
        setVia(message, via);
        return message;
    }
    
    int compare(Comparable<?> a, Comparable<?>b) {
        return KeyComparator.getInstance().compare(a, b);
    }

    protected Map<Object,Object> searchOp(Node startNode, Comparable<?> searchKey, int level) {
        return map((Object)Arg.OP, (Object)Op.SEARCH).map(Arg.NODE, startNode).map(OverlayManager.KEY, searchKey).map(Arg.LEVEL, level);
    }
    
    private Map<Object,Object> foundOp(Node v) {
        return map((Object)Arg.OP, (Object)Op.FOUND).map(Arg.NODE, v);
    }

    protected Map<Object,Object> notFoundOp(Node v) {
        return map((Object)Arg.OP, (Object)Op.NOT_FOUND).map(Arg.NODE, v);
    }
    
    protected Map<Object,Object> getMaxLevelOp() {
        return map((Object)Arg.OP, (Object)Op.GET_MAX_LEVEL);
    }
    
    protected Map<Object,Object> retMaxLevelOp(int level) {
        return map((Object)Arg.OP, (Object)Op.RET_MAX_LEVEL).map(Arg.LEVEL, level);
    }
    
    protected Map<Object,Object> getNeighborOp(int side, int level) {
        return map((Object)Arg.OP, (Object)Op.GET_NEIGHBOR).map(Arg.SIDE, side).map(Arg.LEVEL, level);
    }
    
    protected Map<Object,Object> retNeighborOp(Node vsidel) {
        return map((Object)Arg.OP, (Object)Op.RET_NEIGHBOR).map(Arg.NODE, vsidel);
    }

    protected Map<Object,Object> getLinkOp(Node u, int side, int level) {
        return map((Object)Arg.OP, (Object)Op.GET_LINK).map(Arg.NODE, u).map(Arg.SIDE, side).map(Arg.LEVEL, level);
    }

    protected Map<Object,Object> setLinkOp(Node node, int level) {
        return map((Object)Arg.OP, (Object)Op.SET_LINK).map(Arg.NODE, node).map(Arg.LEVEL,level);
    }

    protected Map<Object,Object> buddyOp(Node node, int level, MembershipVector m, int side) {
        return map((Object)Arg.OP, (Object)Op.BUDDY).map(Arg.NODE, node).map(Arg.LEVEL,level).map(Arg.VAL, m).map(Arg.SIDE, side);
    }
    
    protected Map<Object,Object> deleteOp(int level, Node node) {
        return map((Object)Arg.OP, (Object)Op.DELETE).map(Arg.LEVEL, level).map(Arg.NODE, node);
    }
    
    protected Map<Object,Object> confirmDeleteOp(int level) {
        return map((Object)Arg.OP, (Object)Op.CONFIRM_DELETE).map(Arg.LEVEL, level);
    }
    
    protected Map<Object,Object> noNeighborOp(int level) {
        return map((Object)Arg.OP, (Object)Op.NO_NEIGHBOR).map(Arg.LEVEL, level);
    }
    
    protected Map<Object,Object> setNeighborNilOp(int level, Node node) {
        return map((Object)Arg.OP, (Object)Op.SET_NEIGHBOR_NIL).map(Arg.LEVEL, level).map(Arg.NODE, node);
    }
    
    protected Map<Object,Object> findNeighborOp(int level, Node node) {
        return map((Object)Arg.OP, (Object)Op.FIND_NEIGHBOR).map(Arg.LEVEL, level).map(Arg.NODE, node);
    }
    
    protected Map<Object,Object> foundNeighborOp(Node node, int level) {
        return map((Object)Arg.OP, (Object)Op.FOUND_NEIGHBOR).map(Arg.LEVEL, level).map(Arg.NODE, node);
    }
    
    public int getMaxLevel() {
        return neighbors.skipTable.size() - 1;
    }

    protected void onReceiveGetMaxLevelOp(Node sender, Map<Object, Object> args) {
        Node u = sender;
        try {
            u.send(retMaxLevelOp(getMaxLevel()));
        } catch (IOException e) {
            e.printStackTrace();
        } 
    }
    
    protected void onReceiveGetNeighborOp(Node sender, Map<Object, Object> args) {
        Node u = sender;
        int side = (Integer) args.get(Arg.SIDE);
        int l = (Integer) args.get(Arg.LEVEL);
        try {
            u.send(retNeighborOp(getNeighbor(side, l)));
        } catch (IOException e) {
            e.printStackTrace();
        } 
    }
    
    protected void onReceiveGetLinkOp(Node sender, Map<Object, Object> args) {
        Node u = (Node) args.get(Arg.NODE);
        int side = (int)((Integer)args.get(Arg.SIDE));
        int l = (int)((Integer)args.get(Arg.LEVEL));
        change_neighbor(u, side, l);
    }

    // Most of l + 1 were l in the original paper.
    protected void onReceiveBuddyOp(Node sender, Map<Object,Object> args) {
        int otherSide;
        
        Node u = (Node) args.get(Arg.NODE);
        int l = (int)((Integer)args.get(Arg.LEVEL));
        MembershipVector val = (MembershipVector) args.get(Arg.VAL);
        List<Id> via = getVia(args);
        
        int side = (int)((Integer)args.get(Arg.SIDE));
        if (side == L) {
            otherSide = R;
        }
        else {
            otherSide = L;
        }
        if (!m.existsElementAt(l + 1)) {
            m.randomElement(l + 1);
            neighbors.put(L, l + 1, null);
            neighbors.put(R, l + 1, null);
        }
        if (m.equals(l + 1, val)) {
            change_neighbor(u, side, l + 1);
        }
        else {
            try {
                if (neighbors.get(otherSide, l) != null) {
                    neighbors.get(otherSide, l).send(setVia(buddyOp(u, l, val, side), via));
                }
                else {
                    u.send(setVia(setLinkOp(null, l + 1), via));
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void change_neighbor(Node u, int side, int l) {
        Comparable<?> sideKey = neighbors.getKey(side, l);
        Comparable<?> uKey = getKey(u);
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
            arg = getLinkOp(u, side, l);
        }
        else {
            to = u;
            arg = setLinkOp(self, l);
        }
        try {
            to.send(arg);
            neighbors.put(side, l, u);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void onReceiveSearchOp(Node sender, Map<Object,Object> args) {
        Node startNode = (Node) args.get(Arg.NODE);
        Comparable<?> searchKey = (Comparable<?>) args.get(OverlayManager.KEY);
        int level = (int)((Integer)args.get(Arg.LEVEL));
        List<Id> via = getVia(args);
        try {
            if (compare(key,searchKey) == 0) {
                startNode.send(setVia(foundOp(self), via));
            }
            else if (compare(key, searchKey) < 0) {
                while (level >= 0) {
                    Comparable<?> rightKey = neighbors.getKey(R, level);
                    if (rightKey != null && compare(rightKey, searchKey) <= 0) {
                        //System.out.println(String.format("R: KEY:%4d, LEVEL:%2d, MV:%s", key, level, m.toString()));
                        neighbors.get(R, level).send(setVia(searchOp(startNode, searchKey, level), via));
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
                    if (leftKey != null && compare(searchKey, leftKey) <= 0) {
                        //System.out.println(String.format("L: KEY:%4s, LEVEL:%2d, MV:%s", key, level, m.toString()));
                        neighbors.get(L, level).send(setVia(searchOp(startNode, searchKey, level), via));
                        break;
                    }
                    else {
                        level--;
                    }
                }
            }
            if (level < 0) {
                startNode.send(setVia(notFoundOp(self), via));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void onReceiveDeleteOp(Node s, Map<Object,Object> args) {
        int l = (int)((Integer)args.get(Arg.LEVEL));
        Node sender = (Node)args.get(Arg.NODE);
        List<Id> via = getVia(args);
        try {
            if (deleteFlag) {
                if (neighbors.get(R, l) != null) {
                    neighbors.get(R, l).send(setVia(deleteOp(l, sender), via));
                }
                else {
                    sender.send(noNeighborOp(l));
                }
            }
            else {
                if (neighbors.get(L, l) != null) {
                    // original paper claims this is findNeighborOp(l, senderId, senderKey) but foundNeighborOp never arrives to this node.
                    Map<Object, Object> sr = neighbors.get(L, l).sendAndWait(findNeighborOp(l, self), new CheckOp(Op.FOUND_NEIGHBOR));
                    Node x = (Node) sr.get(Arg.NODE);
                    int level = (int)((Integer) sr.get(Arg.LEVEL));
                    System.out.println("update " + key + "'s L :" + neighbors.getKey(L, level) + "-(" + level + ")->" + x);
                    neighbors.put(L, level, x);
                    sender.send(confirmDeleteOp(l));
                }
                else {
                    throw new IOException("no left node.");
                }
            }
        }
        catch (Exception e) {
            // delete was failed.
            e.printStackTrace();
        }
        
    }

    protected void onReceiveFindNeighborOp(Node s, Map<Object,Object> args) {
        int l = (int)((Integer)args.get(Arg.LEVEL));
        Node sender = (Node)args.get(Arg.NODE);
        List<Id> via = getVia(args);
        try {
            if (deleteFlag) {
                if (neighbors.get(L, l) != null) {
                    neighbors.get(L, l).send(setVia(findNeighborOp(l, sender), via));
                }
                else {
                    sender.send(setVia(foundNeighborOp(null, l), via));
                }
            }
            else {
                sender.send(setVia(foundNeighborOp(self, l), via));
                System.out.println("update " + key + "'s R :" + neighbors.getKey(R, l) + "-(" + l + ")->" + sender);
                neighbors.put(R, l, sender);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    protected void onReceiveSetNeighborNilOp(Node s, Map<Object,Object> args) {
        int l = (int)((Integer)args.get(Arg.LEVEL));
        Node sender = (Node)args.get(Arg.NODE);
        try {
            if (deleteFlag) {
                if (neighbors.get(L, l) != null) {
                    neighbors.get(L, l).send(setNeighborNilOp(l, sender));
                }
                else {
                    sender.send(confirmDeleteOp(l));
                }
            }
            else {
                sender.send(confirmDeleteOp(l));
                neighbors.put(R, l, null);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkResponse(Map<Arg, Object> arg, Op op) throws IOException {
        if (arg == null) {
            throw new IOException("received null message");
        }
        if (arg.get(Arg.OP) != op) {
            throw new IOException("unexpected response");
        }
    }
    
    public String toString() {
        String s = "";
        s += "ID=" + id + (deleteFlag? "(DELETED)" : "") + "\n";
        s += "Key=" + getKey() + "\n";
        s += "MV=" + m.toString() + "\n";
        s += "Table:" + neighbors.toString() + "\n";
        return s;
    }
    
    
    // skip graph protocol (search/insert/delete)
    public Node search(Comparable<?> key) {
        self.trans.setParameter(SimTransportOracle.Param.NestedWait, Boolean.FALSE); // for better performance
        searchResult = new SearchResult();
        onReceiveSearchOp(self, searchOp(self, key, getMaxLevel()));
        synchronized(searchResult) {
            try {
                searchResult.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (searchResult.result == Op.FOUND) {
            return searchResult.node;
        }
        return null;
    }
    
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
                Map<Object,Object> mr = introducer.sendAndWait(getMaxLevelOp(), new CheckOp(Op.RET_MAX_LEVEL));
                int maxLevel = (Integer)mr.get(Arg.LEVEL);
                Map<Object, Object> sr = introducer.sendAndWait(searchOp(self, key, maxLevel - 1), new CheckOp(Op.NOT_FOUND, Op.FOUND));
                if (sr.get(Arg.OP) == Op.FOUND) {
                    System.out.println("FOUND.");
                    return;
                }
                Node otherSideNeighbor = (Node)sr.get(Arg.NODE);
                Map<Object, Object> nr = otherSideNeighbor.sendAndWait(getNeighborOp(side, 0), new CheckOp(Op.RET_NEIGHBOR));
                Node sideNeighbor = (Node)nr.get(Arg.NODE); 

                if (otherSideNeighbor.equals(sideNeighbor)) {
                    sideNeighbor = null;
                }
                if (otherSideNeighbor != null) {
                    Map<Object, Object> r = otherSideNeighbor.sendAndWait(getLinkOp(self, side, 0), new CheckOp(Op.SET_LINK));
                    Node newNeighbor = (Node) r.get(Arg.NODE);
                    int newLevel = (int)((Integer)r.get(Arg.LEVEL));
                    neighbors.put(otherSide, newLevel, newNeighbor);
                }
                if (sideNeighbor != null) {
                    Map<Object, Object> r = sideNeighbor.sendAndWait(getLinkOp(self, otherSide, 0), new CheckOp(Op.SET_LINK));
                    Node newNeighbor = (Node) r.get(Arg.NODE);
                    int newLevel = (int)((Integer)r.get(Arg.LEVEL));
                    neighbors.put(side, newLevel, newNeighbor);
                }
                int l = 0;
                while (true) {
                    l++;
                    m.randomElement(l);
                    if (neighbors.get(R, l - 1) != null) {
                        Map<Object, Object> r = neighbors.get(R, l - 1).sendAndWait(buddyOp(self, l - 1, m, L), new CheckOp(Op.SET_LINK));
                        Node newNeighbor = (Node) r.get(Arg.NODE);
                        neighbors.put(R, l, newNeighbor);
                    }
                    else {
                        neighbors.put(R, l, null);
                    }
                    if (neighbors.get(L, l - 1) != null) {
                        Map<Object,Object> r = neighbors.get(L, l - 1).sendAndWait(buddyOp(self, l - 1, m, R), new CheckOp(Op.SET_LINK));
                        Node newNeighbor = (Node) r.get(Arg.NODE);
                        neighbors.put(L, l, newNeighbor);
                    }
                    else {
                        neighbors.put(L, l, null);
                    }
                    if (neighbors.get(R, l) == null && neighbors.get(L, l) == null) break;
                }
                maxLevel = l;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void delete() {
        self.trans.setParameter(SimTransportOracle.Param.NestedWait, Boolean.TRUE); // for correct behavior
        deleteFlag = true;
        try {
            for (int l = getMaxLevel(); l >= 0; l--) {
                if (neighbors.get(R, l) != null) {
                    Map<Object, Object> r = neighbors.get(R, l).sendAndWait(deleteOp(l, self), new CheckOp(Op.CONFIRM_DELETE, Op.NO_NEIGHBOR));
                    Op op = (Op) r.get(Arg.OP);
                    if (op == Op.CONFIRM_DELETE) {
                        // finish this level;
                    }
                    else if (op == Op.NO_NEIGHBOR) {
                        if (neighbors.get(L, l) != null) {
                            neighbors.get(L, l).sendAndWait(setNeighborNilOp(l, self), new CheckOp(Op.CONFIRM_DELETE));
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Node> search(Range key) {
        // Not implemented in this class.
        return null;
    }
}
