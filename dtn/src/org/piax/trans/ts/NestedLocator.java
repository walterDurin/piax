/*
 * NestedLocator.java
 * 
 * Copyright (c) 2008 National Institute of Information and 
 * Communications Technology
 * Copyright (c) 2006 Osaka University
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
 * 2009/02/02 designed and implemented by M. Yoshida.
 * 
 * $Id: NestedLocator.java 210 2010-05-07 14:09:41Z teranisi $
 */

package org.piax.trans.ts;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;

import org.piax.trans.common.PeerLocator;
import org.piax.trans.common.ServiceInfo;

/**
 * @author     Mikio Yoshida
 * @version    2.1.0
 */
public class NestedLocator<O extends PeerLocator, I extends PeerLocator>
extends PeerLocator {
    private static final long serialVersionUID = -6265758405730688499L;
    public static final byte ID = 20;

    protected final O outer;
    protected final I inner;
    protected I gateway;
    
    // new the locator of outer side peer
    public NestedLocator(O outer, I inner) {
        this(outer, inner, inner);
    }

    // new the locator of inner side peer
    public NestedLocator(O outer, I inner, I gateway) {
        if (outer == null || inner == null)
            throw new IllegalArgumentException("argument should not be null");
        this.outer = outer;
        this.inner = inner;
        this.gateway = gateway;
    }

    public boolean isOuter() {
        return inner.equals(gateway);
    }
    
    @Override
    public LocatorTransportSpi newLocatorTransportService(
            BytesReceiver bytesReceiver,
            Collection<PeerLocator> relays) throws IOException {
        return new NestedTransportService<O,I>(bytesReceiver, this, relays);
    }

    public O getOuterLocator() {
        return outer;
    }

    public I getInnerLocator() {
        return inner;
    }
    
    public PeerLocator getLocalLocator() {
        return isOuter() ? outer : inner;
    }

    @Override
    public boolean sameClass(PeerLocator target) {
        if (target == null) return false;
        boolean c = this.getClass().equals(target.getClass());
        if (!c) return false;
        NestedLocator<?,?> tar = (NestedLocator<?,?>) target;
        boolean o = this.outer.getClass().equals(tar.outer.getClass());
        if (!o) return false;
        boolean i = this.inner.getClass().equals(tar.inner.getClass());
        return i;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof NestedLocator<?,?>)) {
            return false;
        }
        NestedLocator<?,?> _obj = (NestedLocator<?,?>) obj;
        boolean o = (outer == null) ? (_obj.outer == null) :
            outer.equals(_obj.outer);
        if (!o) return false;
        boolean i = (inner == null) ? (_obj.inner == null) :
            inner.equals(_obj.inner);
        if (!i) return false;
        boolean g = (gateway == null) ? (_obj.gateway == null) :
            gateway.equals(_obj.gateway);
        return g;
    }
    
    @Override
    public int hashCode() {
        int o = (outer == null) ? 0 : outer.hashCode();
        int i = (inner == null) ? 0 : inner.hashCode();
        int g = (gateway == null) ? 0 : gateway.hashCode();
        return o ^ i ^ g;
    }

    @Override
    public String toString() {
        String g = gateway == null ? "" : "=" + gateway;
        String i = inner == null ? "" : inner.toString();
        return String.format("(%s%s!%s)", outer, g, i);
    }

    @Override
    public String getPeerNameCandidate() {
        return getLocalLocator().getPeerNameCandidate();
    }

    @Override
    public byte getId() {
        return ID;
    }

    @Override
    public void pack(ByteBuffer bbuf) {
        // TODO
    }

    @Override
    public int getPackLen() {
        // TODO
        return 0;
    }
    
    @Override
    // XXX What is the right implementation?
    public ServiceInfo getServiceInfo() {
    	return inner.getServiceInfo();
    }
}
