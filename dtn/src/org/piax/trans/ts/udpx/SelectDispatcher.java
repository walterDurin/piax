/*
 * SelectDispatcher.java
 * 
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
 * 2007/11/11 designed and implemented by M. Yoshida.
 * 
 * $Id: SelectDispatcher.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans.ts.udpx;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import org.grlea.log.SimpleLogger;

/**
 * @author     Mikio Yoshida
 * @version    1.1.0
 */
public class SelectDispatcher extends Thread {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(SelectDispatcher.class);

    public final int MAX_FIN_WAIT = 50;
    
    private final Selector selector;
    
    public SelectDispatcher() throws IOException {
        selector = Selector.open();
    }

    public SelectionKey register(SelectorHandler handler, int ops)
            throws ClosedChannelException {
        SelectableChannel channel = handler.getChannel();
        return channel.register(selector, ops, handler);
    }
    
    public void unregister(SelectionKey key) {
        key.cancel();
    }
    
    public void fin() {
        try {
            selector.close();
            this.interrupt();
        } catch (IOException e) {
            log.warnException(e);
        }
        try {
            this.join(MAX_FIN_WAIT);
        } catch (InterruptedException ignore) {}
    }
    
    @Override
    public void run() {
        while (true) {
            try {
                selector.select();
                for (SelectionKey key : selector.selectedKeys()) {
                    SelectorHandler handler = (SelectorHandler) key.attachment();
                    if (key.isReadable())
                        handler.doReadOperation();
                }
            } catch (IOException e) {
                log.warnException(e);
            } catch (ClosedSelectorException e) {
                break;
            }
            if (isInterrupted()) break;
        }
    }
}
