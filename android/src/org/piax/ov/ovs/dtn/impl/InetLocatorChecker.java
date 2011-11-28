package org.piax.ov.ovs.dtn.impl;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.piax.trans.common.PeerLocator;
import org.piax.trans.ts.tcp.TcpLocator;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

public class InetLocatorChecker extends LocatorChecker {
 //   WifiReceiver receiverWifi;
 //   Context ctxt;
//    WifiManager wifiManager;
    LocatorCheckerDelegate delegate;
    InetAddress addr;
    WifiManager.WifiLock wifiLock;
    int port;

    boolean useRMNet;
    
/*    private class WifiReceiver extends BroadcastReceiver {  
        public void onReceive(android.content.Context c, Intent intent) {
        //System.out.println(intent.getAction());
            
            //intent.getAction().equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION))
            //|| intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)

            if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                notifyAddress();
            }
        }
    }*/
    
    public void notifyLocator() {
        synchronized(this) {
            InetAddress newAddr = getAddress();
            if (newAddr == null) {
                PeerLocator l = new TcpLocator(new InetSocketAddress(addr, port));
                delegate.locatorUnavailable(l);
                addr = null;
//                Log.d("AndroidWiFi", "Address " + addr + " is unavailable!");
            }
            else {
                if (addr == null || !addr.equals(newAddr)) {
                    PeerLocator nl = new TcpLocator(new InetSocketAddress(newAddr, port));
                    delegate.locatorAvailable(nl);
                    addr = newAddr;
//                    Log.d("AndroidWiFi", "New Address " + addr + " is available!");
                }
                else {
//                    Log.d("AndroidWiFi", "Address " + addr + " is same as OLD!");
                }
            }
        }
    }

    public void setUseRMNet(boolean useRMNet) {
        this.useRMNet = useRMNet;
    }

    InetAddress getAddress() {
        InetAddress newAddr = AndroidBroadcastTSD.getUsableAddress(useRMNet);
        //InetAddress newAddr = AndroidBroadcastTSD.getUsableAddress(false);
        if (newAddr == null) {
            return null;
        }
        byte[] baddr = newAddr.getAddress();
        if (baddr[0] == 0 && baddr[1] == 0 && baddr[2] == 0 && baddr[3] == 0) {
            return null;
        }
        return newAddr;
    }

    public InetLocatorChecker(Context ctxt, int port, LocatorCheckerDelegate delegate) {
 //       this.ctxt = ctxt;
        WifiManager wm = (WifiManager) ctxt.getSystemService(android.content.Context.WIFI_SERVICE);
        //        receiverWifi = new WifiReceiver();
        //        register();
        this.delegate = delegate;
        this.port = port;

        addr = null;
        notifyLocator();
        useRMNet = false;

        wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "DTN WifiLock");
        wifiLock.setReferenceCounted(false);
        wifiLock.acquire(); 
    }

    //    private void unregister() {
    //        ctxt.unregisterReceiver(receiverWifi);   
    //    }  

//     private void register() {
//         IntentFilter intentFilter = new IntentFilter();
//         intentFilter.addAction (WifiManager.NETWORK_STATE_CHANGED_ACTION);
//         //intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
//         //        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
//         //        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
//         ctxt.registerReceiver(receiverWifi, intentFilter);        
//     }

    public void fin() {
        wifiLock.release();
//         try {
//             // XXX It is harmful when receiverWifi is not registered correctly
//             // unregister();
//         }
//         catch (Throwable e) {
//             e.printStackTrace();
//         }
    }
}
