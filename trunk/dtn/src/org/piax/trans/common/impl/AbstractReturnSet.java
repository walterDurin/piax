/*
 * AbstractReturnSet.java
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
 * 2007/03/02 designed and implemented by M. Yoshida.
 * 
 * $Id: AbstractReturnSet.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans.common.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.grlea.log.SimpleLogger;
import org.piax.trans.common.FutureReturn;
import org.piax.trans.common.Id;
import org.piax.trans.common.PeerId;
import org.piax.trans.common.ReturnSet;

/**
 * @author     Mikio Yoshida
 * @version    2.0.0
 * @param <V>
 */
public abstract class AbstractReturnSet<V> implements ReturnSet<V> {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(AbstractReturnSet.class);
    
    protected static final FutureReturnImpl END_OF_STREAM = new FutureReturnImpl();
    
//    private static final ReturnSet emptyReturnSet;

    /*
     * TODO should define emptyReturnSet to use as final instance
     */
    public static <V> ReturnSet<V> emptyReturnSet() {
        ReturnSetImpl<V> rset = new ReturnSetImpl<V>(true);
        rset.noMoreFutures();
        rset.endOfDone();
        return rset;
//        return (ReturnSet<T>) emptyReturnSet;
    }

    public static <V> ReturnSet<V> singletonReturnSet(V value) {
        FutureReturnImpl<V> f = new FutureReturnImpl<V>();
        ReturnSetImpl<V> rset = new ReturnSetImpl<V>(true);
        f.setResult(value);
        rset.addFuture(f);
        rset.noMoreFutures();
        return rset;
    }

    public static <V> ReturnSet<V> newReturnSet(Set<V> values) {
        ReturnSetImpl<V> rset = new ReturnSetImpl<V>(true);
        for (V value : values) {
            FutureReturnImpl<V> f = new FutureReturnImpl<V>();
            f.setResult(value);
            rset.addFuture(f);
        }
        rset.noMoreFutures();
        return rset;
    }

    //    static {
//        ReturnSetImpl<Object> rset = new ReturnSetImpl<Object>(true);
//        rset.noMoreFutures();
//        rset.endOfDone();
//        emptyReturnSet = rset;
//    }

    private final boolean filterExceptedResults;
    private final BlockingQueue<FutureReturnImpl<V>> done;
    
    /**
     * Secondary queue of overflowed done elements.
     * This queue is necessary to merge ReturnSet.
     */
    private Queue<FutureReturnImpl<V>> overflows = null;
    private int numOfExcepted;
    private int numOfCancelled;
    volatile private boolean cancelled;
    volatile private boolean isEndOfDone;

    private final Object mutex = new Object();
    private FutureReturn<V> current = null;
    
    volatile private ReturnSetAggregator<V> aggregator = null;

    public AbstractReturnSet(int capacity, boolean filterExceptedResults) {
        this.filterExceptedResults = filterExceptedResults;
        if (capacity == Integer.MAX_VALUE) {
            done = new LinkedBlockingQueue<FutureReturnImpl<V>>();
        } else {
            done = new ArrayBlockingQueue<FutureReturnImpl<V>>(capacity);
        }
        numOfExcepted = 0;
        numOfCancelled = 0;
        cancelled = false;
        isEndOfDone = false;
    }

    private void backFromOverflow() {
        if (overflows != null) {
            synchronized (overflows) {
                while (true) {
                    FutureReturnImpl<V> f = overflows.peek();
                    if (f == null)
                        break;
                    if (!done.offer(f))
                        break;
                    overflows.poll();
                }
            }            
        }
    }
    
    private void clearQueue() {
        for (FutureReturnImpl<V> f : done) {
            f.cancel();
        }
        done.clear();
        if (overflows != null) {
            synchronized (overflows) {
                for (FutureReturnImpl<V> f : overflows) {
                    f.cancel();
                }
                overflows.clear();
            }            
        }
    }
    
    private void offerInQueue(FutureReturnImpl<V> future) {
        if (future.isCancelled()) {
            numOfCancelled++;
            return;
        }
        if (future.isExcepted() && filterExceptedResults) {
            numOfExcepted++;
            return;
        }
        backFromOverflow();
        if (!done.offer(future)) {
            if (overflows == null) {
                overflows = new LinkedBlockingQueue<FutureReturnImpl<V>>();
            }
            synchronized (overflows) {
                overflows.offer(future);
            }            
        }
    }
    
    // this method could be blocked
    private void putInQueue(FutureReturnImpl<V> future) {
        if (future.isCancelled()) {
            numOfCancelled++;
            return;
        }
        if (future.isExcepted() && filterExceptedResults) {
            numOfExcepted++;
            return;
        }
        backFromOverflow();
        try {
            done.put(future);
        } catch (InterruptedException ignore) {}
    }

    // this method could be blocked
    private FutureReturnImpl<V> pollFromQueue(long timeout) throws InterruptedException {
        backFromOverflow();
        return done.poll(timeout, TimeUnit.MILLISECONDS);
    }
    
