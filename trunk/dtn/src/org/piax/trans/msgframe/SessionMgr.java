/*
 * SessionMgr.java
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
 * 2009/12/16 designed and implemented by M. Yoshida.
 * 
 * $Id: SessionMgr.java 183 2010-03-03 11:41:21Z yos $
 */
package org.piax.trans.msgframe;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.grlea.log.SimpleLogger;
import org.piax.trans.ConfigValues;

/**
 * SessionオブジェクトにsessionIdを割り当てるための管理クラス。
 * Messaging Frameworkでは、外部との通信接点を持つMessagingRoot
 * により保持させる。
 * 
 * @author     Mikio Yoshida
 * @version    2.2.0
 */
class SessionMgr {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(SessionMgr.class);

    private final ConcurrentMap<Integer, Session> sessionMap;
    private int genId;

    SessionMgr() {
        genId = 0;
        sessionMap = new ConcurrentHashMap<Integer, Session>();
    }

    /**
     * 指定された SessionオブジェクトにsessionIdを割り当てる。
     * <p>
     * sessionId は1からMAX_SESSIONSまでの正整数で、0は使用しない。
     * MAX_SESSIONS個のsessionIdが払い出された場合は、エラーとして-1が返される。
     * 
     * @param session Sessionオブジェクト
     * @return sessionId, エラーの場合-1
     */
    synchronized int newSessionId(Session session) {
        if (sessionMap.size() >= ConfigValues.MAX_SESSIONS) return -1;
        // decide genId
        // 使用中のIDについてはスキップさせる
        do {
            if (genId < ConfigValues.MAX_SESSIONS) genId++;
            else genId = 1;
        } while (sessionMap.putIfAbsent(genId, session) != null);
        session.mgr = this;
        session.sessionId = genId;
        log.debugObject("new sessionId", genId);
        return genId;
    }
    
    /**
     * sessionIdからSessionオブジェクトを取得する。
     * timeout等により、Sessionオブジェクトが解放された場合は、nullが返る。
     * 
     * @param sessionId sessionId
     * @return Sessionオブジェクト。オブジェクトが解放された場合はnull
     */
    Session getSession(int sessionId) {
        return sessionMap.get(sessionId);
    }
    
    boolean removeSession(int sessionId, Session session) {
        return sessionMap.remove(sessionId, session);
    }

    /**
     * SessionMgrオブジェクトを終了させる。
     */
    void fin() {
    }
}
