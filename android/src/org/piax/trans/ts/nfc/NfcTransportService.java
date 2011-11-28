package org.piax.trans.ts.nfc;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.piax.trans.common.PeerLocator;
import org.piax.trans.ts.BytesReceiver;
import org.piax.trans.ts.LocatorTransportSpi;

public class NfcTransportService implements LocatorTransportSpi {
    NfcLocator peerLocator;
    BytesReceiver bytesReceiver;

    public NfcTransportService(BytesReceiver bytesReceiver,
                               NfcLocator peerLocator) {
        this.peerLocator = peerLocator;
        this.bytesReceiver = bytesReceiver;
    }

    @Override
    public boolean canSend(PeerLocator target) {
        return peerLocator.sameClass(target);
    }

    @Override
    public boolean canSet(PeerLocator target) {
        return target instanceof NfcLocator && peerLocator == null;
    }

    @Override
    public void fin() {
        // nothing to do.
    }

    @Override
    public PeerLocator getLocator() {
        return peerLocator;
    }

    @Override
    public void sendBytes(boolean isSend, PeerLocator toPeer, ByteBuffer msg)
        throws IOException {
        // data, offset, length
        int offset = msg.arrayOffset() + msg.position();
        int length = msg.remaining();
        byte[] sending = new byte[length];
        msg.get(sending, 0, length);
        peerLocator.nfc.send(sending);
    }

    @Override
    public void setLocator(PeerLocator locator) {
        peerLocator = (NfcLocator) locator;
    }
}
