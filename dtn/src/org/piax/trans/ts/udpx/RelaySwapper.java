/*
 * RelaySwapper.java
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
 * 2009/03/19 designed and implemented by M. Yoshida.
 * 
 * $Id: RelaySwapper.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans.ts.udpx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;
import java.util.TreeMap;

import org.grlea.log.SimpleLogger;
import org.piax.trans.LocatorTransport;
import org.piax.trans.common.PeerLocator;
import org.piax.trans.stat.Probable;

/**
 * @author     Mikio Yoshida
 * @version    2.1.0
 */
class RelaySwapper extends TimerTask {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(RelaySwapper.class);
    
    static int CANDS_NUM = 8;
    static int CHECK_MINUTES = 30;
    static float ALLOWED_ERR_RATIO = 0.05f;
    static int ALIVE_TIME_MIN = 60 * 60 * 1000;
    static int CURR_SEND_MSG_NUM_MAX = 300 * CHECK_MINUTES;
    
    private final UdpXTransportService transService;
    private LocatorTransport trans = null;

    RelaySwapper(UdpXTransportService transService) {
        this.transService = transService;
    }

    /**
     * 性能の良い順に並べ直す。
     * 並べ直す際に、エラー率が許容範囲にない候補は削除する。
     * 
     * @param globals 元のPeerLocatorリスト
     * @return 性能順に並べ直したPeerLocatorリスト
     */
    private List<PeerLocator> ordering(Collection<PeerLocator> globals) {
        TreeMap<Integer, PeerLocator> cands = new TreeMap<Integer, PeerLocator>();
        for (PeerLocator peer : globals) {
            int timeout = trans.getLinkStatistics().getTimeout(peer);
            float errRatio = trans.getLinkStatistics().getErrRatio(peer);
            // 5%以上のエラー率は相手にしない
            if (errRatio > ALLOWED_ERR_RATIO) continue;
            // 評価式。1%のエラーを10秒のハンディとして算出
            int eval = (int) (errRatio * 1000000) + timeout;
            cands.put(eval, peer);
        }
        return new ArrayList<PeerLocator>(cands.values());
    }
    
    /**
     * 指定された数だけグローバルなUdpXLocatorを集める。
     * 但し、情報収集する範囲は自ピアの知っているグローバルなUdpXLocatorから
     * 直接得られるホップ範囲まで。
     * 
     * @param candNum グローバルなUdpXLocatorの収集目標数
     * @return 収集したグローバルなUdpXLocatorのセット
     */
    private Set<PeerLocator> getCands(int candNum) {
    	Probable probe = trans.getProbable();
        // 自ピアが知っているグローバルなUdpXLocatorを集める
        List<PeerLocator> g = 
            ordering(trans.getLinkStatistics().getGlobalUdpXLocators());
        Set<PeerLocator> cands = new HashSet<PeerLocator>(g);
        for (PeerLocator p : g) {
            // 予定数に達したら聞くのをやめる
            if (cands.size() > candNum) break;
            try {
                // 予定数に満たない分を聞く
                cands.addAll(probe.getGlobalUdpXLocators(p, candNum - cands.size()));
            } catch (Exception ignore) {
            }
        }
        return cands;
    }

    private List<PeerLocator> ordering2(Collection<PeerLocator> globals) {
        TreeMap<Integer, PeerLocator> cands = new TreeMap<Integer, PeerLocator>();
        for (PeerLocator peer : globals) {
            int timeout = trans.getLinkStatistics().getTimeout(peer);
            float errRatio = trans.getLinkStatistics().getErrRatio(peer);
            int aliveTime = trans.getLinkStatistics().getAliveTime(peer);
            int currSendMsgs = trans.getLinkStatistics().getCurrSendMsgs(peer);
            // 5%以上のエラー率は相手にしない
            if (errRatio > ALLOWED_ERR_RATIO) continue;
            // 1時間未満の稼働のピアは相手にしない
            if (aliveTime < ALIVE_TIME_MIN) continue;
            // 1分あたり、300メッセージを越えている送信をしているピアは相手にしない
            if (currSendMsgs > CURR_SEND_MSG_NUM_MAX) continue;
            // 評価式。1%のエラーを10秒のハンディとして算出
            int eval = (int) (errRatio * 1000000) + timeout;
            cands.put(eval, peer);
        }
        return new ArrayList<PeerLocator>(cands.values());
    }

    private List<PeerLocator> getBests(int candNum) {
    	Probable probe = trans.getProbable();
    	Set<PeerLocator> cands = getCands(candNum);
        // 候補のピアについて、稼働時間と直近の送信メッセージ数を収集する
        // この処理によって、未送信のピアのリンク性能も収集する
        for (PeerLocator peer : cands) {
            try {
                long[] r = probe.getAliveTimeAndCurrentSendMsgNum(peer, CHECK_MINUTES);
                      trans.getLinkStatistics().setAliveTimeAndCurrentSendMsgNum(peer, r);
            } catch (Exception ignore) {
            }
        }
        // 性能順に並べ直す
        List<PeerLocator> bestPeers = ordering2(cands);
        // 性能順に内容を出力する（デバッグ）
        log.debug("### link staticstics ###");
        for (PeerLocator peer : bestPeers) {
            String stat = trans.getLinkStatistics().getStatisticsString(peer);
            log.debug(peer + ":: " + stat);
        }
        return bestPeers;
    }
    
    @Override
    public void run() {
        trans = transService.getLocatorTransport();
        if (trans == null) return;
        List<PeerLocator> bests = getBests(CANDS_NUM);
        if (bests.size() == 0) return;
        UdpXLocator me = (UdpXLocator) trans.getLocator();
        UdpXLocator cand1 = (UdpXLocator) bests.get(0);
        UdpXLocator cand2 = (bests.size() < 2) ? null : (UdpXLocator) bests.get(1);
        if (me.global.equals(cand1.global)
                && (me.global2 == null ? cand2 == null : 
                    me.global2.equals(cand2 == null ? null : cand2.global))) {
            // 変更なし
            return;
        }
        log.debug("1st relay changed. " + me.global + " -> %s%n" + 
                (cand1 == null ? null : cand1.global));
        log.debug("2nd relay changed. " + me.global + " -> %s%n" +
                (cand2 == null ? null : cand2.global));
        me.global = cand1.global;
        me.global2 = (cand2 == null ? null : cand2.global);
        transService.connMgr.getConnection(me.global, true);
        if (me.global2 != null) {
            transService.connMgr.getConnection(me.global2, true);
        }
        transService.acceptChange(me);
    }
}
