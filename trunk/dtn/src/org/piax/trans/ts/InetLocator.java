/*
 * InetLocator.java
 * 
 * Copyright (c) 2006- Osaka University
 * Copyright (c) 2004-2005 BBR Inc, Osaka University
 * 
 * Permission is hereby granted, free of charge, to any person obtaining 
 * a copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including 
 * without limitation the rights to use, copy, modify, merge, publish, 
 * distribute, sublicense, and/or sell copies of the Software, and to 
 * permit persons to whom the Software is furnished to do so, subject to 
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be 
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
/*
 * Revision History:
 * ---
 * 2007/10/24 designed and implemented by M. Yoshida.
 * 
 * $Id: InetLocator.java 225 2010-06-20 12:34:07Z teranisi $
 */

package org.piax.trans.ts;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.piax.trans.common.PeerLocator;
import org.piax.trans.common.ServiceInfo;
import org.piax.trans.util.AddressUtil;
import org.piax.trans.util.LocalInetAddrs;

/**
 * IPv4アドレスを示すPeerLocatorの抽象クラス。
 * 
 * @author     Mikio Yoshida
 * @version    2.1.0
 */
public abstract class InetLocator extends PeerLocator {
    private static final long serialVersionUID = 7679302658794618590L;

    /*
     * patch for GCJ-4.1.0 bug
     */
    private transient InetSocketAddress addr;
    
    protected InetLocator(InetSocketAddress addr) {
        if (addr == null)
            throw new IllegalArgumentException("argument should not be null");
        this.addr = addr;
    }
    
    public InetLocator(ByteBuffer bbuf) throws UnknownHostException {
        addr = PeerLocator.getAddr(bbuf);
        if (addr == null)
            throw new UnknownHostException();
    }

    public String getHostName() {
        return addr.getHostName();
    }
    
    public int getPort() {
        return addr.getPort();
    }
    
    public InetSocketAddress getSocketAddress() {
        return addr;
    }
    
    @Override
    public boolean sameClass(PeerLocator target) {
        if (target == null) return false;
        return target instanceof InetLocator;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof InetLocator)) {
            return false;
        }
        InetLocator _obj = (InetLocator) obj;
        if (getPort() != _obj.getPort()) return false;
        if (addr.getAddress().equals(_obj.addr.getAddress())) 
            return true;
        if (LocalInetAddrs.isLocal(addr.getAddress())
                && LocalInetAddrs.isLocal(_obj.addr.getAddress()))
            return true;
        return false;
    }
    
    // TODO this not equivalent to equals method
    @Override
    public int hashCode() {
        return addr.getPort();
    }
    
    @Override
    public String toString() {
        return addr.getAddress().getHostAddress() + ":" + getPort();
    }
    
    /*
     * patch for GCJ-4.1.0 bug
     */
    private void writeObject(java.io.ObjectOutputStream s)
    throws java.io.IOException {
        // Write out element count, and any hidden stuff
        s.defaultWriteObject();

        s.writeObject(addr.getAddress().getHostAddress());
        s.writeInt(addr.getPort());
    }

    private void readObject(java.io.ObjectInputStream s)
    throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        
        String hostname = (String) s.readObject();
        int port = s.readInt();
        // addr = new InetSocketAddress(hostname, port);
        addr = AddressUtil.getAddress(hostname, port);
    }

    @Override
    public String getPeerNameCandidate() {
        return addr.getAddress().getHostAddress() + ":" + getPort();
    }
    
    @Override
    public void pack(ByteBuffer bbuf) {
        PeerLocator.putAddr(bbuf, addr);
    }
    
    @Override
    public int getPackLen() {
        return 6;
    }

    @Override
    public ServiceInfo getServiceInfo() {
    	return ServiceInfo.create(this.getClass().getName(), addr, null, "");
    }
}
