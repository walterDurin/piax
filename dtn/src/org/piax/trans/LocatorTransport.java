/*
 * LocatorTransport.java
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
 * 2007/10/25 designed and implemented by M. Yoshida.
 * 
 * $Id: LocatorTransport.java 225 2010-06-20 12:34:07Z teranisi $
 */

package org.piax.trans;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.grlea.log.DebugLevel;
import org.grlea.log.SimpleLogger;
import org.piax.gnt.handover.Peer;
import org.piax.gnt.handover.PeerManager;
import org.piax.trans.common.PeerLocator;
import org.piax.trans.msgframe.MessageReachable;
import org.piax.trans.msgframe.CallerHandle;
import org.piax.trans.msgframe.MessagingComponent;
import org.piax.trans.msgframe.MessagingRoot;
import org.piax.trans.msgframe.Session;
import org.piax.trans.stat.LinkStatAndScoreIf;
import org.piax.trans.stat.LinkStatistics;
import org.piax.trans.stat.Probable;
import org.piax.trans.stat.TrafficInfo;
import org.piax.trans.ts.BytesReceiver;
import org.piax.trans.ts.LocatorTransportSpi;
import org.piax.trans.ts.NestedLocator;
import org.piax.trans.util.ByteBufferUtil;
import org.piax.trans.util.ByteUtil;

//-- I am waiting for someone to translate the following doc into English. :-)

/**
 * Locatorベースのtransport機能を持つクラス。
 * 
 * @author     Mikio Yoshida
 * @version    2.2.0
 */
