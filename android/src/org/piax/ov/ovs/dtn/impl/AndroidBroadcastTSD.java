/*
 * AndroidTSD.java
 *
 * TSD implementation on Android with broadcast;
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
package org.piax.ov.ovs.dtn.impl;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

import java.net.UnknownHostException;
import java.util.Enumeration;

import org.piax.trans.tsd.broadcast.BroadcastTSD;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;

public class AndroidBroadcastTSD extends BroadcastTSD {
    Context ctxt;

    public AndroidBroadcastTSD(Context ctxt) throws SocketException {
        super();
        this.ctxt = ctxt;
    }
    
    static NetworkInterface getUsableInterface(boolean widearea) {
        try {
            Enumeration<NetworkInterface> enuIfs = NetworkInterface.getNetworkInterfaces();
            NetworkInterface ni = null;
            NetworkInterface selected = null;
            while (enuIfs.hasMoreElements()) {
                ni = (NetworkInterface)enuIfs.nextElement();
                // Log.d("AndroidBroadcastTSD","Available Network Interface=" + ni.getName());
                // XXX rmnet0 is widearea (3G) interface. Is it true in all devices?
                if (!ni.getName().startsWith("lo") &&
                    (widearea || !ni.getName().startsWith("rm"))) {
                    selected = ni;
                    // use the first one.
                    break;
                }
            }
            return selected;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static InetAddress getUsableAddress (boolean widearea) {
        try {
            NetworkInterface ni = getUsableInterface(widearea);
            if (ni == null) return null;
            Enumeration<InetAddress> interfaceAddresses = ni.getInetAddresses();
            InetAddress address = null;
            while (interfaceAddresses.hasMoreElements()) {
                address = (InetAddress) interfaceAddresses.nextElement();
                // Log.d("AndroidBroadcastTSD", ni + "address=" + address);
            }
            return address;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }    
    
    public InetAddress getBroadcastAddress() {
        try {
            InetAddress addr = getUsableAddress(false);
            if (addr != null) {
                byte[] baddr = addr.getAddress();
                // USING DHCP only is harmful since it returns no-existent
                // address when ad-hoc mode.
                WifiManager wm = (WifiManager) ctxt
                    .getSystemService(android.content.Context.WIFI_SERVICE);
                DhcpInfo dhcp = wm.getDhcpInfo();
                if (dhcp != null) {
                    byte[] daddr = new byte[4];
                    for (int i = 0; i < 4; i++) {
                        daddr[i] = (byte) ((dhcp.ipAddress >> i * 8) & 0xFF);
                    }
                    boolean same = true;
                    for (int i = 0; i < 4; i++) {
                        if (daddr[i] != baddr[i]) {
                            same = false;
                            break;
                        }
                    }
                    // If usable address and dhcp address are same,
                    // dhcp netmask can be applied.
                    if (same) {
                        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
                        byte[] dbaddr = new byte[4];
                        for (int k = 0; k < 4; k++) {
                            dbaddr[k] = (byte) ((broadcast >> k * 8) & 0xFF);
                        }
                        try {
                            //Log.d("AndroidBroadcastTSD", "broadcast address by dhcp:" + InetAddress.getByAddress(dbaddr));
                            return InetAddress.getByAddress(dbaddr);
                        } catch (UnknownHostException e) {
                            e.printStackTrace(); 
                            return null;
                        }
                    }
                    else {
                        // XXX In this case, netmask is assumed to be 24bit
                        baddr[3] = (byte) 0xFF;
                        //Log.d("AndroidBroadcastTSD", "different dhcp. broadcast address" + InetAddress.getByAddress(baddr));
                        return InetAddress.getByAddress(baddr);
                    }
                }
                else {
                    //Log.d("AndroidBroadcastTSD", "DHCP is null");
                    // XXX In this case, netmask is assumed to be 24bit
                    baddr[3] = (byte) 0xFF;
                    
                    //Log.d("AndroidBroadcastTSD","broadcast address by ad-hoc:" + InetAddress.getByAddress(baddr));
                    return InetAddress.getByAddress(baddr);
                }
            }
            //System.out.println("Cannot obtain broadcast address???");
            return null;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
        
    }

    @Override
    public boolean requiresWiFi() {
        return true;
    }

}