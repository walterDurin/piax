package org.piax.ov.ovs.skipgraph;

import java.io.IOException;
import java.util.Map;

import org.piax.ov.common.KeyComparator;
import org.piax.trans.Message;
import org.piax.trans.ResponseChecker;
import org.piax.trans.SimTransport;
import org.piax.trans.common.Id;

import static org.piax.trans.Literals.map;

public class SkipGraph {
public    Comparable<?> key;   // Key
    Id id;               // Node Id
public    MembershipVector m;  // Membership vector
    SimTransport trans;

    public NeighborTable neighbors;
    public boolean deleteFlag;

    static public enum Op {
        SEARCH, FOUND, NOT_FOUND, NEIGHBOR, BUDDY, GET_LINK, SET_LINK,
        DELETE, CONFIRM_DELETE, NO_NEIGHBOR, SET_NEIGHBOR_NIL,
        FIND_NEIGHBOR, FOUND_NEIGHBOR
    }
    
    static public enum Arg {
        OP, SIDE, OTHER_SIDE, SIDE_NEIGHBOR_ID, ID, KEY, LEVEL,
        OTHER_SIDE_NEIGHBOR_ID, NEW_NEIGHBOR_ID, NEW_NEIGHBOR_KEY, VAL,
        FOUND_ID, SENDER_ID, FOUND_KEY, SENDER_KEY
    }
    
    static final public int R = 0;
    static final public int L = 1;
    
    class SearchResult {
        public Op result;
        public Id foundId;
    }

    private SearchResult searchResult;
    
    class CheckOp implements ResponseChecker {
        Id id;
        Op op;
        Op op2;
        public CheckOp(Id id, Op op) {
            this.id = id;
            this.op = op;
            this.op2 = null;
        }
        
        public CheckOp(Id id, Op op, Op op2) {
            this.id = id;
            this.op = op;
            this.op2 = op2;
        }
        @Override
        public boolean isWaitingFor(Message message) {
            if (op2 == null) {
                return (message.to.equals(id) && message.args.get(Arg.OP) == op);
            }
            else {
                return (message.to.equals(id) && (message.args.get(Arg.OP) == op || message.args.get(Arg.OP) == op2));
            }
        }
        
    }
     
    public SkipGraph(SimTransport trans, Id id, Comparable<?> key) {
        this.trans = trans;
        this.id = id;
        this.key = key;
        this.m = new MembershipVector();
        neighbors = new NeighborTable(key);
    }

    // Send/Receive

    public Comparable<?> getKey() {
        return key;
    }

    public Id getNeighborOp(int side, int l) {
        Id nid = neighbors.get(side, l);
        if (nid == null) {
            return id; 
        }
        else {
            return nid;
        }
    }

    public Map<Arg,Object> sendAndWait(Map<Arg,Object> message, Id to, Op op) throws IOException {
        Message ret = trans.sendAndWait(new Message(to, id, message), new CheckOp(id, op));
        return ret.args;
    }
    
    public Map<Arg,Object> sendAndWait(Map<Arg,Object> message, Id to, Op op, Op op2) throws IOException {
        Message ret = trans.sendAndWait(new Message(to, id, message), new CheckOp(id, op, op2));
        return ret.args;
    }
    
    public void onReceiveSearchResult(Message mes) {
        Map<Arg,Object> arg = mes.args;
        searchResult.result = (Op) arg.get(Arg.OP);
        System.out.println("search hops =" + mes.via.size());
        if (searchResult.result == Op.FOUND){
            searchResult.foundId = (Id) arg.get(Arg.FOUND_ID);
        }
        else {
            searchResult.foundId = (Id) arg.get(Arg.OTHER_SIDE_NEIGHBOR_ID);
        }
        synchronized(searchResult) {
            searchResult.notify();
        }
    }