public class LocatorTransport extends MessagingRoot
        implements BytesReceiver, LocatorChangeObserver, HangupObserver {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(LocatorTransport.class);

    /**
     * スレッドプールの終了時にアクティブなスレッドの終了を待機する最長時間
     */
    public static final long MAX_WAIT_TIME_FOR_TERMINATION = 100L;

    private PeerLocator defaultLocator;
    private final List<LocatorTransportSpi> locTransList;
    private final ThreadedReceiver threadedReceiver;

    /** traffic計測用probe */
    //private final TrafficInfo traffic;
    private PeerManager peerStats;
    
    public static LinkStatAndScoreIf statAndScore = new StaticStatAndScore();
    
    public LocatorTransport(PeerLocator myLocator, Collection<PeerLocator> relays)
            throws IOException {
        super();
        // myLocator can be null at start time.
        // if (myLocator == null)
        //     throw new IllegalArgumentException("me should not be null");
        this.defaultLocator = myLocator;
        locTransList = new CopyOnWriteArrayList<LocatorTransportSpi>();
        if (myLocator != null)
        locTransList.add(myLocator.newLocatorTransportService(this, relays));
        threadedReceiver = new ThreadedReceiver(this);
    //    traffic = new TrafficInfo();
    }
    
    public void setPeerStatManager(PeerManager peerStats) {
        this.peerStats = peerStats;
    }
    
    /**
     * PeerLocatorを追加する。
     * 
     * @param me
     * @param relays
     * @throws IOException
     */
    public boolean addLocator(PeerLocator myLocator, Collection<PeerLocator> relays)
            throws IOException {
        if (myLocator == null)
            throw new IllegalArgumentException("me should not be null");
        // XXX defaultLocator should be changed to added one...?
        //        if (defaultLocator == null) {
        this.defaultLocator = myLocator;
        //        }
        synchronized (locTransList) {
            for (LocatorTransportSpi ts : locTransList) {
                // XXX sameClass...port is same.
                // address can be different...
                if (ts.canSet(myLocator)) {
                    ts.setLocator(myLocator);
                    acceptChange(ts, myLocator);
                    return false;
                }
            }
            LocatorTransportSpi ts = myLocator.newLocatorTransportService(this, relays);
            locTransList.add(0, ts);
            acceptChange(ts, myLocator);
        }
        return true;
    }

    /**
     * Clear all PeerLocators
     * 
     */
    public void clearLocators() {
        synchronized (locTransList) {
            for (LocatorTransportSpi ts : locTransList) {
                ts.fin();
            }
            locTransList.clear();
        }
        this.defaultLocator = null;
    }

    /**
     * Clear the specified PeerLocator
     * 
     */
    public void clearLocator(PeerLocator locator) {
        LocatorTransportSpi match = null;
        synchronized (locTransList) {
            for (LocatorTransportSpi ts : locTransList) {
                if (ts.getLocator().equals(locator)) {
                    match = ts;
                    break;
                }
            }
            if (match != null) {
            	match.fin();
            	locTransList.remove(match);
            }
        }
        if (locTransList.size() == 0) {
            this.defaultLocator = null;
        }
        else if (this.defaultLocator.equals(locator)) {
            this.defaultLocator = locTransList.get(0).getLocator();
        }
    }

    public TrafficInfo getTraffic() {
        return peerStats.getTraffic();
    }
    
    public LinkStatistics getLinkStatistics() {
        // FIXME
        return null;
    }

    public Probable getProbable() {
        for (MessagingComponent rec : getChildren()) {
            if (rec instanceof Probable) {
                return (Probable) rec;
            }
        }
        return null;
    }

    @Override
    public void fin() {
        for (LocatorTransportSpi ts : locTransList) {
            ts.fin();
        }
        super.fin();
        threadedReceiver.fin();
    }
    
    @Override
    public MessageReachable getLocalPeer() {
        return defaultLocator;
    }
    
    public LocatorTransportSpi getSameLocatorTransportService(PeerLocator target) {
        for (LocatorTransportSpi ts : locTransList) {
            if (ts.getLocator().sameClass(target)) {
                return ts;
            }
        }
        return null;
    }
    
    private boolean hasMatchedPeerLocator(PeerLocator target) {
        for (LocatorTransportSpi ts : locTransList) {
            if (ts.getLocator().equals(target)) {
                return true;
            }
        }
        return false;
    }

    public LocatorTransportSpi getApplicableLocatorTransportService(
            PeerLocator target) {
        // 完全に型が一致する
        LocatorTransportSpi ts = getSameLocatorTransportService(target);
        if (ts != null) return ts;
        // （途中までも含め）転送可能である
        for (LocatorTransportSpi ts2 : locTransList) {
            if (ts2.canSend(target)) {
                return ts2;
            }
        }
        return null;
    }

    public PeerLocator getLocator() {
        return defaultLocator;
    }

    public PeerLocator getLocator(Class<? extends PeerLocator> locatorType) {
        for (LocatorTransportSpi ts : locTransList) {
            if (ts.getLocator().getClass().equals(locatorType)) {
                return ts.getLocator();
            }
        }
        return null;
    }
    
    public List<PeerLocator> getLocators() {
        List<PeerLocator> locs = null;
        for (LocatorTransportSpi ts : locTransList) {
           if (locs == null) {
               locs = new ArrayList<PeerLocator>();
           }
           locs.add(ts.getLocator());
        }
        return locs;
    }

    static class WrappedCaller implements CallerHandle {
        final int sessionId;
        final PeerLocator srcPeer;
        final PeerLocator dstPeer;

        private WrappedCaller(int sessionId, PeerLocator srcPeer, 
                PeerLocator dstPeer) {
            this.sessionId = sessionId;
            this.srcPeer = srcPeer;
            this.dstPeer = dstPeer;
        }

        public MessageReachable getSrcPeer() {
            return srcPeer;
        }
    }
    
    /**
     * orgにより指定されたPeerLocatorをtsの送信先としてセットできる
     * PeerLocatorに変換する。変換できない場合はnullが返る。
     * 
     * @param org 変換元のPeerLocator
     * @param ts 設定対象のLocatorTransportService
     * @return tsの送信先としてセット可能なPeerLocator、
     *          変換できなかった場合はnull
     */
    private PeerLocator matchedPeerLocator(PeerLocator org, 
            LocatorTransportSpi ts) {
        PeerLocator tsLoc = ts.getLocator();
        if (org.sameClass(tsLoc)) return org;
        if (org instanceof NestedLocator<?,?>) {
            PeerLocator tmp = ((NestedLocator<?,?>) org).getOuterLocator();
            if (tmp.sameClass(tsLoc)) return tmp;
        }
        return null;
    }
    
    private int score(PeerLocator src, PeerLocator dst) {
        if (statAndScore == null) return 0;
        Integer[] e = statAndScore.eval(src, dst);
        return e[e.length - 1];
    }
    
    public PeerLocator bestRemoteLocator(Collection<PeerLocator> remoteLocs) {
        PeerLocator best = null;
        int bestScore = -1;
        for (PeerLocator loc : remoteLocs) {
            LocatorTransportSpi ts = getApplicableLocatorTransportService(loc);
            if (ts != null) {
                int score = score(ts.getLocator(), loc);
                //System.out.println("*** Score:" + score + "from:" + ts.getLocator() + " to locator=" + loc);
                if (score > bestScore) {
                    bestScore = score;
                    best = loc;
                }
            }
        }
        //System.out.println("*** BEST locator=" + best);
        return best;
    }
    

    private void forwardMsg(boolean isSend, PeerLocator toPeer, ByteBuffer msg)
            throws IOException {
        if (log.isTracing()) {
            log.entry("forwardMsg() " + msg.remaining() + "bytes");
            log.debugObject("=>", toPeer);
            log.debugObject("msg", ByteUtil.dumpBytes(msg));
        }
        try {
            LocatorTransportSpi ts;
            // LocatorTransportServiceを選択する
            ts = getApplicableLocatorTransportService(toPeer);
            if (ts != null) {
                ts.sendBytes(isSend, toPeer, msg);
                return;
            }
            // dstPeerがNestedLocatorの場合、outer locatorを使って再挑戦する
            if (toPeer instanceof NestedLocator<?,?>) {
                toPeer = ((NestedLocator<?,?>) toPeer).getOuterLocator();
                ts = getApplicableLocatorTransportService(toPeer);
                if (ts != null) {
                    ts.sendBytes(isSend, toPeer, msg);
                    return;
                }
            }
            // メッセージの転送先がなかった状態
            throw new IOException("unreachable dst locator: " + toPeer);
        } finally {
            log.exit("forwardMsg()");
        }
    }
    
    // get appropriate src locator for locator 'dest'.
    private PeerLocator correspondingLocator(PeerLocator dest) {
        PeerLocator ret = defaultLocator;
        LocatorTransportSpi ts = this.getApplicableLocatorTransportService(dest);
        if (ts != null) {
            ret = ts.getLocator();
        }
        return ret;
    }
    
    @Override
    protected void send(Session session, MessageReachable toPeer, ByteBuffer msg)
        throws IOException {
        log.entry("send()");
        if (log.wouldLog(DebugLevel.L5_DEBUG))
            log.debug("send to " + toPeer + ", msg=" + msg.remaining() + " bytes");
        try {
            PeerLocator dstPeer = (PeerLocator) toPeer;
            int sessionId = 0;
            if (session != null) {
                sessionId = newSessionId(session);
                if (sessionId < 0) {
                    throw new IOException("session overflow");
                }
            }
            if (dstPeer == null) {
                throw new IOException("dst peer is unknown");
            }

            if (defaultLocator == null) {
                throw new IOException("locator is not set yet");
            }
            // probe
            if (peerStats != null) {
                peerStats.putSend(msg.remaining(), dstPeer);
            }

            MsgHeader header = new MsgHeader(true, sessionId, correspondingLocator(dstPeer),
                    dstPeer, System.currentTimeMillis());
            if (header == null) {
                throw new IOException("message header cannot be created");
            }
            MsgHeader.concat(header, msg);
            if (log.wouldLog(DebugLevel.L5_DEBUG))
                log.debug(header.toString());
            try {
                forwardMsg(true, dstPeer, msg);
            }
            catch (IOException e) {
                if (peerStats != null) {
                    if (!isMyLocator(dstPeer)) {
                        peerStats.setLocatorStatus(dstPeer, Peer.TransportStateLinkFailure);
                    }
                }
                throw e;
            }
        } finally {
            log.exit("send()");
        }
    }

    @Override
    protected void reply(CallerHandle caller, ByteBuffer msg)
            throws IOException {
        log.entry("reply()");
        if (log.wouldLog(DebugLevel.L5_DEBUG))
            log.debug("reply to " + caller + ", msg=" + msg.remaining() + " bytes");
        try {
            WrappedCaller wCaller = (WrappedCaller) caller;
            /*
             * TODO 
             * timestamp のセットは便宜的
             */
            MsgHeader header = new MsgHeader(false, wCaller.sessionId,
                    wCaller.srcPeer, wCaller.dstPeer, System.currentTimeMillis());
            MsgHeader.concat(header, msg);
            if (log.wouldLog(DebugLevel.L5_DEBUG))
                log.debug(header.toString());
            forwardMsg(false, wCaller.srcPeer, msg);
        } finally {
            log.exit("reply()");
        }
    }

    public PeerLocator getFromPeer(LocatorTransportSpi caller, ByteBuffer msg) {
        return matchedPeerLocator(MsgHeader.extractSrcPeer(msg), caller);
    }
    
    private boolean isMyLocator(PeerLocator locator) {
        return getLocators().contains(locator);
    }

    @Override
    public void receiveBytes(LocatorTransportSpi caller, ByteBuffer msg) {
        if (log.isTracing()) {
            log.entry("receiveBytes() " + msg.remaining() + "bytes");
            log.debug("from " + caller.getClass().getSimpleName());
            log.debugObject("msg", ByteUtil.dumpBytes(msg));
        }
        try {
                     
            // save position
            int msgOff = msg.position();
            MsgHeader header = MsgHeader.strip(msg);
            if (log.wouldLog(DebugLevel.L5_DEBUG))
                log.debug(header.toString());
            // probe
            if (peerStats != null) {
                peerStats.putReceive(msg.remaining(), header.srcPeer);
                if (!isMyLocator(header.srcPeer) && header.srcPeer.immediateLink()) {
                    peerStats.setLocatorStatus(header.srcPeer, Peer.TransportStateLinked);
                }
            }
            
            if (header.isSend()) {
                if (hasMatchedPeerLocator(header.dstPeer)) {
                    // WrappedCallerを作る
                    WrappedCaller wcaller = new WrappedCaller(header.getSessionId(),
                            header.srcPeer, header.dstPeer);
                    try {
                        threadedReceiver.doThreadedReceive(wcaller, msg, header.getSessionId());
                    }
                    catch (Exception e) {
                        // In case 'RejectedExecutionException' occurs
                        log.warnException(e);
                    }
                } else {
                    // 自ピアが送信先でないため、転送を行う
                    // 転送時の例外については、下位に返す意味がないため無視する
                    if (log.wouldLog(DebugLevel.L5_DEBUG))
                        log.debug("relay SEND : " + header);
                    try {
                        // restore position to msg
                        ByteBufferUtil.reset(msgOff, msg);
                        forwardMsg(true, header.dstPeer, msg);
                    } catch (IOException e) {
                        log.warnException(e);
                    }
                }
            } else {
                if (hasMatchedPeerLocator(header.srcPeer)) {
                    // Sessionを戻す
                    Session session = getSession(header.getSessionId());
                    if (session != null) {
                        try {
                            threadedReceiver.doThreadedReceiveReply(msg, session);
                        }
                        catch (Exception e) {
                            // In case 'RejectedExecutionException' occurs
                            log.warnException(e);
                        }
                    } else {
                        log.info("receiveReply purged as timeout");
                    }
                } else {
                    if (log.wouldLog(DebugLevel.L5_DEBUG))
                        log.debug("relay REPLY : " + header);
                    // 自ピアが送信先でないため、転送を行う
                    // 転送時の例外については、下位に返す意味がないため無視する
                    try {
                        // restore position to msg
                        ByteBufferUtil.reset(msgOff, msg);
                        forwardMsg(false, header.srcPeer, msg);
                    } catch (IOException e) {
                        log.warnException(e);
                    }
                }
            }
        } finally {
            log.exit("receiveBytes()");
        }
    }
    
    @Override
    public void locatorUnavailable(PeerLocator locator) {
        if (peerStats != null) {
            peerStats.setLocatorStatus(locator, Peer.TransportStateUnavailable);
        }
    }
    
    @Override
    public void locatorAvailable(PeerLocator locator) {
        if (peerStats != null) {
            peerStats.setLocatorStatus(locator, Peer.TransportStateLinked);
        }
    }

    public void acceptChange(LocatorTransportSpi locTrans, PeerLocator newLoc) {
        // defaultLocator と関係しないなら return
        if ((locTransList.size() == 0) || !locTransList.get(0).equals(locTrans)) return;
        defaultLocator = newLoc;
        for (MessagingComponent rec : getChildren()) {
            if (rec instanceof IdTransport) {
                IdResolverIf resolver = ((IdTransport) rec).getIdResolver();
                if (resolver != null)
                    resolver.acceptChange(newLoc);
                break;
            }
        }
    }

    public void acceptFadeout(LocatorTransportSpi locTrans) {
        // defaultLocator と関係しないなら return
        if (!locTransList.get(0).equals(locTrans)) return;
        defaultLocator = null;
    }

    public void acceptHangup(LocatorTransportSpi locTrans, Throwable cause) {
        // TODO
    }
}
