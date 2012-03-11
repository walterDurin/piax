package org.piax.ov.ovs.dtn;

import java.io.IOException;
import java.util.Date;
import java.util.List;


import org.json.JSONArray;
import org.piax.gnt.ProtocolUnsupportedException;
import org.piax.gnt.Target;
import org.piax.gnt.handover.Peer;
import org.piax.gnt.handover.PeerStateDelegate;
import org.piax.ov.jmes.Command;
import org.piax.ov.jmes.CommandReceiveListener;
import org.piax.ov.jmes.Message;
import org.piax.ov.jmes.MessageData;


import org.piax.trans.common.PeerId;
import org.piax.trans.common.PeerLocator;

public abstract class DTNAlgorithm implements CommandReceiveListener, PeerStateDelegate {

    DTN dtn;
    String peerIdString;
    PeerId peerId;
    PeerStateDelegate delegate;
    
    public void attachDTN(DTN dtn) {
        this.dtn = dtn;
        dtn.addCommandReceiveListener(this);
        dtn.setPeerStateDelegate(this);
        peerId = dtn.getPeerId();
        peerIdString = peerId.toString();
    }
    public abstract void onReceiveCommand(Peer peer, PeerLocator locator, Command com);
    public abstract void newMessage(Message mes);
    
    public abstract void onPeerStateChange(Peer peer, PeerLocator locator, int state);
    public abstract boolean onAccepting(Peer peer, PeerLocator locator);
    
    // protocol utility interfaces.
    protected String peerIdString() {
        return peerIdString;
    }
    protected PeerId peerId() {
        return peerId;
    }
    protected String peerName() {
        return dtn.getName();
    }
    // 
    protected void newLink(Peer peer, PeerLocator locator) {
        dtn.newLink(peer, locator);
    }

    protected void peerStateChanged(Peer peer, PeerLocator locator, int state) {
        dtn.onPeerStateChange(peer, locator, state);
    }
    
    protected MessageData encodeMessage(Message m) {
        return dtn.encode(m);
    }
    
    protected Message decodeMessageData(MessageData md) {
        Message mes = dtn.decode(md);
        if (mes.isValid()) {
            md.von_id = mes.von_id;
        }
        else {
            md.von_id = null;
        }
        return mes;
    }
    
    protected void receiveMessage(Message mes) {
        dtn.receiveMessage(mes);
    }
    
    protected JSONArray array2jarray(List<?> list) {
        JSONArray arr = new JSONArray();
        for (Object o : list) {
            arr.put(o);
        }
        return arr;
    }
    
    protected void sendCommand(Peer dst, Command com) throws ProtocolUnsupportedException, IOException {
        dtn.sendCommand(dst, com);
    }
    
    protected List<Peer> listPeers() {
        return dtn.getNodes();
    }
    
    protected boolean storeMessage(MessageData md) {
        return (dtn.getDB().storeMessage(md) > 0);
    }
    
    protected MessageData getMessageData(String mid) {
        return dtn.getDB().fetchMessage(mid);
    }

    protected boolean hasMessage(String mid) {
        return dtn.hasMessage(mid);
    }
    
    protected boolean removeMessage(String mid) {
        return dtn.getDB().removeMessage(mid);
    }
    
    protected List<String> listMessageIds(int limit) {
        return dtn.getDB().getAllMessageIdArray(limit);
    }
    
    protected List<String> listMessageIds(Date expirationTime, int limit) {
        return dtn.getDB().getMessageIdArray(expirationTime, limit);
    }
        
    protected long getNrofMessages() {
        return dtn.getDB().countMessages();
    }
}
