package org.piax.ov.ovs.dtn.impl;

import java.io.IOException;
import java.io.Serializable;
import java.net.SocketException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.piax.trans.ts.bluetooth.BluetoothLocatorChecker;
import org.piax.trans.ts.nfc.nfc.Nfc;
import org.piax.trans.ts.nfc.NfcLocator;
import org.piax.trans.tsd.bluetooth.BluetoothTSD;

import org.json.JSONArray;
import org.json.JSONException;
import org.piax.gnt.ProtocolUnsupportedException;
import org.piax.gnt.ReceiveListener;
import org.piax.gnt.SecurityManager;
import org.piax.gnt.Target;
import org.piax.gnt.Transport;
import org.piax.gnt.handover.HandoverTransport;
import org.piax.gnt.handover.Peer;
import org.piax.gnt.handover.PeerManager;
import org.piax.gnt.handover.PeerStateDelegate;
import org.piax.gnt.target.RecipientIdWithLocator;
import org.piax.ov.jmes.Command;
import org.piax.ov.jmes.Message;
import org.piax.ov.jmes.MessageData;
import org.piax.ov.jmes.MessageSecurityManager;
import org.piax.ov.jmes.von.VON;
import org.piax.ov.ovs.dtn.DTN;
import org.piax.ov.ovs.dtn.DTNAlgorithm;
import org.piax.ov.ovs.dtn.DTNException;
import org.piax.ov.ovs.dtn.MessageDB;
import org.piax.ov.ovs.dtn.PeerStateListener;

import org.piax.trans.common.PeerId;
import org.piax.trans.common.PeerLocator;

import org.piax.trans.common.ServiceInfo;
import org.piax.trans.msgframe.NoSuchPeerException;

import org.piax.trans.tsd.TSD;
import org.piax.trans.tsd.TSDRunner;
import org.piax.trans.util.PeriodicRunner;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

/**
 * {@.en AndroidDTN is an implementation of DTN for Android.}
 * {@.ja AndroidDTN は DTN の Android 実装です．}
 * <p>
 * {@.en It watches WiFi availability and when available, seaches neighbor peers, connect and exchange messages. At least, one TSD have to be run.}
 * {@.ja WiFi の状態を監視して，利用可能となったときに隣接ピアを探して接続し，メッセージを交換します．デフォルトではマルチキャストで隣接ピアを探して接続します．}
 * <p>
 * {@.ja 以下は AndroidDTN を用いる最小限のコードのサンプルです．}
 * {@.en Following is the simplest code to run AndroidDTN.}
 * <pre>
            
 * DTN dtn = DTN.getInstance("AndroidDTN");
 * dtn.setParameter(AndroidDTN.paramAndroidContext, this);
 * dtn.start("sample peer");
 * dtn.send(new Message("Hello, world."));
 * [...some process you need...]
 * dtn.fin();
 * </pre>
 * {@.ja 以下の permission を AndroidManifest.xml に設定する必要があります．}
 * {@.en Following permissions are required to be set in the AndroidManifest.xml }
 * <pre>
 * android.permission.INTERNET
 * android.permission.ACCESS_WIFI_STATE
 * android.permission.WAKE_LOCK
 * android.permission.CHANGE_WIFI_MULTICAST_STATE
 * </pre>
 * {@.ja AndroidDTN は，基本的にはバックグラウンドで Service として動作する想定ですが，DTN オブジェクトを static 宣言し，Activity 画面回転時の再起動を回避する設定をすることで，Activity で動作させる方法があります．}
 * {@.en AndroidDTN intended to run in a Service as a backgroud process. But if you declare DTN object as a static object and avoid restarting Activity when orientation change, it may be run in an Activity.}
 */
public class AndroidDTN extends DTN implements LocatorCheckerDelegate {

    /**
     * {@.en The context to use.}
     * {@.ja コンテキストオブジェクトです．}
     *<p>
     * {@.en Used in }{@link DTN#setParameter(String param, Object value) setParameter}{@.ja で <code>param</code> に指定するパラメータです．}{@.en .}
     * {@.en This is mandatory parameter. Usually your Application or Activity object.}
     * {@.ja このパラメータは設定が必須です．通常，Application か Activity オブジェクトを指定します．}
     */
    public static final int ANDROID_CONTEXT = 0x501;

    /**
     * {@.en Use RMNet.}
     * {@.ja RMNet を使います．}
     * <p>
     * {@.en Use RMNet(usually 3G network) for data exchanges instead of WiFi network. The default value is false, which means it does not use RMNet at all.}
     * {@.ja データのやりとりに RMNet(3G ネットワーク)を WiFi よりも優先して使います．デフォルトは false で，RMNet を使いません．}
     */
    public static final int USE_RMNET = 0x502;

