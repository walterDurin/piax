package org.piax.ov.jmes;

import org.piax.gnt.Target;
import org.piax.ov.Overlay;

/**
 * {@.en An interface to be notified when a command is received.}
 * {@.ja コマンドを受信した通知を受けるためのインタフェースです．}
 * <p>
 * {@.en The <code>onReceive()</code> is called by specifying implementation class of this interface to the }
 * {@.ja MessageOverlay クラスの } {@link Overlay#addMessageReceiveListener(MessageReceiveListener listener) addMessageReceiveListener} {@.ja に指定すると，メッセージを受信したときに実装クラスの <code>onArrival()</code> が呼ばれます．}{@.en &nbsp; of the Overlay class.}
 */
public interface CommandReceiveListener {
    /**
     * {@.ja コマンドを受信したときに呼ばれる．}
     * {@.en called when a command is received.}
     * @param com {@.ja 受信した Command.}{@.en The command that has been arrived.}
     */
    public void onReceiveCommand(Command com);
}
