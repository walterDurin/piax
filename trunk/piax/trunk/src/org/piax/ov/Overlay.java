package org.piax.ov;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.piax.ov.cond.Condition;
import org.piax.trans.Peer;
import org.piax.trans.common.ReturnSet;

public abstract class Overlay {
    private String overlayId;
    private SecurityManager smgr;
    protected List<DeliveryListener> deliveryListeners;
    protected List<DiscoveryListener> discoveryListeners;
    
    public Overlay(String overlayId) {
        this(overlayId, null);
    }
    public Overlay(String overlayId, SecurityManager smgr) {
        this.overlayId = overlayId;
        this.smgr = smgr;
        this.deliveryListeners = new ArrayList<DeliveryListener>();
        this.discoveryListeners = new ArrayList<DiscoveryListener>();
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
    
    public String getOverlayId() {
        return overlayId;
    }
    
    // データ配信のエントリポイント。
    public abstract void deliver(Condition condition, Object payload);

    public void addDeliveryListener(DeliveryListener listener) {
        deliveryListeners.add(listener);
    }
    
    public void removeDeliveryListener(DeliveryListener listener) {
        deliveryListeners.remove(listener);
    }

    // クエリ実行のエントリポイント。
    public abstract ReturnSet<?> discover(Condition condition, Object payload) throws UnsupportedConditionException;
    public void addDiscoveryListener(DiscoveryListener listener) {
        discoveryListeners.add(listener);
    }
    public void removeDiscoveryListener(DiscoveryListener listener) {
        discoveryListeners.remove(listener);
    }

    // そのオーバレイ扱うことができるクエリの型を返却する。
    public abstract List<Class> getConditionClass();
}
