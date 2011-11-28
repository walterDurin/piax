/*
 * BroadcastTSD.java
 *
 * TSD implementation on broadcast;
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
package org.piax.trans.tsd.broadcast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.piax.trans.common.PeerId;
import org.piax.trans.common.ServiceInfo;
import org.piax.trans.tsd.TSD;
import org.piax.trans.tsd.TSDListener;
import org.piax.trans.util.Base64;

public abstract class BroadcastTSD extends TSD implements Runnable {
    public static int PSDP_PORT = 12369;
    DatagramSocket socket;
    boolean running;
    ArrayList<TSDListener> listeners;
    ArrayList<ServiceInfo> sil;
    ArrayList<ServiceInfo> availableList;
        
    public BroadcastTSD() throws SocketException {
        sil = new ArrayList<ServiceInfo>();
        availableList = new ArrayList<ServiceInfo>();
        listeners = new ArrayList<TSDListener>();
        socket = new DatagramSocket(PSDP_PORT);
        socket.setBroadcast(true);
    }

    public void start() {
        if (!isRunning()) {
            Thread t = new Thread(this);
            t.start();
            //            t1 = new Timer();
            //            t1.schedule(new TSDRunner(this), 0, 10000);
        }
    }

    public InetAddress getBroadcastAddress() {
        try {
            return InetAddress.getByName("255.255.255.255");
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    private void advertiseService (ServiceInfo info) {	
        String data = "TSD/1.0\t" + info.getJson();
        InetAddress baddr = getBroadcastAddress();
        //System.out.println("BADDR=" + baddr);
        DatagramPacket packet = null;
        try {
            packet = new DatagramPacket(data.getBytes("UTF-8"),
                    data.length(),
                    baddr,
                    PSDP_PORT);
        } catch (UnsupportedEncodingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            if (baddr != null && packet != null) {
            	socket.send(packet);
            }
        } catch (SocketException se) {
            try {
                // Just one more time;
                if (socket != null) {
                    socket.close();
                    socket = new DatagramSocket(PSDP_PORT);
                    socket.setBroadcast(true);
                    socket.send(packet);
                }
            } catch (IOException ie) {
                // TODO Auto-generated catch block
                ie.printStackTrace();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
    }

    public boolean isRunning() {
        synchronized(this) {
            return running;
        }
    }

    public void close() {
        synchronized(this) {
            socket.close();
            running = false;
//            if (t1 != null) {
//                t1.cancel();
//            }
        }
    }

    public void run() {
        byte[] buf = new byte[1024];
        running = true;
        while (running) {
            //System.out.println("***** WAIT LOOP ****");
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);	
            } catch (IOException e) {
                // Canceled.
                //e.printStackTrace();
            }
            String recv = null;
            try {
                recv = new String (buf, 0, buf.length, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            //System.out.println("recv = " + recv);
            if (recv == null) {
                continue;
            }
            String [] pair = recv.split("\t");
            if (!(pair.length >= 2)) {
                continue;
            }
            ServiceInfo info = ServiceInfo.createByJson(pair[1]);
            if (info == null) {
                continue;
            }
            PeerId id = info.getId();
            boolean found = false;
            synchronized (availableList) {
                for (ServiceInfo a : availableList) {
                    if (id.equals(a.getId())) {
                        a.observed();
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                info.observed();
                availableList.add(info);
            }
            synchronized (listeners) {
                for (TSDListener listener : listeners) {
                    listener.serviceAvailable(info, !found);
                }
            }
        }
    }

    public void advertiseAll () {
        synchronized (sil) {
            if (sil != null) {
                for (ServiceInfo info : sil) {
                    //System.out.println("advertise:" + info.getJson());
                    advertiseService(info);
                }
            }
        }
    }

    public void setUnavailable (ServiceInfo info) {
    	synchronized (availableList) {
            availableList.remove(info);
            synchronized (listeners) {
                for (TSDListener listener : listeners) {
                    listener.serviceUnavailable(info);
                }
            }
    	}
    }

    @Override
    public void addServiceListener(TSDListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public ServiceInfo[] list() {
        return availableList.toArray(new ServiceInfo[0]);
    }
    
    @Override
    public void registerService(ServiceInfo info) {
        if (info.getType().contains("Bluetooth")) {
            return;
        }
        synchronized (sil) {
            ServiceInfo match = null;
            for (ServiceInfo si: sil) {
                // XXX Is IP address matching enough?
                if (si.getHost().equals(info.getHost())) {
                    match = si;
                }
            }
            if (match == null) {
                sil.add(info);
                advertiseService(info);
            }
        }
    }
    
    @Override
    public void removeServiceListener(TSDListener listener) {
    	synchronized(listeners) {
    		listeners.remove(listener);
    	}
    }
    
    @Override
    public void unregisterAllServices() {
        sil = new ArrayList<ServiceInfo>();
    }
    
    @Override
    public void unregisterService(ServiceInfo info) {
        synchronized (sil) {
            ServiceInfo match = null;
            for (ServiceInfo si: sil) {
                // XXX Is IP address matching enough?
                if (si.getHost().equals(info.getHost())) {
                    match = si;
                }
            }
            if (match != null) sil.remove(match);
        }
    }

}