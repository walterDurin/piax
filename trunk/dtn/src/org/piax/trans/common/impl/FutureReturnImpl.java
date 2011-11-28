/*
 * FutureReturnImpl.java
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
 * $Id: FutureReturnImpl.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans.common.impl;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

import org.grlea.log.SimpleLogger;
import org.piax.trans.common.FutureReturn;
import org.piax.trans.common.Id;
import org.piax.trans.common.PeerId;

/**
 * @author     Mikio Yoshida
 * @version    1.0.0
 * @param <V>
 */
public class FutureReturnImpl<V> implements FutureReturn<V>, Serializable {
    private static final long serialVersionUID = 1L;

    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(FutureReturnImpl.class);

    private boolean cancelled;
    private boolean accepted;
    /** The result to return from get() */
    private V result;
    /** The exception to throw from get() */
    private Throwable exception;
    
    private PeerId peerId = null;
    private Id objId = null;
    private Object option = null;
    
    /** The set of bound ReturnSet */
    private ReturnSetImpl<V> rset = null;
    
    public FutureReturnImpl() {
        cancelled = false;
        accepted = false;
        result = null;
        exception = null;
    }
    
    /** Methods between ReturnSet **/
    
    private void notifyDone2FutureSet() {
        if (rset != null)
            rset.acceptNotifyDone(this);
    }

    private void notifyCancel2FutureSet() {
        if (rset != null)
            rset.acceptNotifyCancel(this);
    }

    synchronized boolean bindTo(ReturnSetImpl<V> rset) {
        // if accepted, unnecessary to notify done to ReturnSet 
        if (accepted || cancelled) {
            return false;
        }
        this.rset = rset;
        return true;
    }
    
    boolean isExcepted() {
        return exception != null;
    }

    V getResult() {
        return result;
    }

    Throwable getException() {
        return exception;
    }
    
    synchronized void cancelFromFutureSet() {
        if (accepted || cancelled) {
            return;
        }
        cancelled = true;
        notify();
    }
    
    /** Methods for data setter **/

    public synchronized boolean setResult(V obj) {
        if (cancelled || accepted) {
            return false;
        }
        accepted = true;
        result = obj;
        notify();
        notifyDone2FutureSet();

        log.exit("setResult()");
        return true;
    }

    public synchronized boolean setException(Throwable t) {
        if (cancelled || accepted) {
            return false;
        }
        accepted = true;
        exception = t;
        notify();
        notifyDone2FutureSet();
        
        log.exit("setException()");
        return true;
    }
    
    public synchronized boolean setPeerId(PeerId peerId) {
        if (cancelled || accepted) {
            return false;
        }
        this.peerId = peerId;
        return true;
    }
    
    public synchronized boolean setObjectId(Id objId) {
        if (cancelled || accepted) {
            return false;
        }
        this.objId = objId;
        return true;
    }

    public synchronized boolean setOption(Object option) {
        if (cancelled || accepted) {
            return false;
        }
        this.option = option;
        return true;
    }
    

    /* (non-Javadoc)
     * @see org.piax.trans.common.FutureReturn#cancel()
     */
    public synchronized boolean cancel() {
        if (accepted || cancelled) {
            return false;
        }
        cancelled = true;
        notify();
        notifyCancel2FutureSet();
        return true;
    }

    /* (non-Javadoc)
     * @see org.piax.trans.common.FutureReturn#isCancelled()
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /* (non-Javadoc)
     * @see org.piax.trans.common.FutureReturn#isDone()
     */
    public boolean isDone() {
        return accepted;
    }

    /* (non-Javadoc)
     * @see org.piax.trans.common.FutureReturn#get()
     */
    public synchronized V get() throws InvocationTargetException {
        while (!accepted && !cancelled) {
            try {
                wait();
            } catch (InterruptedException e) {
                // ignore
            }
        }
        if (cancelled) {
            throw new IllegalStateException("Invocation was cancelled");
        }
        if (exception != null) {
            throw new InvocationTargetException(exception);
        }
        return result;
    }

    /* (non-Javadoc)
     * @see org.piax.trans.common.FutureReturn#get(long)
     */
    public synchronized V get(long timeout) 
    throws InvocationTargetException, InterruptedException {
        if (timeout == 0) {
            return get();
        } else {
            long until = System.currentTimeMillis() + timeout;
            long rest;
            while (!accepted && !cancelled
                    && (rest = (until - System.currentTimeMillis())) > 0) {
                try {
                    wait(rest);
                } catch (InterruptedException e) {
                    // ignore
                }
            } 
        }
        if (cancelled) {
            throw new IllegalStateException("Invocation was cancelled");
        }
        if (exception != null) {
            throw new InvocationTargetException(exception);
        }
        if (!accepted) {
            throw new InterruptedException("Invocation was timeouted");
        }
        return result;
    }

    /* (non-Javadoc)
     * @see org.piax.trans.common.FutureReturn#getPeerId()
     */
    public synchronized PeerId getPeerId() {
        return peerId;
    }

    /* (non-Javadoc)
     * @see org.piax.trans.common.FutureReturn#getObjectId()
     */
    public synchronized Id getObjectId() {
        return objId;
    }

    /* (non-Javadoc)
     * @see org.piax.trans.common.FutureReturn#getOption()
     */
    public synchronized Object getOption() {
        return option;
    }
}
