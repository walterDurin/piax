/*
 * LocalInetAddrs.java
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
 * 2007/02/12 designed and implemented by M. Yoshida.
 * 
 * $Id: LocalInetAddrs.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans.util;

import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.grlea.log.SimpleLogger;

/**
 * @author     Mikio Yoshida
 * @version    1.0.0
 */
public class LocalInetAddrs {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(LocalInetAddrs.class);

    private static LocalInetAddrs instance = new LocalInetAddrs();
    
    public static LocalInetAddrs getInstance() {
        return instance;
    }

    public static InetAddress choice() {
        instance.listup();
        return instance.choise();
    }
    
    public static InetAddress choiceIfIsLocal(InetAddress target) {
        instance.listup();
        if (instance.isLocalAddr(target)) {
            return instance.choise();
        }
        return target;
    }

    public static boolean isLocal(InetAddress target) {
        return instance.isLocalAddr(target);
    }
    
    private LocalInetAddrs() {
    }

    private List linkLocals = new ArrayList();
    private List siteLocals = new ArrayList();
    private List globals = new ArrayList();

    public void listup() {
        linkLocals.clear();
        siteLocals.clear();
        globals.clear();
        
        Enumeration netIfs;
        try {
            netIfs = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            // never occurred.
            log.errorException(e);
            return;
        }
        while (netIfs.hasMoreElements()) {
            NetworkInterface netIf = (NetworkInterface) netIfs.nextElement();
            Enumeration inetAddrs = netIf.getInetAddresses();
            while (inetAddrs.hasMoreElements()) {
                InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                if (inetAddr instanceof Inet6Address) {
                    // k-abe
                } else
                if (inetAddr.isLoopbackAddress()) {
                    
                } else if (inetAddr.isLinkLocalAddress()) {
                    linkLocals.add(inetAddr);
                } else if (inetAddr.isSiteLocalAddress()) {
                    siteLocals.add(inetAddr);
                } else {
                    globals.add(inetAddr);
                }
            }
        }
    }

    public InetAddress choise() {
        if (globals.size() > 0) {
            return (InetAddress) globals.get(0);
        }
        if (siteLocals.size() > 0) {
            return (InetAddress) siteLocals.get(0);
        }
        if (linkLocals.size() > 0) {
            return (InetAddress) linkLocals.get(0);
        }
        try {
            return InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            // TODO
            log.errorException(e);
            return null;
        }
    }

    public boolean isLocalAddr(InetAddress target) {
        String address = target.getHostAddress();
        
        for (int i = 0; i < globals.size(); i++) {
            InetAddress iaddr = (InetAddress) globals.get(i);
            if (address.equals(iaddr.getHostAddress())) {
                return true;
            }
        }
        for (int i = 0; i < siteLocals.size(); i++) {
            InetAddress iaddr = (InetAddress) siteLocals.get(i);
            if (address.equals(iaddr.getHostAddress())) {
                return true;
            }
        }
        for (int i = 0; i < linkLocals.size(); i++) {
            InetAddress iaddr = (InetAddress) linkLocals.get(i);
            if (address.equals(iaddr.getHostAddress())) {
                return true;
            }
        }
        if (target.isLoopbackAddress()) {
            return true;
        }
        return false;
    }
}