    public void onReceive(Message mes) {
        Map<Arg,Object> message = mes.args;
        Op op = (Op)message.get(Arg.OP);
        if (op == Op.SEARCH) {
            onReceiveSearchOp(mes);
        }
        else if (op == Op.BUDDY) {
            onReceiveBuddyOp(mes);
        }
        else if (op == Op.FOUND){
            onReceiveSearchResult(mes);
        }
        else if (op == Op.GET_LINK){
            onReceiveGetLinkOp(mes);
        }
        else if (op == Op.NOT_FOUND) {
            onReceiveSearchResult(mes);
        }
        else if (op == Op.DELETE) {
            onReceiveDeleteOp(mes);
        }
        else if (op == Op.FIND_NEIGHBOR) {
            onReceiveFindNeighborOp(mes);
        }
        else if (op == Op.SET_NEIGHBOR_NIL) {
            onReceiveSetNeighborNilOp(mes);
        }
    }

    Message updateMessage(Message received, Map a, Id to) {
        return new Message(to, id, a, received.via);
    }
    
    Message newMessage(Map a, Id to) {
        return new Message(to, id, a);
    }

    int compare(Comparable<?> a, Comparable<?>b) {
        return KeyComparator.getInstance().compare(a, b);
    }

    private Map<Arg,Object> searchOp(Id id, Comparable<?> key, int level) {
        return map(Arg.OP, (Object)Op.SEARCH).map(Arg.ID, id).map(Arg.KEY, key).map(Arg.LEVEL, level);
    }
    
    private Map<Arg,Object> foundOp(Id id) {
        return map(Arg.OP, (Object)Op.FOUND).map(Arg.FOUND_ID, id);
    }

    private Map<Arg,Object> notFoundOp(Id id) {
        return map(Arg.OP, (Object)Op.NOT_FOUND).map(Arg.OTHER_SIDE_NEIGHBOR_ID, id);
    }

    private Map<Arg,Object> getLinkOp(Id u, int side, int level) {
        return map(Arg.OP, (Object)Op.GET_LINK).map(Arg.ID, u).map(Arg.SIDE, side).map(Arg.LEVEL, level);
    }

    private Map<Arg,Object> setLinkOp(Id newNeighborId, Comparable<?> newNeighborKey, int level) {
        return map(Arg.OP, (Object)Op.SET_LINK).map(Arg.NEW_NEIGHBOR_ID,newNeighborId).map(Arg.NEW_NEIGHBOR_KEY,newNeighborKey).map(Arg.LEVEL,level);
    }

    private Map<Arg,Object> buddyOp(Id id, int level, MembershipVector m, int side) {
        return map(Arg.OP, (Object)Op.BUDDY).map(Arg.ID, id).map(Arg.LEVEL,level).map(Arg.VAL, m).map(Arg.SIDE, side);
    }
    
    private Map<Arg,Object> deleteOp(int level, Id senderId, Comparable<?> senderKey) {
        return map(Arg.OP, (Object)Op.DELETE).map(Arg.LEVEL, level).map(Arg.SENDER_ID, senderId).map(Arg.SENDER_KEY, senderKey);
    }
    
    private Map<Arg,Object> confirmDeleteOp(int level) {
        return map(Arg.OP, (Object)Op.CONFIRM_DELETE).map(Arg.LEVEL, level);
    }
    
    private Map<Arg,Object> noNeighborOp(int level) {
        return map(Arg.OP, (Object)Op.NO_NEIGHBOR).map(Arg.LEVEL, level);
    }
    
    private Map<Arg,Object> setNeighborNilOp(int level, Id senderId) {
        return map(Arg.OP, (Object)Op.SET_NEIGHBOR_NIL).map(Arg.LEVEL, level).map(Arg.SENDER_ID, senderId);
    }
    
    private Map<Arg,Object> findNeighborOp(int level, Id senderId, Comparable<?> senderKey) {
        return map(Arg.OP, (Object)Op.FIND_NEIGHBOR).map(Arg.LEVEL, level).map(Arg.SENDER_ID, senderId).map(Arg.SENDER_KEY, senderKey);
    }
    