    /**
     * {@.en The port number for PIAX.}
     * {@.ja PIAX が用いるポート番号です．}
     */
    public static final int PIAX_PORT  = 0x503;

    /**
     * {@.en Keep-alive heartbeat interval (seconds).}
     * {@.ja 定期的に自ノードが生きていることを知らせる間隔(秒)です．}
     * <p>
     * {@.en The default value is 10 seconds. Must be an Integer class object.}
     * {@.ja デフォルトは 10 秒．Integer 型で指定．}
     */
    public static final int TSD_KEEP_ALIVE_INTERVAL  = 0x504;

    /**
     * {@.en Node is regarded as unavailable after this inactivity time (seconds).}
     * {@.ja ノードが落ちているとみなす無通信時間(秒)です．}
     * <p>
     * {@.en The default value is 30 seconds.Must be an Integer class object.}
     * {@.ja デフォルトは 30 秒．Integer 型で指定．}
     */
    public static final int TSD_TIMEOUT_PERIOD  = 0x505;
    
    /**
     * {@.en The max number of connections that are established simultaneously.}
     * {@.ja 隣接ノードとして同時に張るコネクション数の上限です．}
     * <p>
     * {@.en The default value is 5. Must be an Integer class object.}
     * {@.ja デフォルトは 5．Integer 型で指定．}
     */
    public static final int NEIGHBOR_LIMIT = 0x506;

    /**
     * {@.en Enables bluetooth TSD.}
     * {@.ja Bluetooth TSDを有効にします．}
     * <p>
     * {@.en Used in }{@link DTN#setParameter(String param, Object value) setParameter}{@.ja で <code>param</code> に指定するパラメータです．}{@.en .}
     */
    public static final int TSD_ENABLE_BLUETOOTH = 0x507;
    
    /**
     * {@.en Enables multicast TSD.}
     * {@.ja マルチキャストTSDを有効にします．}
     * <p>
     * {@.en Used in }{@link DTN#setParameter(String param, Object value) setParameter}{@.ja で <code>param</code> に指定するパラメータです．}{@.en .}
     * {@.en By default multicast TSD is enabled. It is not possible to run both multicast and broadcast TSD. If broadcast TSD is enabled, multicast TSD is disabled automatically.}
     * {@.ja マルチキャストTSDはデフォルトで有効です．ブロードキャストTSDと同時に稼働できません．ブロードキャストTSDが有効化されるとマルチキャストTSDは自動的に無効化されます．}
     */
    public static final int TSD_ENABLE_MULTICAST = 0x508;
    /**
     * {@.en Enables broadcast TSD.}
     * {@.ja ブロードキャストTSDを有効にします．}
     * <p>
     * {@.en Used in }{@link DTN#setParameter(String param, Object value) setParameter}{@.ja で <code>param</code> に指定するパラメータです．}{@.en .}
     * {@.en It is not possible to run both multicast and broadcast TSD. If broadcast TSD is enabled, multicast TSD is disabled automatically.}
     * {@.ja ブロードキャストTSDはマルチキャストTSDと同時に稼働できません．ブロードキャストTSDが有効化されるとマルチキャストTSDは自動的に無効化されます．}
     */    
    public static final int TSD_ENABLE_BROADCAST = 0x509;

    /**
     * {@.en Use Bluetooth.}{@.ja Bluetooth を使用します．}
     */
    public static final int USE_BLUETOOTH = 0x510;
    
    /**
     * {@.en Specify NFC object if needed.}{@.ja NFC を利用する場合指定します．}
     */
    public static final int USE_NFC = 0x511;

    /**
     * {@.en Use Wifi.}{@.ja Wifi を使用します．}
     */
    public static final int USE_WIFI = 0x512;
    
    AndroidMessageDB db;

    /* TSD */
    boolean enableClientTSD;
    boolean enableServerTSD;
    PeerLocator clientTSDLocator;
    PeerLocator serverTSDLocator;
    boolean enableBroadcastTSD;
    boolean enableMulticastTSD;
    boolean enableBluetoothTSD;
    int tsdKeepAliveInterval;
    int tsdTimeoutPeriod;
    int neighborLimit;

    static final int DEFAULT_PIAX_PORT = 12368;
    //List<PeerInfo> nodes;
    Context ctxt;
    InetLocatorChecker inetChecker;
    BluetoothLocatorChecker btChecker;

