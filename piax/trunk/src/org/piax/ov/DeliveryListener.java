package org.piax.ov;

import org.piax.ov.cond.Condition;

/**
 * {@.en An interface to be notified when a message is received.}
 * {@.ja メッセージを受信した通知を受けるためのインタフェースです．}
 * <p>
 * {@.en The <code>onArrival()</code> is called by specifying implementation class of this interface to the }
 * {@.ja Overlay クラスの } {@link Overlay#addDeliveryListener(DeliveryListener listener) addDeliveryListener} {@.ja に指定すると，メッセージを受信したときに実装クラスの <code>onArrival()</code> が呼ばれます．}{@.en &nbsp; of the Overlay class.}
 */
public interface DeliveryListener {
    /**
     * {@.ja メッセージを受信したときに呼ばれる．}
     * {@.en called when a message is received.}
     * @param payload {@.ja 受信したオブジェクト.}{@.en The message payload that has been arrived.}
     */
    public void onArrival(Condition cond, Object payload);
}
