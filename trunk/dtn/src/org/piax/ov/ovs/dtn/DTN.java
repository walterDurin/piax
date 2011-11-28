package org.piax.ov.ovs.dtn;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.piax.ov.jmes.Message;
import org.piax.ov.jmes.MessageData;
import org.piax.ov.jmes.MessageOverlay;
import org.piax.ov.jmes.MessageSecurityManager;
import org.piax.trans.Peer;
import org.piax.trans.PeerStateDelegate;
import org.piax.trans.SecurityManager;
import org.piax.trans.common.PeerId;
import org.piax.trans.common.PeerLocator;

/**
 * {@.en DTN is a class that is intended to run as an overlay network module, to send messages to the all peers.}
 * {@.ja DTN クラスは，メッセージを全てのピアに蓄積転送方式により送信するためのオーバレイネットワークモジュールです．}
 * <p>
 * {@.en DTN class(subclass of DTN) runs by itself and send messages by store and forward manner. By store and forward message handling, messages can be distributed to the peers in the unstable network although it takes time.}
 * {@.ja DTN クラス(のサブクラス)は，単体で動作し，メッセージを蓄積転送方式により送信します．送信されたメッセージを蓄積し，オンラインになったときに送信するという動作をすることにより，時間はかかりますが，不安定なネットワークにおいてもメッセージを到達させることができます．}
 * <p>
 * {@.en DTN clas itself is an abstract class and therefore any of subclasses must be used.}
 * {@.ja DTNクラスは抽象クラスであり，実装クラスのいずれかを用いる必要があります．}
 */
public abstract class DTN extends MessageOverlay implements PeerStateDelegate {
    // mandatory parameters to start.
    public static final int PEER_ID = 0x101;
    public static final int PEER_NAME = 0x102;
    public static final int ALGORITHM = 0x103;
    
    public DTN(Map<Integer,Object> params) {
        super(params);
    }
    
    /**
     * {@.en Returns a new DTN instance.}{@.ja 新規 DTN インスタンスを生成します．}
     * @param name {@.ja 実装名を指定します．}{@.en The implementation name.}
     * 
     */
    public static DTN getInstance(String name, Map<Integer, Object> params) {
        try {
            Class<? extends DTN> clazz = (Class<? extends DTN>) Class.forName("org.piax.ov.ovs.dtn.impl." + name);
            Class<?>[] types = { Map.class };
            Constructor<? extends DTN> constructor = clazz.getConstructor(types);
            Object[] args = {params};
            return constructor.newInstance(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public abstract void setPeerStateDelegate(PeerStateDelegate delegate);


    /**
     * {@.en Sends a message to the network.}{@.ja メッセージをネットワークに送信します．}&nbsp;
     * {@.en The message are received by all peers since it does not specify VON.} {@.ja VON 指定なしで送信されるので全てのピアが受信できるメッセージを送信します.}
     * @param mes  {@.en The message to send.} {@.ja 送信するメッセージオブジェクト．}
     */
    public abstract void newMessage(MessageData mes);
    
    /**
     * {@.en Registers a <code>PeerStateListener</code>.}{@.ja <code>PeerStateListener</code>を登録します．}&nbsp;
     * @param listener {@.en a Listener object that implements }{@link PeerStateListener}{@.ja の実装クラスのインスタンス}
     */
    public abstract void addPeerStateListener(PeerStateListener listener);
    
    /**
     * {@.en Clear  <code>PeerStateDelegate</code>s.}{@.ja <code>PeerStateDelegate</code>を初期状態にもどします．}
     */
    public abstract void clearPeerStateListeners();

    /**
     * {@.en Returns the peer id.}{@.ja ピアID を返却します．}
     * @return {@.ja ピアID．} {@.en the peer id.}
     */
    public abstract PeerId getPeerId();
    /**
     * {@.en Returns the peer's screen name.}{@.ja ピア表示名を返却します．}
     * @return {@.ja ピア表示名．} {@.en the screen name.}
     */
    public abstract String getName();
    
    /**
     * {@.en Starts running as a DTN peer.}{@.ja DTN ピアとしての稼働を開始します．}
     */
    public abstract void start();

    
    public abstract void newLink(Peer peer, PeerLocator locator);
    
    /**
     * {@.en Checks if the peer has specified message.}{@.ja 指定されたメッセージを保持しているかどうかを判定します．}&nbsp;
     * @param id {@.ja メッセージID．} {@.en A message ID.}
     * @return {@.en Returns <code>true</code> if the peer has specfied message.}{@.ja 指定されたメッセージを保持している場合に <code>true</code>．}
     */
    public abstract boolean hasMessage(String id);
    
    /**
     * {@.en Get specified messages if stored.}{@.ja 指定されたメッセージ集合のうち保持されているものを取得します．}&nbsp;
     * @param mids
     * @return
     */
    public abstract List<MessageData> getStoredMessages(ArrayList<String> mids);

    public abstract void receiveMessage(Message mes);
    public abstract Message decode(MessageData md);
    public abstract MessageData encode(Message mes);

    /**
     *{@.en Registers a <code>VONManager</code>.} 
     * @param mgr VONManager {@.ja オブジェクト}{@.en object}
     */
    //public abstract void setVONManager(MessageSecurityManager mgr);
    
    //public abstract void setAuthInfo(String authorityPublicKey, String userPublicKey, String userPrivateKey, String vonEntries);

    //public abstract MessageSecurityManager getVONManager();

    /**
     * {@.en Add public key string for the <code>peerId</code>}{@.ja <code>peerId</code> に公開鍵を関連づけます．}&nbsp;
     */
    //public abstract void setPublicKey(PeerId id, String publicKey, Date publicKeyExpiresAt);

    /**
     * {@.en Returns a message DB that is used for DTN.}{@.ja DTN で用いられるメッセージDBを取得します.}&nbsp;
     * <p>
     * @return {@.ja メッセージDB．} {@.en A message DB.}
     */
    public abstract MessageDB getDB();
    /**
     * {@.en Returns a neighbor node list.}{@.ja 隣接ノードリストを取得します.}&nbsp;
     * <p>
     * @return {@.ja <code>Peer</code> のリスト} {@.en A list of <code>PeerInfo</code>.}
     */
    public abstract List<Peer> getNodes();
    /**
     * {@.en add periodic task using DTN's timer.}{@.ja 定期的に実行するタスクをDTNのタイマを用いて実行します．}
     * @param task {@.ja タスク}{@.en Periodic task to run}
     * 
     */
    public abstract void addTask(Runnable task);

    public abstract void setPublicKey(PeerId id, String publicKey, Date publicKeyExpiresAt);

}
