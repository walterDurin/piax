package org.piax.gnt.handover;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.piax.ov.jmes.authz.SecureSession;
import org.piax.trans.common.PeerId;
import org.piax.trans.common.PeerLocator;
import org.piax.trans.stat.LocatorStat;
import org.piax.trans.stat.TrafficInfo;


/**
 * {@.en Peer class corresponds to a remote peer(node) that can be directly communicate via under-lay network.};
 * {@.ja Peer クラスは直接通信可能な遠隔ピアに対応するクラスです．}
 * <p>
 */
public class Peer {
    public static final int TransportStateUnavailable = 0;
    
    public static final int TransportStateAlive = 10;
    public static final int TransportStateAvailable = 11;
    
    public static final int TransportStateLinking = 12;
    public static final int TransportStateLinkFailure = 13;
    public static final int TransportStateLinkRefused = 14;
    public static final int TransportStateRejected = 15;
    public static final int TransportStateUnlinked = 16;

    public static final int TransportStateCommunicatable = 20;
    public static final int TransportStateLinked = 21;
    public static final int TransportStateAccepted = 22;

    
    /**
     * {@.ja ノード(ピア)のピアIDです．}{@.en The peer id of the node(peer).}
     */
    public PeerId peerId;
    public String peerIdString;
    /**
     * {@.ja ノード(ピア)の表示名です．}{@.en The screen name of the node(peer).}
     */
    public String name;
    // link/connectivity related props.
    
    public TrafficInfo traffic;
    
    //private int status;
    /**
     * {@.ja ノードの PeerLocator リストです．}{@.en The list of loctor for the node.}
     */

    // Strict maintenance is difficult.
    public List<LocatorStat> lstats;
    
    /**
     * {@.ja 最後にノード(ピア)の状態が得られた日時です．}{@.en The last time the node(peer) was observed.}
     */
    public Date lastSeen;
    /**
     * {@.ja 最後に同期した日時です．}{@.en The last synchronization time of the node}
     */
    public Date lastSync;
    /**
     * {@.ja 最後に受信したメッセージIDです．}{@.en The message id of the last message from the node}
     */
    public String lastReceivedMsgId;
    /**
     * {@.ja 最後に接続状態となった日時です．}{@.en The last connection up-time of the node}
     */    
    //public Date lastConnected;
    /**
     * {@.ja ノード(ピア)のトランスポートバージョンです．}{@.en The version information of the node(peer)'s transport.}
     */
    public String version;

    // security related props.
    /**
     * {@.ja 隣接ノード(ピア)の公開鍵です．}{@.en The public key of the neighbor node(peer).}
     */
    public String publicKey;
    /**
     * {@.ja 公開鍵の有効期限です．}{@.en The expiration time of the public key.}
     */
    public Date publicKeyExpiresAt;

    private Map<String,SecureSession> sessions; // sessionId=>Session

    /**
     * {@.ja デフォルトコンストラクタです．}{@.en The default constructor.}
     */
    public Peer() {
        lastSeen = new Date();
        lastSync = null;
        lstats = null;
        this.sessions = null;
        traffic = new TrafficInfo();
    }
    
    public void setPeerId(PeerId peerId) {
        if (peerId != null) {
            this.peerIdString = peerId.toString();
            this.peerId = peerId;
        }
    }
    
    /**
     * {@.ja <code>SecureSession</code> を登録します．}{@.en Put the <code>SecureSession</code>.}
     */
    public void putSecureSession(String sessionId, SecureSession session) {
       if (sessions == null) {
           sessions = new HashMap<String,SecureSession>();
       }
        sessions.put(sessionId, session);
    }
    /**
     * {@.ja <code>SecureSession</code> を取得します．}{@.en Get the <code>SecureSession</code>.}
     */
    public SecureSession getSecureSession(String sessionId) {
        if (sessions != null) {
            return sessions.get(sessionId);
        }
        return null;
    }
    
    public void clearSecureSessions() {
        if (sessions != null) {
            sessions.clear();
        }
    }
    /**
     * {@.ja <code>lastSeen</code> を更新します．}{@.en Updates the <code>lastSeen</code>.}
     */
    private void alive() {
        synchronized(this) {
            lastSeen = new Date();
        }
    }
    /**
     * {@.ja <code>lastSync</code> を更新します．}{@.en Updates the <code>lastSync</code>.}
     */
    public void syncDone() {
        synchronized(this) {
            lastSync = new Date();
        }
    }
    /**
     * {@.ja 同期が必要かどうかを判定します．必要なら true．}{@.en Returns true if the node requires synchronization.}
     */
    public boolean needSync() {
        if (lastSync == null) {
            return true;
        }
        else {
            Date lastConnected = null;
            // get oldest last connected
            for (LocatorStat ls : lstats) {
                if (ls.lastConnected != null) {
                    if (lastConnected != null) {
                        if (ls.lastConnected.before(lastConnected)) {
                            lastConnected = ls.lastConnected;
                        }
                    }
                    else {
                        lastConnected = ls.lastConnected;
                    }
                }
            }
            if (lastConnected == null) {
                return true;
            }
            else {
                return lastSync.before(lastConnected);
            }
        }
    }
    
