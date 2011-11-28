package org.piax.trans.util;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.StringTokenizer;

public class AddressUtil {
    static HashMap<String,InetSocketAddress> cache = new HashMap<String,InetSocketAddress>();
    // implementation with bytes
    static public InetSocketAddress getAddress(byte[] ip, int port) throws UnknownHostException {
        StringBuilder addrStr = new StringBuilder(24);
        addrStr.append(ip[0] & 0xFF);
        addrStr.append(".");
        addrStr.append(ip[1] & 0xFF);
        addrStr.append(".");
        addrStr.append(ip[2] & 0xFF);
        addrStr.append(".");
        addrStr.append(ip[3] & 0xFF);
        // System.out.println("addrStr=" + addrStr.toString());
        InetSocketAddress addr = cache.get(addrStr.toString() + port);
        if (addr != null) {
            return addr;
        }
        else {
            addr = new InetSocketAddress(InetAddress.getByAddress(addrStr.toString(), ip), port);
            cache.put (addrStr.toString() + port, addr);
            return addr;
        }
    }
    static public InetSocketAddress getAddress(String address, int port) throws UnknownHostException {
        StringTokenizer st = new StringTokenizer(address, ".");
        InetAddress addr = null;
        InetSocketAddress saddr = cache.get(address + port);
        if (saddr != null) {
            return saddr;
        }
        else {
            // parse ip address string
            try {
                byte[] addrByte = new byte[4];
                for (int i = 0; i < 4; i++) {
                    addrByte[i] = (byte)Integer.parseInt(st.nextToken());
                }
                addr = InetAddress.getByAddress(address, addrByte);
            } catch (UnknownHostException e) {
            } catch (NumberFormatException e) {
                addr = InetAddress.getByName(address);
            }
            saddr = new InetSocketAddress(addr, port);
            cache.put(address + port, saddr);
            return saddr;
        }
    }
    public static void main (String args[]) throws Exception {
        System.out.println("addr = " + AddressUtil.getAddress("192.168.1.1", 10080));
        System.out.println("addr = " + AddressUtil.getAddress("192.168.1.1.4", 10080));
        System.out.println("addr = " + new InetSocketAddress("192.168.1.1", 10080));
        System.out.println("addr = " + AddressUtil.getAddress("www.google.com", 443));
    }
}
