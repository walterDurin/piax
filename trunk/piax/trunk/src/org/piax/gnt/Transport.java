package org.piax.gnt;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.piax.trans.common.ReturnSet;

public interface Transport {
    // Transport の動作を開始する。
    public abstract void start() throws IOException;
    // Transport の動作を終了する。
    public abstract void stop();
    
    public SecurityManager getSecurityManager();
    
    // データ配信のエントリポイント。
    public abstract void send(Target target, Serializable payload) throws ProtocolUnsupportedException, IOException;

    public void addReceiveListener(ReceiveListener listener);
    public void removeReceiveListener(ReceiveListener listener);
    public void clearReceiveListeners();

    // 遠隔実行のエントリポイント。
    // payload がそのまま RequestListener に渡される。
    public ReturnSet<?> request(Target target, Serializable payload) throws ProtocolUnsupportedException, IOException;
    
    public void addRequestListener(RequestListener listener);
    public void removeRequestListener(RequestListener listener);
    public void clearRequestListeners();
    
    // 扱うことができるTargetの型を返却する。
    public List<Class<? extends Target>> getTargetClass();
}
