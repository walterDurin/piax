/*
 * TrafficInfo.java
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
 * 2009/02/15 designed and implemented by M. Yoshida.
 * 
 * $Id: TrafficInfo.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans.stat;

import java.io.Serializable;
import java.util.Arrays;

import org.piax.trans.common.PeerLocator;

/**
 * traffic情報収集用のクラス。
 * <p>
 * TODO
 * LocatorTransportに差し込む用に作成したが、Locatorが変化することを
 * 考えると、本来は、IdTransportに差し込むべきだったかもしれない。
 * 尚、IdTransportから計測した場合、メッセージ長は多少短くなる。
 * LocatorTransportから計測する場合でも、LocatorTransportServiceで
 * 送受信される実際のbyte数は解らないので、メッセージ長の正確さに
 * おいては、両者にそう差はないと考える。
 * 
 * @author     Mikio Yoshida
 * @version    2.1.0
 */
public class TrafficInfo implements Serializable {
    private static final long serialVersionUID = 5737570628857686780L;
    
    static final int MEASURE_SLOTS = 61;        // 1時間分
    static final int SLOT_UNIT_MILLIS = 60 * 1000;  // 1分

    public long upTime;
    transient private int[] msgNumSlot;    // 1分単位のmsg数蓄積slot
    transient private int slotIx;          // 現在使用しているslotのindex
    transient private long slotTimeMillis; // 現在使用しているslotの開始時刻
    
    public int sendTotalSize = 0;
    public int sendNum = 0;
    public int receiveTotalSize = 0;
    public int receiveNum = 0;
    public int forwardQueryHops = 0;
    transient private Probable probe = null;
    // TODO tmp code
    public int expandQueryHops = 0;
    public int forwardQueryToMaxLessThan = 0;
    
    public TrafficInfo() {
        upTime = System.currentTimeMillis();
        msgNumSlot = new int[MEASURE_SLOTS];
        slotIx = 0;
        slotTimeMillis = upTime / SLOT_UNIT_MILLIS * SLOT_UNIT_MILLIS;
    }
    
    public void setProbable(Probable probe) {
        this.probe = probe;
    }
    
    public void reset() {
        sendTotalSize = 0;
        sendNum = 0;
        receiveTotalSize = 0;
        receiveNum = 0;
        forwardQueryHops = 0;
    }
    
    private void updateSlotIx() {
        long newSlotTimeMillis = System.currentTimeMillis() / SLOT_UNIT_MILLIS 
        * SLOT_UNIT_MILLIS;
        int diffIx = (int) ((newSlotTimeMillis - slotTimeMillis) / SLOT_UNIT_MILLIS);
        if (diffIx >= MEASURE_SLOTS) {
            // リング状のslotを一周以上した
            Arrays.fill(msgNumSlot, 0);
        } else {
            // 経過した時間のslotは0にする
            for (int i = slotIx + 1; i <= slotIx + diffIx; i++) {
                msgNumSlot[i % MEASURE_SLOTS] = 0;
            }
        }
        slotIx = (slotIx + diffIx) % MEASURE_SLOTS;
        slotTimeMillis = newSlotTimeMillis;
    }
    
    public void putSend(int msgSize, PeerLocator to) {
        synchronized (msgNumSlot) {
            // slotへの足し込み
            updateSlotIx();
            msgNumSlot[slotIx]++;
        }
        sendNum++;
        sendTotalSize += msgSize;
        if (probe != null) {
            probe.sendFlow(msgSize, to);
        }
    }
    
    public void putReceive(int msgSize, PeerLocator from) {
        receiveNum++;
        receiveTotalSize += msgSize;
        // XXX Should take 'from' into account.
    }

    /**
     * 生存時間をmsecで返す。
     * 
     * @return 生存時間（msec）
     */
    public long getAliveTime() {
        return System.currentTimeMillis() - upTime;
    }
    
    /**
     * 過去 mins分間にこのピアが送信したメッセージ数を返す。
     * 
     * @param mins 時間間隔（min）
     * @return 過去 mins分間に送信したメッセージ数
     */
    public int getCurrentSendMsgNum(int mins) {
        if (mins >= MEASURE_SLOTS) mins = MEASURE_SLOTS - 1;
        int num;
        synchronized (msgNumSlot) {
            updateSlotIx();
            num = msgNumSlot[slotIx];
            for (int i = 1; i <= mins; i++) {
                num += msgNumSlot[(slotIx - i + MEASURE_SLOTS) % MEASURE_SLOTS];
            }
        }
        return num;
    }
    
    @Override
    public String toString() {
        return String.format("#send: %,d av: %,dbyte%n" +
        		"#recv: %,d av: %,dbyte%n", sendNum,
        		(sendNum == 0) ? 0 : sendTotalSize / sendNum,
        		receiveNum, 
        		(receiveNum == 0) ? 0 : receiveTotalSize / receiveNum);
    }
    
    public static void main(String[] args) throws Exception {
//        System.out.printf("#send: %,d av: %,dByte%n", 123456789, 9876);
        TrafficInfo info = new TrafficInfo();

        Thread.sleep(7000);
        for (int i = 0; i < 50; i++) {
            Thread.sleep(100);
            info.putSend(1, null);
        }
        Thread.sleep(4000);
        System.out.println(info.getCurrentSendMsgNum(10));
    }
}