    private Map<Arg,Object> foundNeighborOp(Id foundId, Comparable<?> foundKey, int level) {
        return map(Arg.OP, (Object)Op.FOUND_NEIGHBOR).map(Arg.LEVEL, level).map(Arg.FOUND_ID, foundId).map(Arg.FOUND_KEY, foundKey);
    }

    public int getMaxLevel() {
        return neighbors.skipTable.size() - 1;
    }
    
    public void onReceiveGetLinkOp(Message mes) {
        Map<Arg, Object> args = mes.args;
        Id u = (Id) args.get(Arg.ID);
        int side = (int)((Integer)args.get(Arg.SIDE));
        int l = (int)((Integer)args.get(Arg.LEVEL));
        change_neighbor(u, side, l);
    }

    // Most of l + 1 were l in the original paper.
    public void onReceiveBuddyOp(Message mes) {
        int otherSide;
        
        Map<Arg, Object> args = mes.args;
        Id u = (Id) args.get(Arg.ID);
        int l = (int)((Integer)args.get(Arg.LEVEL));
        MembershipVector val = (MembershipVector) args.get(Arg.VAL);
        int side = (int)((Integer)args.get(Arg.SIDE));
        if (side == L) {
            otherSide = R;
        }
        else {
            otherSide = L;
        }
        if (!m.existsElementAt(l + 1)) {
            m.randomElement(l + 1);
            neighbors.put(L, l + 1, null, null);
            neighbors.put(R, l + 1, null, null);
        }
        if (m.equals(l + 1, val)) {
            change_neighbor(u, side, l + 1);
        }
        else {
            if (neighbors.get(otherSide, l) != null) {
                trans.send(updateMessage(mes, buddyOp(u, l, val, side), neighbors.get(otherSide, l)));
            }
            else {
                trans.send(updateMessage(mes, setLinkOp(null, null, l + 1), u));
            }
        }
    }
    
    private void change_neighbor(Id u, int side, int l) {
        Comparable<?> sideKey = neighbors.getKey(side, l);
        Comparable<?> uKey = trans.getKey(u);
        int cmp = 0;
        if (sideKey != null) {
            if (side == R) {
                cmp = compare(sideKey, uKey);
            }
            else {
                cmp = compare(uKey, sideKey);
            }
        }
        Id to;
        Map<Arg,Object> arg;
        if (cmp < 0) {
            to = neighbors.get(side, l);
            arg = getLinkOp(u, side, l);
        }
        else {
            to = u;
            arg = setLinkOp(id, key, l);
        }
        trans.send(new Message(to, id, arg));
        neighbors.put(side, l, u, uKey);
    }

