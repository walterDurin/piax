package org.piax.trans.ts.nfc.nfc;

import java.nio.ByteBuffer;

public interface NfcReceiver {
    public void receive(ByteBuffer bb);
}
