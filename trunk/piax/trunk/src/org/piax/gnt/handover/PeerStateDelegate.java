package org.piax.gnt.handover;

import org.piax.trans.common.PeerLocator;


/**
 * {@.en An interface to be notified the neighbor node(peer) status.}
 * {@.ja 隣接ノード(ピア)の状態が変化した通知を受けるためのインタフェースです．}
 * <p>
 */
public interface PeerStateDelegate {
    /**
     * {@.ja 隣接ノード(ピア)の状態が変化したときに呼ばれる．}
     * {@.en called when the neighbor node(peer) status is changed.}
     * @param peer the peer which changed status.
     * @param locator the locator which changed status.
     * @param state the state which is changed.
     */
    public void onPeerStateChange(Peer peer, PeerLocator locator, int state);
    /**
     * {@.ja コンタクト要求を受け取ったときに呼ばれる．false を返すと、要求は拒否される．}
     * {@.en called when a neighbor node requests to contact. If the return value is false, the request is denied.} 
     * @param peer the peer which requests to contact.
     * @return Denies the request if false is returned.
     */
    public boolean onAccepting(Peer peer, PeerLocator locator);
}