    private void mergeTo(AbstractReturnSet<V> dst) {
        while (true) {
            FutureReturnImpl<V> f = done.poll();
            if (f == null) break;
            if (f == AbstractReturnSet.END_OF_STREAM) return;
            dst.offerInQueue(f);
        }
        if (overflows != null) {
            synchronized (overflows) {
                while (true) {
                    FutureReturnImpl<V> f = overflows.poll();
                    if (f == null)
                        break;
                    if (f == AbstractReturnSet.END_OF_STREAM)
                        return;
                    dst.offerInQueue(f);
                }
            }            
        }
    }
    
    // if unnecessary to chain, return false
    protected boolean acceptChain(ReturnSetAggregator<V> aggregator) {
        this.aggregator = aggregator;
        mergeTo(aggregator);
        return !isEndOfDone;
    }
    
    private void unchainAggregator() {
        if (aggregator == null) {
            return;
        }
        aggregator.unchain(this);
        aggregator = null;
    }
    
    // this method could be blocked
    protected void putDone(FutureReturnImpl<V> future) {
        log.entry("putDone");
        log.debugObject("result", future.getResult());
        
        if (cancelled) {
            // no exception raised
            return;
        }
        if (aggregator != null) {
            aggregator.putDone(future);
        } else {
            putInQueue(future);
        }
    }
    
    @SuppressWarnings("unchecked")
    protected void endOfDone() {
        log.entry("endOfDone()");
        if (cancelled) {
            // no exception raised
            return;
        }        
        if (aggregator != null) {
            unchainAggregator();
        } else {
            offerInQueue(END_OF_STREAM);
        }
        isEndOfDone = true;
    }
    
    /* (non-Javadoc)
     * @see org.piax.trans.common.ReturnSet#cancel()
     */
    @SuppressWarnings("unchecked")
    public boolean cancel() {
        log.entry("cancel()");
        if (cancelled) {
            return false;
        }
        cancelled = true;
        clearQueue();
        offerInQueue(END_OF_STREAM);
        
        if (aggregator != null) {
            unchainAggregator();
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.piax.trans.common.ReturnSet#isCancelled()
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /* (non-Javadoc)
     * @see org.piax.trans.common.ReturnSet#isRegisterDone()
     */
    public abstract boolean isRegisterDone();

    /* (non-Javadoc)
     * @see org.piax.trans.common.ReturnSet#numOfRegistered()
     */
    public abstract int numOfRegistered();

    /* (non-Javadoc)
     * @see org.piax.trans.common.ReturnSet#numOfExcepted()
     */
    public int numOfExcepted() {
        return numOfExcepted;
    }

    /* (non-Javadoc)
     * @see org.piax.trans.common.ReturnSet#numOfCancelled()
     */
    public int numOfCancelled() {
        return numOfCancelled;
    }

    /* (non-Javadoc)
     * @see org.piax.trans.common.ReturnSet#numOfRemained()
     */
    public abstract int numOfRemained();
    
    private boolean isReallyEnd() {
        return cancelled 
            || done.peek() == END_OF_STREAM;
    }

    /* (non-Javadoc)
     * @see org.piax.trans.common.ReturnSet#hasNext()
     */
    public boolean hasNext() {
        log.entry("hasNext()");
        if (cancelled || aggregator != null) {
            throw new IllegalStateException("This ReturnSet is inactive");
        }
        return !isReallyEnd();
    }

    /* (non-Javadoc)
     * @see org.piax.trans.common.ReturnSet#getNext()
     */
    public V getNext() throws NoSuchElementException,
            InvocationTargetException {
        try {
            return getNext(Long.MAX_VALUE);
        } catch (InterruptedException ignore) {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see org.piax.trans.common.ReturnSet#getNext(long)
     */
    public V getNext(long timeout) throws NoSuchElementException,
            InvocationTargetException, InterruptedException {
        log.entry("getNext()");
        if (cancelled || aggregator != null) {
            throw new IllegalStateException("This ReturnSet is inactive");
        }

        synchronized (mutex) {
            if (isReallyEnd()) {
                throw new NoSuchElementException();
            }
            current = null;
            FutureReturnImpl<V> f = pollFromQueue(timeout);
            if (f == END_OF_STREAM) {
                // unget
                offerInQueue(f);
                throw new NoSuchElementException();
            }
            if (f == null) {
                throw new InterruptedException("getNext was timeouted");
            }
            current = f;
            if (f.isExcepted()) {
                numOfExcepted++;
                throw new InvocationTargetException(f.getException());
            }
            return f.getResult();
        }        
    }

    /* (non-Javadoc)
     * @see org.piax.trans.common.ReturnSet#getThisPeerId()
     */
    public PeerId getThisPeerId() {
        return current == null ? null : current.getPeerId();
    }
    
    /* (non-Javadoc)
     * @see org.piax.trans.common.ReturnSet#getThisTargetId()
     */
    public Id getThisTargetId() {
        return current == null ? null : current.getObjectId();
    }

    /* (non-Javadoc)
     * @see org.piax.trans.common.ReturnSet#getThisOption()
     */
    public Object getThisOption() {
        return current == null ? null : current.getOption();
    }

}
