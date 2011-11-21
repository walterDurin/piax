package org.piax.ov;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.piax.trans.Peer;
import org.piax.trans.common.ReturnSet;

public abstract class Overlay {
    private String serviceId;
    private SecurityManager smgr;
    protected List<ReceiveListener> receiveListeners;
    protected List<MatchListener> matchListeners;
    
    public Overlay(String serviceId) {
        this(serviceId, null);
    }
    public Overlay(String serviceId, SecurityManager smgr) {
        this.serviceId = serviceId;
        this.smgr = smgr;
        this.receiveListeners = new ArrayList<ReceiveListener>();
        this.matchListeners = new ArrayList<MatchListener>();
    }
    
    // Overlay の動作を開始する。join。
    public abstract void start() throws IOException;
    // Overlay の動作を終了する。leave。
    public abstract void stop();
    
    // 紹介者(Seed)を設定する。
    // Peer はリモートピアと通信するためのオブジェクト。
    public void setIntroducerList(List<Peer> introducers) {
        // Do nothing.
    }
    
    public String getServiceId() {
        return serviceId;
    }
    
    public SecurityManager getSecurityManager() {
        return smgr;
    }
    
    // データ配信のエントリポイント。
    public abstract void send(Target target, Object payload);

    public void addReceiveListener(ReceiveListener listener) {
        receiveListeners.add(listener);
    }
    
    public void removeReceiveListener(ReceiveListener listener) {
        receiveListeners.remove(listener);
    }

    // クエリ実行のエントリポイント。
    // payload がそのまま MatchListener に渡される。
    public abstract ReturnSet<?> query(Target target, Object payload) throws UnsupportedTargetException;
    
    public void addMatchListener(MatchListener listener) {
        matchListeners.add(listener);
    }
    public void removeMatchListener(MatchListener listener) {
        matchListeners.remove(listener);
    } 

    // そのオーバレイが扱うことができるTargetの型を返却する。
    public abstract List<Class<? extends Target>> getTargetClass();
}