    public void addLocator(PeerLocator locator) {
        if (lstats == null) {
            lstats = new ArrayList<LocatorStat>();
        }
        boolean found = false;
        for (LocatorStat ls : lstats) {
            if (ls.locator.equals(locator)) {
                found = true;
            }
        }
        if (!found) {
            LocatorStat lstat = new LocatorStat();
            lstat.locator = locator;
            lstat.status = Peer.TransportStateAvailable;
            lstat.lastSeen = new Date();
            lstats.add(lstat);
        }
    }
    
    public void setLocatorStatus(PeerLocator locator, int status) {
        LocatorStat found = null;
        lastSeen = new Date();
        if (lstats != null) {
            for (LocatorStat lstat : lstats) {
                if (lstat.locator.equals(locator)) {
                    found = lstat;
                    break;
                }
            }
        }
        if (found == null) {
            LocatorStat lstat = new LocatorStat();
            lstat.locator = locator;
            lstat.status = status;
            if (status >= TransportStateCommunicatable) {
                lstat.lastConnected = new Date();
            }
            lstat.lastSeen = new Date();
            if (lstats == null) {
                lstats = new ArrayList<LocatorStat>();
            }
            lstats.add(lstat);
        }
        else {
            if (status >= TransportStateCommunicatable) {
                if (found.status < TransportStateCommunicatable) {
                    found.lastConnected = new Date();
                }
                else {
                //    Log.d(this.getClass().toString(), "communicatable but already connected");
                }
            }
            if (((status == TransportStateAlive) || (status == TransportStateAvailable))
                    && (found.status >= TransportStateCommunicatable)) {
                    }
            else {
                found.status = status;
            }
            found.lastSeen = new Date();
        }
    }
    
    /**
     * {@.ja ノード(ピア)の状態を示します．}{@.en Indicates the node(peer) status.}
     * {@.ja 以下のいずれかの値を取ります．}{@.en It has one of following values.}
     * <P>
     * <DL>
     * <DD> <code>TransportStateAvailable</code>:
     * {@.ja ノードが発見されたことを示す．}{@.en The node is discovered.}
     * <DD> <code>TransportStateUnavailable</code>:
     * {@.ja ノードが通信範囲に無いことを示す．}{@.en The node does not exist inside communication area.}
     * <DD> <code>TransportStateLinked</code>:
     * {@.ja ノードに繋がっていることを示す．}{@.en The node is linked.}
     * <DD> <code>TransportStateUnlinked</code>:
     * {@.ja ノードが繋がっていないことを示す．}{@.en The node is not linked.}
     * <DD> <code>TransportStateLinking</code>:
     * {@.ja ノードに接続しようとしていることを示す．}{@.en Trying to connect to the node.}
     * <DD> <code>TransportStateLinkRefused</code>:
     * {@.ja ノードからの接続要求を拒否したことを示す．}{@.en Refused request from the node.}
     * <DD> <code>TransportStateAccepted</code>:
     * {@.ja ノードに接続できたことを示す．}{@.en Successfully connected to the node.}
     * <DD> <code>TransportStateRejected</code>:
     * {@.ja ノードへの接続要求が拒否されたことを示す．}{@.en The request to connect to the node was rejected.}
     * <DD> <code>TransportStateLinkFailure</code>:
     * {@.ja ノードへの接続が失敗したことを示す．}{@.en The request to connect to the node was failed.}
     * </DL>
     * </P>
     */
    public int getStatus() {
        int stat = TransportStateUnavailable;
        if (lstats != null) {
            for (LocatorStat lstat : lstats) {
                if (lstat.status >= stat) {
                    stat = lstat.status;
                }
            }
        }
        return stat;
    }
    
    public void putSend(int msgSize, PeerLocator to) {
        traffic.putSend(msgSize, to);
    }
    
    public void putReceive(int msgSize, PeerLocator from) {
        traffic.putReceive(msgSize, from);
    }
}