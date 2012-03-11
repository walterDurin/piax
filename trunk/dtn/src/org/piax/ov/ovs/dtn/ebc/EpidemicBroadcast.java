package org.piax.ov.ovs.dtn.ebc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.piax.gnt.handover.Peer;
import org.piax.ov.jmes.Command;
import org.piax.ov.jmes.Message;
import org.piax.ov.jmes.MessageData;

import org.piax.ov.ovs.dtn.DTNAlgorithm;
import org.piax.trans.common.PeerLocator;
import org.piax.trans.msgframe.MessageReachable;

public class EpidemicBroadcast extends DTNAlgorithm {
    static final int SYNC_MESSAGE_LIMIT = 20;
    static final int LINK_LIMIT = 7;
    
    List<String> getting;    

    public EpidemicBroadcast() {
        getting = new ArrayList<String>();
    }
    
    private void send_available(Peer peer) {
        List<String> arr = listMessageIds(new Date(), SYNC_MESSAGE_LIMIT);
        try {
            sendCommand(peer, new Command("AVAILABLE", Command.PLAIN_TYPE, peerIdString(), array2jarray(arr)));
            peer.syncDone();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void send_get(Peer peer, ArrayList<String> ids) {
        if (ids.size() == 0) {
            return;
        }
        try {
            synchronized(getting) {
                getting.clear();
                getting.addAll(ids);
            }
            sendCommand(peer, new Command("GET", Command.PLAIN_TYPE, peerIdString(), array2jarray(ids)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void send_ping(Peer peer) {
        JSONArray arr = new JSONArray();
        arr.put(peerName());
        try {
            sendCommand(peer, new Command("PING", Command.PLAIN_TYPE, peerIdString(), arr));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void send_push(Peer peer, JSONArray ids) {
        ArrayList<MessageData> arr = new ArrayList<MessageData>();
        for (int i = 0; i < ids.length(); i++) {
            MessageData md = null;
            try {
                md = getMessageData((String)ids.get(i));
                arr.add(md);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        send_push(peer, arr);
    }    

    private void send_push(Peer peer, List<MessageData> mdList) {
        JSONArray arr = new JSONArray();
        for (MessageData md : mdList) {
            arr.put(md.toString());
        }
        try {        
            if (arr.length() != 0) {
                sendCommand(peer, new Command("PUSH", Command.MESSAGE_TYPE, peerIdString(), arr));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }        
    }
    
    private boolean canLinkMoreNode() {
        int count = 0;
        for (Peer peer : listPeers()) {
            if (peer.getStatus() > Peer.TransportStateCommunicatable)
                count++;
        }
        return (count <= LINK_LIMIT - 1);
    }

    public void onPeerStateChange(Peer peer, PeerLocator locator, int state) {
        switch (state) {
        case Peer.TransportStateAvailable:
        case Peer.TransportStateAlive:
            if (peerId().equals(peer.peerId)) {
                // myself is found. ignore.
            }
            else {
                if ((peer.getStatus() == Peer.TransportStateLinking) || 
                    (peer.getStatus() == Peer.TransportStateLinked) ||
                    (peer.getStatus() == Peer.TransportStateAccepted) ||
                    (peer.getStatus() == Peer.TransportStateLinkRefused)) {
                    // already linking or linked or refused
                }
                else {
                    if (canLinkMoreNode()) {
                        System.out.println("LINKING since state=" + peer.getStatus());
                        newLink(peer, locator);
                        System.out.println("LINKING END");
                    }
                }
            }
            break;
        case Peer.TransportStateUnavailable:
        case Peer.TransportStateLinkRefused:
            break;
        case Peer.TransportStateAccepted:
        case Peer.TransportStateLinked:
            send_available(peer);
            break;
        }
    }

    public class PingRunner implements Runnable {
        public PingRunner() {
        }
        public void run() {
            for(Peer peer : listPeers()) {
                if (peer.getStatus() >= Peer.TransportStateCommunicatable) {
                    send_ping(peer);
                }
            }
        }
    }
    
    private void onReceivePINGCommand(Peer peer, PeerLocator locator, Command command) {
        JSONArray arr = (JSONArray)command.argument;
        try {
            String name = (String)arr.get(0);
            peer.name = name;
            peerStateChanged(peer, locator, Peer.TransportStateAlive);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void onReceiveGETCommand(Peer peer, PeerLocator locator, Command command) {
        send_push(peer, (JSONArray) command.argument);
    }

    private void onReceiveAVAILABLECommand(Peer peer, PeerLocator locator, Command command) {
        JSONArray avail = (JSONArray) command.argument;
        ArrayList<String> missing = new ArrayList<String>();
        if (avail != null) {
            List<String> existing = listMessageIds(new Date(), SYNC_MESSAGE_LIMIT);
            synchronized(getting) {
                // XXX remove old entries from 'getting'
                for (String gid : getting) {
                    existing.add(gid);
                }
            }
            for (int i = 0; i < avail.length(); i++) {
                boolean found = false;
                try {
                    String cid = (String)avail.get(i);
                    for (String eid : existing) {
                        if (cid.equals(eid)) {
                            found = true;
                            break;
                        }
                    }
                    // not found in available AND
                    // don' t have the message
                    if (!found && !hasMessage(cid)) {
                        missing.add(0, cid);
                    }
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        // System.out.println("Need to get " + missing.size() + " messages");
        send_get(peer, missing);
    }
    
    private void onReceivePUSHCommand(Peer peer, PeerLocator locator, Command command) {
        String locStr = viaLocatorString(locator);
        ArrayList<MessageData> mList = (ArrayList<MessageData>)command.argument;
        // update all message properties.
        for (MessageData md : mList) {
            md.ttl = md.ttl > 0 ? md.ttl - 1 : 0;
            if (md.via == null) {
                md.via = new ArrayList<String>();
            }
            if (locStr != null) {
                md.via.add(locStr);
            }
            md.via.add(peerIdString());
            md.received_at = new Date();

            // XXX should consider timing of removal.
            synchronized(getting) {
                getting.remove(md.id);
            }
            deliverMessageDataToSelf(md);
        }
        forward(mList);
    }
    
    private void deliverMessageDataToSelf(MessageData md) {
        // XXX side effect on argument message data.
        // decodeMessageData should be called before storeMessage.
        Message mes = decodeMessageData(md);
        if (storeMessage(md) && mes.isValid()) {
            if (md.recipient_id != null) {
                if (md.recipient_id.equals(peerIdString())) {
                    // receive direct message.
                    receiveMessage(mes);
                }
            }
            else {
                // recipient is null=broadcast. receive.
                receiveMessage(mes);
            }
        }
    }
    
    private void forward(List<MessageData> mdList) {
        for (Peer peer : listPeers()) {
            if (peer.getStatus() > Peer.TransportStateCommunicatable) {
                if (peer.needSync()) {
                    send_available(peer);
                }
                else {
                    ArrayList<MessageData> sending = new ArrayList<MessageData>();
                    for (MessageData md : mdList) {
                        if (!(md.via != null && md.via.contains(peer.peerIdString))) {
                            sending.add(md);
                        }
                    }
                    send_push(peer, sending);
                }
            }
        }        
    }

    public void onReceiveCommand(Peer peer, PeerLocator locator, Command com) {
        System.out.println("Received:" + com.toString());
        if (com.commandString.equals("AVAILABLE")) {
            onReceiveAVAILABLECommand(peer, locator, com);
        }
        else if (com.commandString.equals("PUSH")) {
            onReceivePUSHCommand(peer, locator, com);
        }
        else if (com.commandString.equals("GET")) {
            onReceiveGETCommand(peer, locator, com);
        }
        else if (com.commandString.equals("PING")) {
            onReceivePINGCommand(peer, locator, com);
        }
        else {
            System.out.println("Received unspported command.");
        }
    }
    
    private String viaLocatorString(MessageReachable locator) {
        long time = new Date().getTime();
        if (locator instanceof org.piax.trans.ts.nfc.NfcLocator) {
            return "n" + time;
        }
        else if (locator instanceof org.piax.trans.ts.bluetooth.BluetoothLocator) {
            return "b" + time;
        }
        else if (locator instanceof org.piax.trans.ts.InetLocator) {
            return "i" + time;
        }
        return null;
    }

    @Override
    public void onReceiveCommand(Command com) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean onAccepting(Peer peer, PeerLocator locator) {
        return canLinkMoreNode();
    }

    @Override
    public void newMessage(Message mes) {
        MessageData md = encodeMessage(mes);
        deliverMessageDataToSelf(md);
        ArrayList<MessageData> mList = new ArrayList<MessageData>();
        mList.add(md);
        forward(mList);
    }

}
