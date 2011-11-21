package org.piax.trans;

import org.piax.ov.Overlay;


/**
 * {@.en An interface to be notified when a discovery condition is matched.}
 * {@.ja 探索要求にマッチしたことの通知を受け，返信するためのインタフェースです．}
 * <p>
 * {@.en The <code>onRequest()</code> is called by specifying implementation class of this interface to the }
 * {@.ja Overlayクラスの } {@link Overlay#addRequestListener(RequestListener listener) addRequestListener} {@.ja に指定すると，条件を満たすときに実装クラスの <code>onConditionMatch()</code> が呼ばれます．}{@.en &nbsp; of the Overlay class.}
 */
public interface RequestListener {
    /**
     * {@.ja メッセージを受信したときに呼ばれる．}
     * {@.en called when a message is received.}
     * @param target {@.ja 探索要求に指定された対象です．}{@.en The target specified by the discovery.}
     * @param payload {@.ja 受信したオブジェクト.}{@.en The message payload that has been arrived.}
     * @return {@.ja discovery に対する返信オブジェクト．}{@.en The reply object for the discovery.}
     */
    public Object onRequest(Target target, Object payload);
}
