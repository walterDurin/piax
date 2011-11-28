/*
 * ReturnSetAggregator.java
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
 * 2007/03/01 designed and implemented by M. Yoshida.
 * 
 * $Id: ReturnSetAggregator.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans.common.impl;

import java.util.HashSet;
import java.util.Set;

import org.grlea.log.SimpleLogger;

/**
 * @author     Mikio Yoshida
 * @version    1.0.0
 * @param <V>
 */
public class ReturnSetAggregator<V> extends AbstractReturnSet<V> {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(ReturnSetAggregator.class);
    
    private final Set<AbstractReturnSet<V>> rsets;
    volatile private boolean registerDone;

    public ReturnSetAggregator(int capacity) {
        this(capacity, true);
    }

    public ReturnSetAggregator(int capacity, boolean filterExceptedResults) {
        super(capacity, filterExceptedResults);
        rsets = new HashSet<AbstractReturnSet<V>>();
        registerDone = false;
    }
    
    public synchronized boolean chain(AbstractReturnSet<V> rset) {
        log.entry("chain()");
        if (registerDone || isCancelled()) {
            throw new IllegalStateException(
                    "This ReturnSet was registerDone or cancelled");
        }

        if (rset == null) {
            return false;
        }
        if (rset.acceptChain(this)) {
            if (!rsets.add(rset)) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    protected synchronized boolean acceptChain(ReturnSetAggregator<V> aggregator) {
        return super.acceptChain(aggregator);
    }

    boolean unchain(AbstractReturnSet<V> rset) {
        log.entry("unchain()");
        if (isCancelled()) return true;
        
        synchronized (this) {
            if (rset == null) {
                return false;
            }
            if (!rsets.remove(rset)) {
                return false;
            }
            if (registerDone && rsets.size() == 0) {
                endOfDone();
            }
            return true;
        }        
    }
    
    public synchronized void noMoreChains() {
        log.entry("noMoreChains()");
        if (registerDone) {
            return;
        }
        if (rsets.size() == 0) {
            endOfDone();
        }
        registerDone = true;
    }
    
    /* (non-Javadoc)
     * @see org.piax.trans.common.impl.AbstractReturnSet#cancel()
     */
    @Override
    public synchronized boolean cancel() {
        if (!super.cancel()) {
            return false;
        }
        for (AbstractReturnSet<V> rset : rsets) {
            rset.cancel();
        }
        rsets.clear();
        return true;
    }

    /* (non-Javadoc)
     * @see org.piax.trans.common.AbstractReturnSet#isRegisterDone()
     */
    @Override
    public boolean isRegisterDone() {
        return registerDone;
    }

    /* (non-Javadoc)
     * @see org.piax.trans.common.AbstractReturnSet#numOfRegistered()
     */
    @Override
    public synchronized int numOfRegistered() {
        int sum = 0;
        for (AbstractReturnSet<V> rset : rsets) {
            sum += rset.numOfRegistered();
        }
        return sum;
    }

    /* (non-Javadoc)
     * @see org.piax.trans.common.AbstractReturnSet#numOfRemained()
     */
    @Override
    public synchronized int numOfRemained() {
        int sum = 0;
        for (AbstractReturnSet<V> rset : rsets) {
            sum += rset.numOfRemained();
        }
        return sum;
    }

}
