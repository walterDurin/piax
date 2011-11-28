/*
 * DumbIdResolver.java
 * 
 * Copyright (c) 2008- National Institute of Information and 
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
 * 2009/05/26 designed and implemented by M. Yoshida.
 * 
 * $Id: DumbIdResolver.java 218 2010-05-19 03:51:55Z teranisi $
 */

package org.piax.trans;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.grlea.log.SimpleLogger;
import org.piax.trans.common.PeerId;
import org.piax.trans.common.PeerLocator;
import org.piax.trans.msgframe.MagicNumberConflictException;

/**
 * 与えられた情報を使ってID解決をするだけのIdResolver。
 * 未知のIDを隣接ノードから探索する機能は持たない。
 * 
 * @author     Mikio Yoshida
 * @version    2.2.0
 */
@Deprecated
public class DumbIdResolver implements IdResolverIf {
	/*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(DumbIdResolver.class);

    /** DumbIdResolver を特定させるためのmagic number */
    static final byte[] ID_RES_MAGIC = {(byte) 0x55};

    public DumbIdResolver(byte[] magic, LocatorTransport locTrans)
	throws MagicNumberConflictException {
    	//super(magic, null, locTrans);
    	isActive = true;
    }
    public static void newAndBindTo(IdTransport trans) {
        DumbIdResolver res;
		try {
			res = new DumbIdResolver(ID_RES_MAGIC, trans.getLocatorTransport());
	        trans.setIdResolver(res);
	        res.activate();
		} catch (MagicNumberConflictException e1) {
        } catch (IOException e) {
        }  
    }
    
    private boolean isActive;

    /* (non-Javadoc)
     * @see org.piax.trans.IdResolver#acceptChange(org.piax.trans.PeerLocator)
     */
    @Override
    public void acceptChange(PeerLocator newLoc) {
        // ピアが移動するなどして、PeerLocatorが変化したことを受け取るための
        // メソッド。
        // この例では実装する必要はない。
    }

    /* (non-Javadoc)
     * @see org.piax.trans.IdResolver#activate()
     */
    //@Override
    public boolean activate() throws IOException {
        isActive = true;
        return true;
    }

    /* (non-Javadoc)
     * @see org.piax.trans.IdResolver#activate(java.util.Collection)
     */
    //@Override
    public boolean activate(Collection<PeerLocator> seeds) throws IOException {
        isActive = true;
        return true;
    }

    /* (non-Javadoc)
     * @see org.piax.trans.IdResolver#inactivate()
     */
//    @Override
    public boolean inactivate() throws IOException {
        isActive = false;
        return true;
    }

    /* (non-Javadoc)
     * @see org.piax.trans.IdResolver#isActive()
     */
//    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public void fin() {
        // TODO Auto-generated method stub
        
    }
//	@Override
	protected PeerLocator lookupRemoteLocator(PeerId peerId) {
		// TODO Auto-generated method stub
		return null;
	}
    @Override
    public boolean add(PeerId peerId, PeerLocator loc) {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public PeerLocator getLocator(PeerId peerId) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public List<PeerId> getPeerIds() {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public void put(PeerId peerId, PeerLocator loc) {
        // TODO Auto-generated method stub
        
    }
    @Override
    public boolean remove(PeerId peerId, PeerLocator loc) {
        // TODO Auto-generated method stub
        return false;
    }

}
