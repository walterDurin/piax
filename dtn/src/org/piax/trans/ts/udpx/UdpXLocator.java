/*
 * UdpXLocator.java
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
 * 2007/11/11 designed and implemented by M. Yoshida.
 * 
 * $Id: UdpXLocator.java 210 2010-05-07 14:09:41Z teranisi $
 */

package org.piax.trans.ts.udpx;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collection;

import org.piax.trans.common.PeerLocator;
import org.piax.trans.common.ServiceInfo;
import org.piax.trans.ts.BytesReceiver;
import org.piax.trans.ts.LocatorTransportSpi;

/**
 * 
 * <p>
 * globalやprivateの個々のアドレスについては、必ず外部からアクセスでき、ピア
 * を一意に特定できるアドレスをセットしないといけない。
 * localhostや127.0.0.1等のloopbackアドレスをセットした場合は、
 * 同一の <code>UdpXLocator</code> を異なるlocatorと間違える可能性が生じる。
 * <p>
 * 尚、ここでは、encodingの関係で、IPv4 を前提としている。
 * 
 * @author     Mikio Yoshida
 * @version    2.1.0
 */
public class UdpXLocator extends PeerLocator {
    private static final long serialVersionUID = 6199995214625137843L;
    public static final byte ID = 30;

    InetSocketAddress global = null;
    InetSocketAddress global2 = null;
    InetSocketAddress nat = null;
    InetSocketAddress privateAddr = null;
    
    UdpXLocator() {}

    public UdpXLocator(InetSocketAddress global) {
        if (global == null)
            throw new IllegalArgumentException("argument should not be null");
        this.global = global;
    }

    public UdpXLocator(InetSocketAddress global, InetSocketAddress privateAddr) {
        if (privateAddr == null)
            throw new IllegalArgumentException("argument should not be null");
        this.global = global;
        this.privateAddr = privateAddr;
    }

    public UdpXLocator(ByteBuffer bbuf) throws UnknownHostException {
        global = PeerLocator.getAddr(bbuf);
        global2 = PeerLocator.getAddr(bbuf);
        nat = PeerLocator.getAddr(bbuf);
        privateAddr = PeerLocator.getAddr(bbuf);
    }
    
    public void setNatAddress(InetSocketAddress nat) {
        this.nat = nat;
    }

    @Override
    public LocatorTransportSpi newLocatorTransportService(
            BytesReceiver bytesReceiver, Collection<PeerLocator> relays)
            throws IOException {
        return new UdpXTransportService(bytesReceiver, this, relays);
    }

    public boolean isGlobal() {
        return privateAddr == null;
    }
    
    public InetSocketAddress getLocalAddress() {
        return isGlobal() ? global : privateAddr;
    }
    
    public InetSocketAddress getRelayAddress() {
        return isGlobal() ? null : global;
    }
    
    public InetSocketAddress getGlobalAddress() {
        return isGlobal() ? global : nat;
    }

    /*
     * TODO
     */
    public boolean sameSite(UdpXLocator loc) {
        return (global == null && loc.global == null
                && nat == null && loc.nat == null) ||
                (nat != null && loc.nat != null 
                        && nat.getAddress().equals(loc.nat.getAddress()));
    }

    @Override
    public boolean sameClass(PeerLocator target) {
        if (target == null) return false;
        return this.getClass().equals(target.getClass());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof UdpXLocator)) {
            return false;
        }
        UdpXLocator _obj = (UdpXLocator) obj;
        boolean g = (global == null) ? (_obj.global == null) :
            global.equals(_obj.global);
        if (!g) return false;
        boolean g2 = (global2 == null) ? (_obj.global2 == null) :
            global2.equals(_obj.global2);
        if (!g2) return false;
        boolean n = (nat == null) ? (_obj.nat == null) :
            nat.equals(_obj.nat);
        if (!n) return false;
        boolean p = (privateAddr == null) ? (_obj.privateAddr == null) :
            privateAddr.equals(_obj.privateAddr);
        return p;
    }
    
    @Override
    public int hashCode() {
        int g = (global == null) ? 0 : global.hashCode();
        int g2 = (global2 == null) ? 0 : global2.hashCode();
        int n = (nat == null) ? 0 : nat.hashCode();
        int p = (privateAddr == null) ? 0 : privateAddr.hashCode();
        return g ^ g2 ^ n ^ p;
    }
    
    private String toString(InetSocketAddress addr) {
        if (addr == null) return "";
        return addr.getAddress().getHostAddress() + ":" + addr.getPort();
    }
    
    @Override
    public String toString() {
        return "[" + toString(global) + "!" + toString(global2)  
            + "!" + toString(nat) + "!" + toString(privateAddr) + "]";
    }

    @Override
    public String getPeerNameCandidate() {
        return "p" + getLocalAddress().getPort();
    }

    @Override
    public byte getId() {
        return ID;
    }

    @Override
    public int getPackLen() {
        return 4 * 6;
    }

    @Override
    public void pack(ByteBuffer bbuf) {
        PeerLocator.putAddr(bbuf, global);
        PeerLocator.putAddr(bbuf, global2);
        PeerLocator.putAddr(bbuf, nat);
        PeerLocator.putAddr(bbuf, privateAddr);
    }
    
    public ServiceInfo getServiceInfo() {
    	return ServiceInfo.create (this.getClass().getName(), getGlobalAddress(), getPeerNameCandidate(), "");
    }
}
