/*
 * Fragments.java
 * 
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
 * 2008/11/26 designed and implemented by M. Yoshida.
 * 
 * $Id: Fragments.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans.ts.udpx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.grlea.log.SimpleLogger;

/**
 * MTUサイズに収まるよう分割されたUDPメッセージを再構成するためのクラス。
 * 
 * @author     Mikio Yoshida
 * @version    2.0.1
 */
class Fragments {
    private static final SimpleLogger log = new SimpleLogger(Fragments.class);
    private final Map<String, List<byte[]>> msgFragsMap; 
    
    /*
     * TODO for packet loss and miss order
     * 最後のパケットが到着した時点で、通信エラーを判断する点もやや
     * 甘いところがある。（getMsgの呼び出し処理において）
     */
    Fragments() {
        msgFragsMap = new HashMap<String, List<byte[]>>();
    }
    
    private void set(List<byte[]> list, int idx, byte[] data) {
        for (int i = list.size(); i <= idx; i++) {
            list.add(null);
        }
        list.set(idx, data);
    }
    
    private int byteLength(List<byte[]> list) {
        if (list == null) return -1;
        int cnt=0;
        int len = 0;
        for (byte[] ele : list) {
            cnt++;
            if (ele == null) {
                log.warn(String.format("empty bucket %d/%d", cnt, list.size()));
                return -1;
            }
            len += ele.length;
        }
        return len;
    }
    
    synchronized void put(String commonHeader, int idx, byte[] data) {
        List<byte[]> frags = msgFragsMap.get(commonHeader);
        if (frags == null) {
            frags = new ArrayList<byte[]>();
            msgFragsMap.put(commonHeader, frags);
        }
        set(frags, idx, data);
    }

    synchronized byte[] getMsg(String commonHeader) {
        List<byte[]> frags = msgFragsMap.get(commonHeader);
        int len = byteLength(frags);
        if (len == -1) return null;
        byte[] msg = new byte[len];
        int idx = 0;
        for (byte[] ele : frags) {
            System.arraycopy(ele, 0, msg, idx, ele.length);
            idx += ele.length;
        }
        msgFragsMap.remove(commonHeader);
        return msg;
    }
}
