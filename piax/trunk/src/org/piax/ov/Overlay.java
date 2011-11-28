package org.piax.ov;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.piax.trans.Peer;
import org.piax.trans.ReceiveListener;
import org.piax.trans.RequestListener;
import org.piax.trans.SecurityManager;
import org.piax.trans.Target;
import org.piax.trans.Transport;
import org.piax.trans.ProtocolUnsupportedException;
import org.piax.trans.common.PeerId;
import org.piax.trans.common.ReturnSet;

public abstract class Overlay implements Transport {
    // underlay transport.
    protected Transport trans;
    protected String serviceId;
    protected SecurityManager smgr;
    protected List<ReceiveListener> receiveListeners;
    protected List<RequestListener> requestListeners;
    protected Map<Integer,Object> params;
    
 // mandatory parameters to start.
    public static final int TRANSPORT = 0x1;
    public static final int SERVICE_ID = 0x2;
    public static final int SECURITY_MANAGER = 0x3; 

    public Overlay(Map<Integer,Object> params) {
        this(null, null, null);
        if (params != null) {
            for (int p : params.keySet()) {
                setParameter(p, params.get(p));
            }
        }
    }
    
    protected void setParameter(int param, Object value) {
        switch (param) {
        case TRANSPORT:
            trans = (Transport) value;
        break;
        case SERVICE_ID:
            serviceId = (String) value;
        break;
        case SECURITY_MANAGER:
            smgr = (SecurityManager) value;
        break;
        }
    }
    
    public Overlay(Transport trans, String serviceId, SecurityManager smgr) {
        this.trans = trans;
        this.serviceId = serviceId;
        this.smgr = smgr;
        this.receiveListeners = new ArrayList<ReceiveListener>();
        this.requestListeners = new ArrayList<RequestListener>();
    }
    
    // Overlay の動作を開始する。join。
    @Override
    public abstract void start() throws IOException;
    // Overlay の動作を終了する。leave。
    @Override
    public abstract void stop();
    
    // 紹介者(Seed)を設定する。
    // Peer はリモートピアと通信するためのオブジェクト。
    public void setIntroducerList(List<Peer> introducers) {
        // Do nothing.
    }
    
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public SecurityManager getSecurityManager() {
        return smgr;
    }
    
    // データ配信のエントリポイント。
    @Override
    public abstract void send(Target target, Serializable payload) throws ProtocolUnsupportedException, IOException;

    @Override
    public void addReceiveListener(ReceiveListener listener) {
        receiveListeners.add(listener);
    }
    
    @Override
    public void removeReceiveListener(ReceiveListener listener) {
        receiveListeners.remove(listener);
    }

    @Override
    public void clearReceiveListeners() {
        receiveListeners.clear();
    }

    // 遠隔実行のエントリポイント。
    // payload がそのまま MatchListener に渡される。
    @Override
    public abstract ReturnSet<?> request(Target target, Serializable payload) throws ProtocolUnsupportedException, IOException;

    @Override
    public void addRequestListener(RequestListener listener) {
        requestListeners.add(listener);
    }
    
    @Override
    public void removeRequestListener(RequestListener listener) {
        requestListeners.remove(listener);
    } 
    
    @Override
    public void clearRequestListeners() {
        requestListeners.clear();
    }

    // そのオーバレイが扱うことができるTargetの型を返却する。
    public abstract List<Class<? extends Target>> getTargetClass();
}
