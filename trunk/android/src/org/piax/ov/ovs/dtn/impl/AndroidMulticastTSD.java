package org.piax.ov.ovs.dtn.impl;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;

import org.piax.trans.common.ServiceInfo;
import org.piax.trans.tsd.multicast.MulticastTSD;
import org.piax.trans.tsd.TSDListener;
import org.piax.trans.util.MethodUtil;

import android.content.Context;
import android.net.wifi.WifiManager;
//import android.net.wifi.WifiManager.MulticastLock;

public class AndroidMulticastTSD extends MulticastTSD {
    Context ctxt;
    Object mlock;
    
    public AndroidMulticastTSD(Context ctxt) throws SocketException {
        WifiManager wifi = (WifiManager)ctxt.getSystemService(Context.WIFI_SERVICE);
        try {
            mlock = MethodUtil.invoke(wifi, "createMulticastLock", "piax_multicast_lock");
            MethodUtil.invoke(mlock, "acquire");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        //        mclock = wifi.createMulticastLock("piax_multicast_lock");
        //mclock.acquire();

        this.ctxt = ctxt;

        sil = new ArrayList<ServiceInfo>();
        availableList = new ArrayList<ServiceInfo>();
        listeners = new ArrayList<TSDListener>();

        try {
            group = InetAddress.getByName(PSDP_GROUP);
            socket = new MulticastSocket(PSDP_PORT);
            NetworkInterface ni = AndroidBroadcastTSD.getUsableInterface(false);
            if (ni != null) {
                socket.setNetworkInterface(ni);
                socket.setTimeToLive(255);
                socket.joinGroup(group);
            }
            //            System.out.println("JOINED to" + group);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void close() {
        synchronized(this) {
            try {
                if (mlock != null) {
                    if (MethodUtil.invoke(mlock, "isHeld") == Boolean.TRUE) {
                        MethodUtil.invoke(mlock, "release");
                    }
                }
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            try {
                socket.leaveGroup(group);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            socket.close();
            running = false;
        }
    }

    @Override
    public boolean requiresWiFi() {
        return true;
    }
}
