/*
 * ReturnSetImpl.java
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
 * $Id: ReturnSetImpl.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans.common.impl;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.piax.trans.common.FutureReturn;

/**
 * @author     Mikio Yoshida
 * @version    1.0.0
 * @param <V>
 */
public class ReturnSetImpl<V> extends AbstractReturnSet<V> 
	implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Set<FutureReturnImpl<V>> registered;
    private int numOfFuture;
    private int numOfCancelled;
    volatile private boolean registerDone;

    public ReturnSetImpl() {
        this(false);
    }
    
    public ReturnSetImpl(boolean filterExceptedResults) {
        super(Integer.MAX_VALUE, filterExceptedResults);
        registered = new HashSet<FutureReturnImpl<V>>();
        numOfFuture = 0;
        numOfCancelled = 0;
        registerDone = false;
    }
    
    @Override
    protected synchronized boolean acceptChain(ReturnSetAggregator<V> aggregator) {
        return super.acceptChain(aggregator);
    }
    
    /** Methods called by FutureReturn **/

    private void tryEndOfDone() {
        if (isRegisterDone() && numOfRemained() == 0) {
            endOfDone();
        }
    }
    
    private void acceptNotify0(FutureReturnImpl<V> future) {
        if (!registered.contains(future)) {
            throw new IllegalArgumentException("Invalid future");
        }
        registered.remove(future);
    }
    
    // this method could be blocked
    synchronized void acceptNotifyDone(FutureReturnImpl<V> future) {
        acceptNotify0(future);
        putDone(future);
        tryEndOfDone();
    }
    
    synchronized void acceptNotifyCancel(FutureReturnImpl<V> future) {
        acceptNotify0(future);
        numOfCancelled++;
        tryEndOfDone();
    }
    
    /** Methods for mechanism structure **/

    // this method could be blocked
    public synchronized void addFuture(FutureReturn<V> future)
            throws IllegalStateException {
        if (registerDone || isCancelled()) {
            throw new IllegalStateException(
                    "This ReturnSet was registerDone or cancelled");
        }
        FutureReturnImpl<V> f = (FutureReturnImpl<V>) future;
        if (f.bindTo(this)) {
            registered.add(f);
        } else {
            // no need to add future to registered list
            putDone(f);
        }
    }

    // this method could be blocked
    public synchronized boolean noMoreFutures() {
        if (registerDone) {
            return false;
        }
        registerDone = true;
        tryEndOfDone();
        return true;
    }
    
    /** Methods for applications **/

    /* (non-Javadoc)
     * @see org.piax.trans.common.impl.AbstractReturnSet#cancel()
     */
    @Override
    public synchronized boolean cancel() {
        if (!super.cancel()) {
            return false;
        }
        for (FutureReturnImpl<V> f : registered) {
            f.cancelFromFutureSet();
            numOfCancelled++;
        }
        registered.clear();
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
    public int numOfRegistered() {
        return numOfFuture;
    }

    /* (non-Javadoc)
     * @see org.piax.trans.common.AbstractReturnSet#numOfCancelled()
     */
    @Override
    public int numOfCancelled() {
        return super.numOfCancelled() + numOfCancelled;
    }
    
    /* (non-Javadoc)
     * @see org.piax.trans.common.AbstractReturnSet#numOfRemained()
     */
    @Override
    public int numOfRemained() {
        return registered.size();
    }
}
