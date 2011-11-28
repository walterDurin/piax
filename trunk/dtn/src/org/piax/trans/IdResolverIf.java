/*
 * IdResolverIf.java
 * 
 * Copyright (c) 2008-2010 National Institute of Information and 
 * Communications Technology
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
 * 2011/02/10 designed and implemented by M. Yoshida.
 * 
 * $Id: IdResolverIf.java 358 2011-02-14 05:47:34Z yos $
 */
package org.piax.trans;

import java.util.List;
import java.util.Map;

import org.piax.trans.common.PeerId;
import org.piax.trans.common.PeerLocator;

/**
 * @author     Mikio Yoshida
 * @version    2.2.0
 */
public interface IdResolverIf {
    void fin();
    
    /**
     * Setup a locator for peerId.
     * Replace if a locator with same type already exists.
     * 
     * @param peerId
     * @param loc
     */
    void put(PeerId peerId, PeerLocator loc);
    
    /**
     * Add a locator for peerId.
     * If a locator with same type already exists, it is not removed. 
     *
     * @param peerId
     * @param loc
     * @return
     */
    boolean add(PeerId peerId, PeerLocator loc);

    /**
     * peerIdに対して、locatorを削除する。
     * 
     * @param peerId
     * @param loc
     * @return
     */
    boolean remove(PeerId peerId, PeerLocator loc);
    
    /**
     * Mapで指定されたすべてのidについてput(id, loc)を実行する。
     * XXX Is this needed?? I don't think so.
     * @param map
     */
    //void putAll(Map<? extends PeerId,? extends PeerLocator> map);
    
    List<PeerId> getPeerIds();
    
    /**
     * 指定されたPeerIdに対応するPeerLocatorを返す。
     * IdResolverの実装によって、複数のlocatorから最適のものを選択したり、
     * DHTの探索機能を使って、peer Idを探索して結果を返すものまである。
     * 
     * @param peerId
     * @return
     */
    PeerLocator getLocator(PeerId peerId);
    
    /**
     * 自ピアのlocatorが変化した際に、新しいlocatorを IdResolver が
     * 受理するためのメソッド。
     * 
     * @param newLoc 変化後のlocator
     */
    void acceptChange(PeerLocator newLoc);
}
