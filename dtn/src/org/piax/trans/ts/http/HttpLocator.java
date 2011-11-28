package org.piax.trans.ts.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collection;

import org.piax.trans.common.PeerLocator;
import org.piax.trans.ts.BytesReceiver;
import org.piax.trans.ts.InetLocator;
import org.piax.trans.ts.LocatorTransportSpi;

public class HttpLocator extends InetLocator {

    private static final long serialVersionUID = 7250057206823539138L;
    public static final int ID = 80;
    
    public HttpLocator(ByteBuffer bbuf) throws UnknownHostException {
        super(bbuf);
        // TODO Auto-generated constructor stub
    }
    
    public HttpLocator(InetSocketAddress addr) {
        super(addr);
    }
    
    @Override
    public byte getId() {
        return ID;
    }
    @Override
    public LocatorTransportSpi newLocatorTransportService(
            BytesReceiver bytesReceiver, Collection<PeerLocator> relays)
            throws IOException {
        // TODO Auto-generated method stub
        return null;
    }
    

}
