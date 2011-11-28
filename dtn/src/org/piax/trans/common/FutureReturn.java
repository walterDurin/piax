/*
 * FutureReturn.java
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
 * 2007/01/30 designed and implemented by M. Yoshida.
 * 
 * $Id: FutureReturn.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans.common;

import java.lang.reflect.InvocationTargetException;


/**
 * The class includes the method call result which will return in future.
 * The specification of this class is same as the java.util.concurrent.Future
 * interface, except in some methods.
 * 
 * @author     Mikio Yoshida
 * @version    1.0.0
 */
public interface FutureReturn<V> {
    
    /**
     * Attempts to cancel execution of this task.  This attempt will
     * fail if the task has already completed, already been canceled,
     * or could not be canceled for some other reason. If successful,
     * and this task has not started when <code>cancel</code> is called,
     * this task should never run.  If the task has already started,
     * then the <code>mayInterruptIfRunning</code> parameter determines
     * whether the thread executing this task should be interrupted in
     * an attempt to stop the task.
     *
     * @return <code>false</code> if the task could not be canceled,
     *     typically because it has already completed normally;
     *     <code>true</code> otherwise
     */
    boolean cancel();
    
    /**
     * Returns <code>true</code> if this task was canceled before it completed
     * normally.
     *
     * @return <code>true</code> if task was canceled before it completed
     */
    boolean isCancelled();

    /**
     * Returns <code>true</code> if this task completed.  
     *
     * Completion may be due to normal termination, an exception, or
     * cancellation -- in all of these cases, this method will return
     * <code>true</code>.
     * 
     * @return <code>true</code> if this task completed.
     */
    boolean isDone();
    
    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     *
     * @return the computed result
     * @throws IllegalStateException if the computation was canceled
     * @throws InvocationTargetException if the computation threw an
     *     exception
     */
    V get() throws InvocationTargetException;

    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result, if available.
     *
     * @param timeout the maximum time to wait
     * @return the computed result
     * @throws IllegalStateException if the computation was canceled
     * @throws InvocationTargetException if the computation threw an
     *     exception
     * @throws InterruptedException if the current thread was interrupted
     *     while waiting
     */
    V get(long timeout) 
            throws InvocationTargetException, InterruptedException;
    
    PeerId getPeerId();

    Id getObjectId();

    Object getOption();
}
