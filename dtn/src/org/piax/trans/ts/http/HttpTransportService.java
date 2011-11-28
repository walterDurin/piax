package org.piax.trans.ts.http;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.piax.trans.common.PeerLocator;
import org.piax.trans.ts.BytesReceiver;
import org.piax.trans.ts.LocatorTransportSpi;

public class HttpTransportService implements LocatorTransportSpi {

    public HttpTransportService(BytesReceiver bytesReceiver,
                                HttpLocator peerLocator) throws IOException {
        
    }

    public boolean canSend(PeerLocator target) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean canSet(PeerLocator target) {
        // TODO Auto-generated method stub
        return false;
    }

    public void fin() {
        // TODO Auto-generated method stub
        
    }

    public PeerLocator getLocator() {
        // TODO Auto-generated method stub
        return null;
    }

    public void sendBytes(boolean isSend, PeerLocator toPeer, ByteBuffer msg)
            throws IOException {
        // TODO Auto-generated method stub
        
    }

    public void setLocator(PeerLocator locator) {
        // TODO Auto-generated method stub
        
    }
    
}