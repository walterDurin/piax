/*
 * LinkStatistics.java
 * 
 * Copyright (c) 2008-2010 National Institute of Information and 
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
 * 2009/03/18 designed and implemented by M. Yoshida.
 * 
 * $Id: LinkStatistics.java 457 2009-04-17 00:27org.piax.ctrl*/

package org.piax.trans.stat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.piax.trans.common.PeerLocator;
import org.piax.trans.ts.udpx.UdpXLocator;

/**
 * @author     Mikio Yoshida
 * @version    2.1.0
 */
public class LinkStatistics {

    /*
     * TODO コードの整理が必要
     */
    static final int MAX_SAMPLING_NUM = 10000;
    static class LinkInfo {
        int rttNum = 0;
        long rttMean = 0L;      // 平均（nano秒）
        long rttSquareSum = 0L; // 平方和（nano秒）
        int errNum = 0;
        int aliveTime = 0;       // msec
        int currSendMsgs = 0;
        
        private long deviation() {
            return (rttNum - 1 <= 0) ? 0 : 
                (long) Math.sqrt(rttSquareSum / (rttNum - 1));
        }
        
    }
    
    private final Map<PeerLocator, LinkInfo> stats; 
    public LinkStatistics() {
        stats = new ConcurrentHashMap<PeerLocator, LinkInfo>();
    }
    
    private LinkInfo getInfo(PeerLocator peer) {
        LinkInfo info = stats.get(peer);
        if (info == null) {
            info = new LinkInfo();
            stats.put(peer, info);
        }
        return info;
    }
    
    public void setAliveTimeAndCurrentSendMsgNum(PeerLocator peer, long[] vals) {
        LinkInfo info = getInfo(peer);
        info.aliveTime = (int) vals[0];
        info.currSendMsgs = (int) vals[1];
    }

    public void addRTT(PeerLocator peer, long rtt) {
        LinkInfo info = getInfo(peer);
        if (rtt == -1) {
            info.errNum++;
            return;
        }
        if (info.rttNum < MAX_SAMPLING_NUM) info.rttNum++;
        long x = rtt - info.rttMean;
        info.rttMean += x / info.rttNum;
        info.rttSquareSum += (info.rttNum - 1) * x * x / info.rttNum;
    }
    
    /**
     * peerへの通信について、回数、RTTの平均、標準偏差、エラー回数と
     * peerの稼働時間、メッセージ処理数を返す。
     * 
     * @param peer 通信相手ピア
     * @return 通信回数、RTTの平均、標準偏差、エラー回数、稼働時間、メッセージ処理数
     */
    public long[] getStatistics(PeerLocator peer) {
        LinkInfo info = getInfo(peer);
        // 標準偏差（nano秒）
        long deviation = info.deviation();
        return new long[] {info.rttNum + info.errNum, info.rttMean, deviation, 
                info.errNum, info.aliveTime, info.currSendMsgs};
    }

    public String getStatisticsString(PeerLocator peer) {
        long[] vals = getStatistics(peer);
        return String.format(
                "#msg:%,d, rtt-m(ms):%.1f, rtt-d(ms):%.1f, #err:%,d, " +
                "alive(min):%d, #sendmsgs/min:%.2f", 
                vals[0], vals[1] / 1000000.0, vals[2] / 1000000.0, vals[3], 
                vals[4] / 60000, vals[5] / 30.0
                );
    }
    
    public int getTimeout(PeerLocator peer) {
        LinkInfo info = getInfo(peer);
        long deviation = info.deviation();
        return (int) ((info.rttMean + 4 * deviation) / 1000000);
    }
    
    public float getErrRatio(PeerLocator peer) {
        LinkInfo info = getInfo(peer);
        return (float) info.errNum / (info.rttNum + info.errNum);
    }
    
    public int getAliveTime(PeerLocator peer) {
        LinkInfo info = getInfo(peer);
        return info.aliveTime;
    }

    public int getCurrSendMsgs(PeerLocator peer) {
        LinkInfo info = getInfo(peer);
        return info.currSendMsgs;
    }
    
    public List<PeerLocator> getGlobalUdpXLocators() {
        List<PeerLocator> globals = new ArrayList<PeerLocator>();
        for (PeerLocator loc : stats.keySet()) {
            if (!(loc instanceof UdpXLocator)) continue;
            if (((UdpXLocator) loc).isGlobal()) {
                globals.add(loc);
            }
        }
        return globals;
    }
}
