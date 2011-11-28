package org.piax.trans.ts.nfc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;

import org.piax.trans.ts.nfc.nfc.Nfc;
import org.piax.trans.ts.nfc.nfc.NfcReceiver;

import org.piax.trans.common.Id;
import org.piax.trans.common.PeerLocator;
import org.piax.trans.common.ServiceInfo;
import org.piax.trans.ts.BytesReceiver;
import org.piax.trans.ts.LocatorTransportSpi;

public class NfcLocator extends PeerLocator {
    private static final long serialVersionUID = -4708361582007368443L;

    static final public byte SELF = 0;
    static final public byte OTHER = 127;

    static final public byte ID = 21;
    Nfc nfc;
    byte address;

    
    private class NfcTransportReceiver implements NfcReceiver {
        NfcTransportService nts;
        BytesReceiver br;
        public NfcTransportReceiver(NfcTransportService nts, BytesReceiver br) {
            this.nts = nts;
            this.br = br;
        }
        
        @Override
        public void receive(ByteBuffer bb) {
            br.receiveBytes(nts, bb);
        }
    }
    
    public NfcLocator(Nfc nfc, boolean broadcast) {
        this.nfc = nfc;
        this.address = broadcast ? OTHER : SELF; 
    }
    
    public NfcLocator(ByteBuffer bbuf) {
        this.nfc = null;
        byte[] addr = new byte[1];
        bbuf.get(addr);
        this.address = addr[0];
    }
    
    public static NfcLocator getBroadcastLocator(Nfc nfc) {
        return new NfcLocator(nfc, true); 
    }

    public static NfcLocator getSelfLocator(Nfc nfc) {
        return new NfcLocator(nfc, false);
    }
    
    @Override
    public byte getId() {
        return ID;
    }

    @Override
    public int getPackLen() {
        return 1;
    }

    @Override
    public String getPeerNameCandidate() {
        return "NFC";
    }

    @Override
    public ServiceInfo getServiceInfo() {
        return null;
    }
    
    @Override
    public boolean immediateLink() {
        return false;
    }

    @Override
    public LocatorTransportSpi newLocatorTransportService(
            BytesReceiver bytesReceiver, Collection<PeerLocator> relays)
            throws IOException {
        NfcTransportService nts = new NfcTransportService(bytesReceiver, this);
        NfcTransportReceiver ntr = new NfcTransportReceiver(nts, bytesReceiver);
        nfc.setupReceiver(ntr);
        return nts;
    }

    @Override
    public void pack(ByteBuffer bbuf) {
        byte[] macBytes = new byte[] { address };
        bbuf.put(macBytes);
    }

    @Override
    public boolean sameClass(PeerLocator target) {
        if (target == null) return false;
        return this.getClass().equals(target.getClass());
    }
    
    @Override
    public boolean equals(Object target) {
        return sameClass((PeerLocator)target);
    }

}