    public void onReceiveSearchOp(Message mes) {
        Map<Arg, Object> args = mes.args;
        Id startNodeId = (Id) args.get(Arg.ID);
        Comparable<?> searchKey = (Comparable<?>) args.get(Arg.KEY);
        int level = (int)((Integer)args.get(Arg.LEVEL));

        if (compare(key,searchKey) == 0) {
            trans.send(updateMessage(mes, foundOp(id), startNodeId));
        }
        else if (compare(key, searchKey) < 0) {
            while (level >= 0) {
                Comparable<?> rightKey = neighbors.getKey(R, level);
                if (rightKey != null && compare(rightKey, searchKey) <= 0) {
                    System.out.println(String.format("R: KEY:%4d, LEVEL:%2d, MV:%s", key, level, m.toString()));
                    trans.send(updateMessage(mes, searchOp(startNodeId, searchKey, level), neighbors.get(R, level)));
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
                if (leftKey != null && compare(leftKey, searchKey) >= 0) {
                    System.out.println(String.format("L: KEY:%4d, LEVEL:%2d, MV:%s", key, level, m.toString()));
                    trans.send(updateMessage(mes, searchOp(startNodeId, searchKey, level), neighbors.get(L, level)));
                    break;
                }
                else {
                    level--;
                }
            }
        }
        if (level < 0) {
            trans.send(updateMessage(mes, notFoundOp(id), startNodeId));
        }
    }

    private void onReceiveDeleteOp(Message mes) {
        Map<Arg, Object> args = mes.args;
        int l = (int)((Integer)args.get(Arg.LEVEL));
        Id senderId = (Id)args.get(Arg.SENDER_ID);
        Comparable<?> senderKey = (Comparable<?>)args.get(Arg.SENDER_KEY);
        try {
            if (deleteFlag) {
                if (neighbors.get(R, l) != null) {
                    trans.send(updateMessage(mes, deleteOp(l, senderId, senderKey), neighbors.get(R, l)));
                }
                else {
                    trans.send(newMessage(noNeighborOp(l), senderId));
                }
            }
            else {
                if (neighbors.get(L, l) != null) {
                    // original paper claims this is findNeighborOp(l, senderId, senderKey) but foundNeighborOp never arrives to this node.
                    Map<Arg, Object> sr = sendAndWait(findNeighborOp(l, id, key), neighbors.get(L, l), Op.FOUND_NEIGHBOR);
                    Id xId = (Id) sr.get(Arg.FOUND_ID);
                    Comparable<?> xKey = (Comparable<?>) sr.get(Arg.FOUND_KEY);
                    int level = (int)((Integer) sr.get(Arg.LEVEL));
                    System.out.println("update " + key + "'s L :" + neighbors.getKey(L, level) + "-(" + level + ")->" + xKey);
                    neighbors.put(L, level, xId, xKey);
                    trans.send(newMessage(confirmDeleteOp(l), senderId));
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

    private void onReceiveFindNeighborOp(Message mes) {
        Map<Arg, Object> args = mes.args;
        int l = (int)((Integer)args.get(Arg.LEVEL));
        Id senderId = (Id)args.get(Arg.SENDER_ID);
        Comparable<?> senderKey = (Comparable<?>) args.get(Arg.SENDER_KEY);
        if (deleteFlag) {
            if (neighbors.get(L, l) != null) {
                trans.send(updateMessage(mes, findNeighborOp(l, senderId, senderKey), neighbors.get(L, l)));
            }
            else {
                trans.send(updateMessage(mes, foundNeighborOp(null, null, l), senderId));
            }
        }
        else {
            trans.send(updateMessage(mes, foundNeighborOp(id, key, l), senderId));
            System.out.println("update " + key + "'s R :" + neighbors.getKey(R, l) + "-(" + l + ")->" + senderKey);
            neighbors.put(R, l, senderId, senderKey);
        }
    }
    
    private void onReceiveSetNeighborNilOp(Message mes) {
        Map<Arg, Object> args = mes.args;
        int l = (int)((Integer)args.get(Arg.LEVEL));
        Id senderId = (Id)args.get(Arg.SENDER_ID);
        if (deleteFlag) {
            if (neighbors.get(L, l) != null) {
                trans.send(newMessage(setNeighborNilOp(l, senderId), neighbors.get(L, l)));
            }
            else {
                trans.send(newMessage(confirmDeleteOp(l), senderId));
            }
        }
        else {
            trans.send(newMessage(confirmDeleteOp(l), senderId));
            neighbors.put(R, l, null, null);
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
    
    // skip graph protocol (search/insert/delete)
    public Id search(Comparable<?> key) {
        trans.nestedWait = false; // for better performance
        searchResult = new SearchResult();
        onReceiveSearchOp(new Message(id, id, searchOp(id, key, getMaxLevel())));
        synchronized(searchResult) {
            try {
                searchResult.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (searchResult.result == Op.FOUND) {
            return searchResult.foundId;
        }
        return null;
    }
    
    public void insert(Id introducerId) {
        int side, otherSide;
        
        trans.nestedWait = false; // for better performance
        if (introducerId.equals(id)) {
            neighbors.put(L, 0, null, null);
            neighbors.put(R, 0, null, null);
            //maxLevel = 0;
        }
        else {
            Comparable<?> introducerKey = trans.getKey(introducerId); 
            if (compare(introducerKey, key) < 0) {
                side = R;
                otherSide = L;
            }
            else {
                side = L;
                otherSide = R;
            }
            try {
                int maxLevel = trans.getMaxLevelOp(introducerId);
                Map<Arg, Object> sr = sendAndWait(searchOp(id, key, maxLevel - 1), introducerId, Op.NOT_FOUND);
                Id otherSideNeighborId = (Id)sr.get(Arg.OTHER_SIDE_NEIGHBOR_ID);
                Id sideNeighborId = trans.getNeighborOp(otherSideNeighborId, side, 0);
                if (otherSideNeighborId.equals(sideNeighborId)) {
                    sideNeighborId = null;
                }
                if (otherSideNeighborId != null) {
                    Map<Arg, Object> r = sendAndWait(getLinkOp(id, side, 0), otherSideNeighborId, Op.SET_LINK);
                    Id newNeighborId = (Id) r.get(Arg.NEW_NEIGHBOR_ID);
                    Comparable<?> newNeighborKey = (Comparable<?>) r.get(Arg.NEW_NEIGHBOR_KEY);
                    int newLevel = (int)((Integer)r.get(Arg.LEVEL));
                    neighbors.put(otherSide, newLevel, newNeighborId, newNeighborKey);
                }
                if (sideNeighborId != null) {
                    Map<Arg, Object> r = sendAndWait(getLinkOp(id, otherSide, 0), sideNeighborId, Op.SET_LINK);
                    Id newNeighborId = (Id) r.get(Arg.NEW_NEIGHBOR_ID);
                    Comparable<?> newNeighborKey = (Comparable<?>) r.get(Arg.NEW_NEIGHBOR_KEY);
                    int newLevel = (int)((Integer)r.get(Arg.LEVEL));
                    neighbors.put(side, newLevel, newNeighborId, newNeighborKey);
                }
                int l = 0;
                while (true) {
                    l++;
                    m.randomElement(l);
                    if (neighbors.get(R, l - 1) != null) {
                        Map<Arg, Object> r = sendAndWait(buddyOp(id, l - 1, m, L), neighbors.get(R, l - 1), Op.SET_LINK);
                        Id newNeighborId = (Id) r.get(Arg.NEW_NEIGHBOR_ID);
                        Comparable<?> newNeighborKey =  (Comparable<?>) r.get(Arg.NEW_NEIGHBOR_KEY);
                        neighbors.put(R, l, newNeighborId, newNeighborKey);
                    }
                    else {
                        neighbors.put(R, l, null, null);
                    }
                    if (neighbors.get(L, l - 1) != null) {
                        Map<Arg,Object> r = sendAndWait(buddyOp(id, l - 1, m, R), neighbors.get(L, l - 1), Op.SET_LINK);
                        Id newNeighborId = (Id) r.get(Arg.NEW_NEIGHBOR_ID);
                        Comparable<?> newNeighborKey =  (Comparable<?>) r.get(Arg.NEW_NEIGHBOR_KEY);
                        neighbors.put(L, l, newNeighborId, newNeighborKey);
                    }
                    else {
                        neighbors.put(L, l, null, null);
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
        trans.nestedWait = true; // for correct behavior
        deleteFlag = true;
        try {
            for (int l = getMaxLevel(); l >= 0; l--) {
                if (neighbors.get(R, l) != null) {
                    Map<Arg, Object> r = sendAndWait(deleteOp(l, id, key), neighbors.get(R, l), Op.CONFIRM_DELETE, Op.NO_NEIGHBOR);
                    Op op = (Op) r.get(Arg.OP);
                    if (op == Op.CONFIRM_DELETE) {
                        // finish this level;
                    }
                    else if (op == Op.NO_NEIGHBOR) {
                        if (neighbors.get(L, l) != null) {
                            sendAndWait(setNeighborNilOp(l, id), neighbors.get(L, l), Op.CONFIRM_DELETE);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