    static final int DEFAULT_NEIGHBOR_LIMIT = 5;
    static final int DEFAULT_TSD_KEEP_ALIVE_INTERVAL = 10;
    static final int DEFAULT_TSD_TIMEOUT_PERIOD = 30;

    static final int TIMER_INTERVAL = 5;

    static final String VERSION = "1.2";

    PeerId peerId;
    String peerIdString;
    String name;
    
    boolean running;
    PeriodicRunner pr;

    PowerManager.WakeLock wl;
    
    //List<VON> vons;
    
    boolean useRMNet;
    boolean useBluetooth;
    boolean useWifi;

    List<String> getting;
    int piaxPort;
    static final PeerId BROADCAST_PEER_ID = PeerId.zeroId();
    
    Map<String,BlockingQueue<JSONArray>> queryMap;
    
    MessageSecurityManager smgr;
    DTNAlgorithm alg;
    
    Nfc nfc;

    List<PeerStateListener> pListeners;
    PeerStateDelegate delegate;

    private String ovId;
    
    List<TSD> tsds;

    // ["AVAILABLE", ["abc","def","ghi"]]
    
    public List<MessageData> getStoredMessages(ArrayList<String> mids) {
        ArrayList<MessageData> mess = new ArrayList<MessageData>();
        for (String mid : mids) {
            MessageData mes = db.fetchMessage(mid); 
            if (mes != null) {
                mess.add(mes);
            }
        }
        return mess;
    }
    
    static int rcount = 0;
    static int scount = 0;
    
    //public JSONArray listNonExpiredMessageIds() {
    //    return db.getMessageIdArray(new Date(), SYNC_MESSAGE_LIMIT);
    //}   
    
    private void setNfc(Nfc nfc) {
        ((HandoverTransport)trans).learnIdLocatorMapping(BROADCAST_PEER_ID, NfcLocator.getBroadcastLocator(nfc));
        try {
            ((HandoverTransport)trans).addLocator(NfcLocator.getSelfLocator(nfc));
        } catch (NoSuchPeerException e) {
            e.printStackTrace();
        }
    }

    /**
     * {@.en (An implmenentation of LocatorCheckerDelegate interface.)}
     * {@.ja (LocatorCheckerDelegate インタフェースの実装です．)}
     */
    public void locatorAvailable(PeerLocator l) {
        try {
            ((HandoverTransport)trans).addLocator(l);
//            trans.setAvailable(true);
            for (TSD tsd: tsds) {
                if (tsd.isRunning()) {
                    tsd.advertiseAll();
                }
            }
        } catch (NoSuchPeerException e) {
            e.printStackTrace();
        }
        //        System.out.println("NOW new default locator is:" + trans.getLocator());
    }
    
    /**
     * {@.en (An implmenentation of LocatorCheckerDelegate interface.)}
     * {@.ja (LocatorCheckerDelegate インタフェースの実装です．)}
     */
    public void locatorUnavailable(PeerLocator l) {
        //PeerLocator locator = new TcpLocator(new InetSocketAddress(addr, piaxPort));
        // XXX myself is unavailable. trans.setLocatorStatus(locator, PeerStat.TransportStateUnavailable);
        ((HandoverTransport)trans).clearLocator(l);
    }
    
    public void addTask(Runnable task) {
        pr.addTask(task);
    }

    private void fin() {
        receiveListeners.clear();
        pListeners.clear();
        pr.stop();
        trans.stop();
        if (inetChecker != null) {
            inetChecker.fin();
        }
        db.close();
        if (wl.isHeld()) {
            wl.release();
        }
    }

    @Override
    public PeerId getPeerId() {
        return peerId;
    }

    @Override
    public String getName() {
        return name;
    }

    private void addTSD(Transport trans, PeriodicRunner pr, TSD tsd) {
        ((HandoverTransport)trans).addTSD(tsd);
        pr.addTask(new TSDRunner(tsd,
                                 tsdKeepAliveInterval * 1000,
                                 tsdTimeoutPeriod * 1000));
    }

