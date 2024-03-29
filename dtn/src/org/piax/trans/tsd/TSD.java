/*
 * TSD.java - A class for Transport Service Discovery
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

import org.piax.trans.common.ServiceInfo;

public abstract class TSD {
    public abstract void registerService(ServiceInfo info);
    public abstract void unregisterService(ServiceInfo info);
    public abstract void addServiceListener(TSDListener listener);
    public abstract void removeServiceListener(TSDListener listener);
    public abstract void unregisterAllServices();

    public abstract void start();
    public abstract void close();
    public abstract boolean isRunning();
    public abstract ServiceInfo[] list();
    public abstract void advertiseAll();
    public abstract void setUnavailable(ServiceInfo info);

    public abstract boolean requiresWiFi();
}