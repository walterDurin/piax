/*
 * SysInfo.java
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
 * 2009/03/18 designed and implemented by M. Yoshida.
 * 
 * $Id: SysInfo.java 457 2009-04-17 00:27org.piax.ctrl*/
package org.piax.trans.stat;

public class SysInfo {
    
    static String memInfo() {
        Runtime run = Runtime.getRuntime();
        //int a = run.availableProcessors();
        long free = run.freeMemory();
        long max = run.maxMemory();
        long total = run.totalMemory();
        
        return "max: " + max + ", total: " + total + ", free: " + free + "\r\n";
    }
    
    public static long getUsedMem() {
        Runtime run = Runtime.getRuntime();
        long free = run.freeMemory();
        long total = run.totalMemory();

        return total - free;
    }
    
    public static int getThreadsNum() {
        // get root thread
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        ThreadGroup parent = tg.getParent();
        while (parent != null) {
            tg = parent;
            parent = parent.getParent();
        }

        Thread[] thlist = new Thread[tg.activeCount()];
        return tg.enumerate(thlist, true);
    }
    
    static String threadInfo() {
        // get root thread
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        ThreadGroup parent = tg.getParent();
        while (parent != null) {
            tg = parent;
            parent = parent.getParent();
        }

        // print member Thread info
        StringBuffer sbuf = new StringBuffer();

        Thread[] thlist = new Thread[1000];
        int size = tg.enumerate(thlist, true);
        for (int i = 0; i < size; i++) {
            Thread th = thlist[i];
            sbuf.append(th.toString() + "\r\n");
        }
        
        return sbuf.toString();
    }

    public static String getSysInfo() {
        return "- memory:\r\n" + memInfo() 
            + "- active threads:\r\n" + threadInfo();
    }
    
}
