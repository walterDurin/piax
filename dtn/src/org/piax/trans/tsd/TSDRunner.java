/*
 * TSDRunner.java
 *
 * Runner for TSD tasks
 * 
 * Copyright (c) 2010 Osaka University
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
 * @author Yuuichi Teranishi
 * $Id$
 */
package org.piax.trans.tsd;
import java.util.Date;

import org.piax.trans.common.ServiceInfo;

public class TSDRunner implements Runnable {
    TSD tsd;
    long keepAliveInterval;
    long timeoutPeriod;
    long lastAdvertised;

    public TSDRunner (TSD tsd, long keepAliveInterval, long timeoutPeriod) {
        this.tsd = tsd;
        this.keepAliveInterval = keepAliveInterval;
        this.timeoutPeriod = timeoutPeriod;
        lastAdvertised = -1;
    }

    public void run () {
        if (tsd.isRunning()) {
            long now = new Date ().getTime ();
            if (lastAdvertised < 0 ||
                (now - lastAdvertised) >= keepAliveInterval) {
                tsd.advertiseAll ();
                lastAdvertised = now;
            }
            ServiceInfo infos[] = tsd.list();
            for (ServiceInfo info : infos) {
                if (now - info.lastObserved().getTime() > timeoutPeriod) {
                    //System.out.println("*** UNAVAILABLE:" + info);
                    tsd.setUnavailable (info);
                }
            }
        }
        else {
        }
    }
}