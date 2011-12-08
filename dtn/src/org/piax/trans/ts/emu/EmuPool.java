/*
 * EmuPool.java -- A table for server socket emulation
 * 
 * Copyright (c) 2006-2007 Osaka University
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
 *
 * $Id: EmuPool.java 183 2010-03-03 11:41:21Z yos $
 */
package org.piax.trans.ts.emu;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author teranisi
 */
class EmuPool {
    static Map<EmuLocator, EmuTransportService> table = 
        new HashMap<EmuLocator, EmuTransportService>();
    
    static synchronized void add(EmuTransportService transport) {
        table.put(transport.peerLocator, transport);
    }

    static synchronized void remove(EmuLocator locator) {
        table.remove(locator);
    }

    static synchronized EmuTransportService lookup(EmuLocator locator)
            throws IOException {
        EmuTransportService emuTrans = table.get(locator);
        if (emuTrans == null) {
            throw new IOException("Cannot connect :-p");
        }
        return emuTrans;
    }

    static synchronized int size() {
        return table.size();
    }
}