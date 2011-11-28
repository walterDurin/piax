package org.piax.ov.ovs.dtn;

import org.piax.trans.Peer;
import org.piax.trans.common.PeerLocator;

/**
 * {@.en An interface to be notified the neighbor node(peer) status.}
 * {@.ja 隣接ノード(ピア)の状態が変化した通知を受けるためのインタフェースです．}
 * <p>
 */
public interface PeerStateListener {
    /**
     * {@.ja 隣接ノード(ピア)の状態が変化したときに呼ばれる．}
     * {@.en called when the neighbor node(peer) status is changed.}
     * @param peer the peer which changed status.
     * @param locator the locator which changed status.
     * @param state the state which is changed.
     */
    public void onPeerStateChange(Peer peer, PeerLocator locator, int state);
}