    @Override
    public void start() {
        if (running) {
            // already started.
        }
        else {
            if (ctxt == null) {
                throw new DTNException("Context is not set at start time.");
            }
            if (peerId == null) {
                peerId = PeerId.newId();
            }
            if (name == null) {
                name = Build.MODEL;
            }
            PowerManager pm = (PowerManager) ctxt.getSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DTN Lock");
            wl.acquire();
            db = new AndroidMessageDB(ctxt);
            db.open();

            pr = new PeriodicRunner(TIMER_INTERVAL * 1000);
            
            try {
                trans.start();
            } catch (IOException e) {
            }
            
            if (tsds.size() != 0) {
                for (TSD tsd: tsds) {
                    tsd.start();
                    addTSD(trans, pr, tsd);
                }
            }
            
            if (useBluetooth) {
                btChecker = new BluetoothLocatorChecker(ctxt, this);
                pr.addTask(btChecker);
            }
            if (useWifi || useRMNet) {
                inetChecker = new InetLocatorChecker(ctxt, piaxPort, this);
                inetChecker.setUseRMNet(useRMNet);
                pr.addTask(inetChecker);
            }
            
            running = true;
        }
    }
    
    protected void setParameter(int p, Object obj) {
        super.setParameter(p, obj);
        switch (p) {
        case ANDROID_CONTEXT:
            this.ctxt = (Context) obj;
        break;
        case PIAX_PORT:
            piaxPort = (((Integer)obj).intValue());
        break;
        case NEIGHBOR_LIMIT:
            neighborLimit = (((Integer)obj).intValue());
        break;
        case TSD_KEEP_ALIVE_INTERVAL:
            this.tsdKeepAliveInterval = (((Integer)obj).intValue());
        break;
        case TSD_TIMEOUT_PERIOD:
            this.tsdTimeoutPeriod = (((Integer)obj).intValue());
        break;
        case TSD_ENABLE_MULTICAST:
            this.enableMulticastTSD = (((Boolean)obj).booleanValue());
        break;
        case TSD_ENABLE_BROADCAST:
            this.enableBroadcastTSD = (((Boolean)obj).booleanValue());
        break;
        case TSD_ENABLE_BLUETOOTH:
            this.enableBluetoothTSD = (((Boolean)obj).booleanValue());
        break;
        case USE_RMNET:
            this.useRMNet = (((Boolean)obj).booleanValue());
        break;
        case USE_BLUETOOTH:
            this.useBluetooth = (((Boolean)obj).booleanValue());
        break;
        case USE_WIFI:
            this.useWifi = (((Boolean)obj).booleanValue());
        break;
        }
//        case NFC:
//            nfc = (Nfc) obj;
//        break;
    }

    public boolean supportsParameter(int p) {
        return p < 0x13;
    }
    
    /**
     * コンストラクタです．
     */
    
    public AndroidDTN(Map<Integer,Object> params) {
        super(params);
        //this.ctxt = Context.getApplicationContext();
        //this.ctxt = ctxt;
        tsds = new ArrayList<TSD>();
        //vons = new ArrayList<VON>();
        pListeners = new ArrayList<PeerStateListener>();
        
        running = false;

        // Needs BOUNCYCASTLE for upper compatibility
        try {
            if (android.os.Build.VERSION.SDK_INT < 10) {
                Security.insertProviderAt((Provider) Class.forName("org.bouncycastle2.jce.provider.BouncyCastleProvider").newInstance(), 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (params.get(PEER_ID) == null) {
            throw new DTNException("peer id is required.");
        }
        else {
            peerId = (PeerId)params.get(PEER_ID);
        }
        peerIdString = peerId.toString();
        
        if (params.get(PEER_NAME) == null) {
            throw new DTNException("peer name is required.");            
        }
        else {
            name = (String)params.get(PEER_NAME);
        }
        
        smgr = (MessageSecurityManager)params.get(SECURITY_MANAGER);
        
        ovId = (String)params.get(SERVICE_ID); 
        
        alg = (DTNAlgorithm)params.get(ALGORITHM);
        alg.attachDTN(this);

        
        if (params.get(PIAX_PORT) == null) {
            setParameter(PIAX_PORT, DEFAULT_PIAX_PORT);
        }
        if (params.get(TSD_KEEP_ALIVE_INTERVAL) == null) {
            setParameter(TSD_KEEP_ALIVE_INTERVAL, DEFAULT_TSD_KEEP_ALIVE_INTERVAL);
        }
        if (params.get(TSD_TIMEOUT_PERIOD) == null) {
            setParameter(TSD_TIMEOUT_PERIOD, DEFAULT_TSD_TIMEOUT_PERIOD);
        }
        if (params.get(NEIGHBOR_LIMIT) == null) {
            setParameter(NEIGHBOR_LIMIT, DEFAULT_NEIGHBOR_LIMIT);
        }
        if (((Boolean)params.get(TSD_ENABLE_MULTICAST)).booleanValue()) {
            try {
                TSD tsd = new AndroidMulticastTSD(ctxt);
                tsds.add(tsd);
            } catch (SocketException e) {
                e.printStackTrace();
            }            
        }
        if (((Boolean)params.get(TSD_ENABLE_BROADCAST)).booleanValue()) {
            try {
                TSD tsd = new AndroidBroadcastTSD(ctxt);
                tsds.add(tsd);
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        if (((Boolean)params.get(TSD_ENABLE_BLUETOOTH)).booleanValue()) {
            TSD tsd = new BluetoothTSD((Activity)ctxt);
            tsds.add(tsd);
        }
        trans = new HandoverTransport(peerId, name);
        ((HandoverTransport)trans).setPeerStateDelegate(this);
        smgr.setPeerManager((HandoverTransport)trans);
        trans.addReceiveListener(this);
    }

    @Override
    public void setPublicKey(PeerId id, String publicKey, Date publicKeyExpiresAt) {
        Peer peer = ((HandoverTransport)trans).getPeerCreate(id);
        if (peer != null) {
            peer.publicKey = publicKey;
            peer.publicKeyExpiresAt = publicKeyExpiresAt;
        }
    }
    
    private void setupMandatoryFields(MessageData mes) {
        // Setup mandatory fields.
        if (mes.source_id == null) {
            mes.source_id = peerIdString;
        }
        if (mes.screen_name == null) {
            mes.screen_name = name;
        }
        if (mes.created_at == null) {
            mes.created_at = new Date();
        }
        if (mes.id == null) {
            Date refDate = new Date(2001 - 1900, 1, 1);
            mes.id = mes.source_id + "." + (mes.created_at.getTime() - refDate.getTime());
        }
        if (mes.expires_at == null) {
            mes.expires_at = new Date(mes.created_at.getTime() + MessageData.DEFAULT_EXPIRE_INTERVAL);
        }
        if (mes.via == null) {
            if (mes.via == null) {
                mes.via = new ArrayList<String>();
            }
            mes.via.add(mes.source_id);
        }
        if (mes.ttl == 0) {
            mes.ttl = MessageData.DEFAULT_TTL;
        }        
    }

    public void newMessage(MessageData md) {
        setupMandatoryFields(md);
        Message m = new Message((MessageSecurityManager)smgr, md, ovId);
        alg.newMessage(m);
    }

    @Override
    public MessageDB getDB() {
        return db;
    }

    @Override
    public List<Peer> getNodes() {
        return ((HandoverTransport)trans).listSortedPeers();
    }

    @Override
    public boolean hasMessage(String id) {
        MessageData mes = db.fetchMessage(id);
        return mes != null;
    }

    @Override
    public void stop() {
        fin();        
    }

    @Override
    protected void onReceiveCommand(Target target, Command com) {
        RecipientIdWithLocator idAndLocator = (RecipientIdWithLocator) target;
        
        Peer src = ((HandoverTransport)trans).getPeerCreate(new PeerId(com.senderId));
        if (src != null) {
            alg.onReceiveCommand(src, idAndLocator.getLocator(), com);
        }
    }  

    @Override
    public Message decode(MessageData md) {
        return ((MessageSecurityManager)smgr).decapsulate(md);
    }

    @Override
    public MessageData encode(Message mes) {
        return ((MessageSecurityManager)smgr).convert(mes);
    }

    @Override
    public void newLink(Peer peer, PeerLocator locator) {
        ((HandoverTransport)trans).newLink(peer, locator);
    }

    @Override
    public void addPeerStateListener(PeerStateListener listener) {
        pListeners.add(listener);
        
    }

    @Override
    public void clearPeerStateListeners() {
        pListeners.clear();
    }

    @Override
    public void onPeerStateChange(Peer peer, PeerLocator locator, int state) {
        delegate.onPeerStateChange(peer, locator, state);
        for (PeerStateListener psl: pListeners) {
            psl.onPeerStateChange(peer, locator, state);
        }
    }

    @Override
    public boolean onAccepting(Peer peer, PeerLocator locator) {
        return delegate.onAccepting(peer, locator);
    }

    @Override
    public void setPeerStateDelegate(PeerStateDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void receiveMessage(Message mes) {
        for (ReceiveListener listener : receiveListeners) {
            listener.onReceive(mes.getTarget(), mes);
        }
    }

    @Override
    public void send(Target target, Serializable payload) throws ProtocolUnsupportedException, IOException {
        if (!(payload instanceof MessageData)) {
            throw new ProtocolUnsupportedException("Only 'MessageData' type is supported.");
        }
        newMessage((MessageData) payload);
    }
    
}
