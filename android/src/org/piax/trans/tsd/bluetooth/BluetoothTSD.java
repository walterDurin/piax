package org.piax.trans.tsd.bluetooth;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import org.piax.trans.ts.bluetooth.BluetoothLocator;

import org.piax.trans.common.ServiceInfo;

import org.piax.trans.tsd.TSD;
import org.piax.trans.tsd.TSDListener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.util.Log;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

public class BluetoothTSD extends TSD {
    BluetoothAdapter mBluetoothAdapter;
    String advertiseString = null;
    public static final int DISCOVERABLE_MAX_DURATION = 300;
//    public static final int DISCOVERY_PERIOD = 40;
    public static final int DISCOVERY_PERIOD = 15;
    boolean isRunning;

    // XXX setDiscovereable is static method.
    //static Date lastAdvertised = null;
    static Date lastDiscovery = null;
    static Activity ctxt = null;
    
    public static final int DISCOVERY_STARTED_CODE = 100;
    public static boolean DISCOVERY_CONFIRMED = true;
    public static boolean ALWAYS_DISCOVERABLE = false;

    ArrayList<TSDListener> listeners;
    ArrayList<ServiceInfo> availableList;
    public static final String DISCOVERABLE_PREFIX = "piax:";
   // String deviceName;

    private void restoreDeviceName() {
        if (mBluetoothAdapter.getName() != null) {
            if (mBluetoothAdapter.getName().startsWith(DISCOVERABLE_PREFIX)) {
                mBluetoothAdapter.setName(mBluetoothAdapter.getName().substring(DISCOVERABLE_PREFIX.length()));
            }
        }
    }
    
    static public boolean isDiscoverable() {
        if (BluetoothAdapter.getDefaultAdapter() != null) {
            try {
                return (BluetoothAdapter.getDefaultAdapter().getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
            }
            catch (Exception e) {
                return false;
            }
        }
        return false;
    }
    
    static public void setDiscoverable() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            return;
        }
        int scanMode = -1;
        try {
            scanMode = adapter.getScanMode();
        }
        catch (Exception e) {
            return;
        }
        // setDiscoverableTimeout 
         if (scanMode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
             Method m1, m2;
             boolean discoverable = false;
             try {
                 m1 = BluetoothAdapter.getDefaultAdapter().getClass().getMethod("setDiscoverableTimeout", new Class[] {int.class});
                 if (m1 != null) {
                     m1.invoke(BluetoothAdapter.getDefaultAdapter(), 0);
                 }
                 m2 = BluetoothAdapter.getDefaultAdapter().getClass().getMethod("setScanMode", new Class[] {int.class}); 
                 if (m2 != null) {
                     m2.invoke(BluetoothAdapter.getDefaultAdapter(), BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
                 }
                 discoverable = true;
             } catch (Exception e) {
                 Log.d("AndroidBluetoothTSD", "*** invocation error");
             }
             if (!discoverable) {
                  // not confirmed...don't confirm twice.
                 if (!DISCOVERY_CONFIRMED) {
                      return;
                 }
                 DISCOVERY_CONFIRMED = false;
                 Intent dIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                 dIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_MAX_DURATION);
                 if (ctxt != null) {
                     ctxt.startActivityForResult(dIntent, DISCOVERY_STARTED_CODE);
                 }
             }
         }
    }
    
    private void advertise() {
        Date now = new Date();
        // run discovery at same time.
        if (lastDiscovery != null && lastDiscovery.getTime() + DISCOVERY_PERIOD * 1000 > now.getTime()) {
        }
        else {
            BluetoothLocator.startDiscovery();
            lastDiscovery = now;
        }
        
        if (mBluetoothAdapter.getName() == null) {
            return;
        }
        if (!mBluetoothAdapter.getName().startsWith(DISCOVERABLE_PREFIX)) {
            mBluetoothAdapter.setName(DISCOVERABLE_PREFIX + mBluetoothAdapter.getName());
        }
        if (ALWAYS_DISCOVERABLE) {
            setDiscoverable();
        }
    }
    
    class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //System.out.println("**** BLUETOOTH event:" + action);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                ServiceInfo info = null;
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //Parcelable[] uuidExtra = intent.getParcelableArrayExtra("android.bluetooth.device.extra.UUID");
                //if (uuidExtra != null) {
                //    System.out.println("*** UUIDEXTRA=" + uuidExtra);
                //}
                //System.out.println("*** FOUND " + device.getName() + "/" + device.describeContents() + "/" + device.getBluetoothClass());
                try {
                    Class cl = Class.forName("android.bluetooth.BluetoothDevice");
                    Class[] par = {};
                    Method method = cl.getMethod("getUuids", par);
                    Object[] args = {};
                    ParcelUuid[] retval = (ParcelUuid[])method.invoke(device, args);
                    if (retval != null) {
                        for (int i = 0; i < retval.length; i++) {
                            UUID uuid = retval[i].getUuid();
                            if (uuid.equals(BluetoothLocator.SERVICE_UUID)) {
                                //System.out.println("*** FOUND OUR UUID=" + uuid.toString());
                                info = ServiceInfo.create("org.piax.trans.ts.bluetooth.BluetoothLocator", null, -1, null, null, null);
                            }
                            else {
                                //System.out.println("*** UUID=" + uuid.toString());
                            }
                        }
                    }
                  //  else {
//                        Method m = cl.getMethod("fetchUuidsWithSdp", par);
//                        m.invoke(device, args);
//                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                if (info == null) {
                    if (device.getName() != null && device.getName().startsWith(DISCOVERABLE_PREFIX)) {
                        info = ServiceInfo.create("org.piax.trans.ts.bluetooth.BluetoothLocator", null, -1, null, null, null);
                    }
                }
                
                if (info != null) {
                    info.setHost(device.getAddress());
                    String host = info.getHost();
                    boolean found = false;
                    // XXX HOST Based...
                    if (host != null) {
                        synchronized (availableList) {
                            for (ServiceInfo a : availableList) {
                                if (host.equals(a.getHost())) {
                                    a.observed();
                                    found = true;
                                    break;
                                }
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
            if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
                //System.out.println("**** BLUETOOTH scan mode changed");
            }
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                //System.out.println("**** BLUETOOTH discovery started");
            }
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //System.out.println("**** BLUETOOTH discovery finished");
//                if (isRunning) {
//                    if (!BluetoothLocator.isCancelling()) {
//                        System.out.println("**** BLUETOOTH discovery AGAIN!");
//                        BluetoothLocator.startDiscovery();
//                    }
//                }
            }
        }
    }

    boolean isRegisterReceiver = false;
    BluetoothReceiver mBluetoothReceiver;
    private void startDiscoverDevices() {
        mBluetoothReceiver = new BluetoothReceiver();
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
//        filter.addAction(BluetoothDevice.ACTION_UUID);
        // hidden API. 
        //filter.addAction("android.bleutooth.device.action.UUID");
        if (mBluetoothReceiver != null) {
            isRegisterReceiver = false;
            ctxt.registerReceiver(mBluetoothReceiver, new IntentFilter(filter));
            BluetoothLocator.startDiscovery();
            isRegisterReceiver = true;
        }
    }
    
    private void stopDiscoverDevices() {
        if (mBluetoothReceiver != null) {
            if (isRegisterReceiver) {
                ctxt.unregisterReceiver(mBluetoothReceiver);
                BluetoothLocator.stopDiscovery();
            }
        }
    }
	
    public BluetoothTSD(Activity activity) {
        ctxt = activity;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        availableList = new ArrayList<ServiceInfo>();
        listeners = new  ArrayList<TSDListener>();
    }

    @Override
    public void addServiceListener(TSDListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void advertiseAll() {
        advertise();
    }

    @Override
    public void close() {
        stopDiscoverDevices();
        restoreDeviceName();
        isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public ServiceInfo[] list() {
        return availableList.toArray(new ServiceInfo[0]);
    }

    @Override
    public void registerService(ServiceInfo info) {
    }

    @Override
    public void removeServiceListener(TSDListener listener) {
    	synchronized(listeners) {
            listeners.remove(listener);
    	}
    }

    @Override
    public void setUnavailable(ServiceInfo info) {
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
    public void start() {
        startDiscoverDevices();
        advertise();
        isRunning = true;
    }

    @Override
    public void unregisterAllServices() {
//        if (advertiseString != null) {
//            advertiseString = null;
//            restoreDeviceName();
//        }
    }

    @Override
    public void unregisterService(ServiceInfo info) {
//        if (advertiseString != null) {
//            advertiseString = null;
//            restoreDeviceName();
//        }
    }
    @Override
    public boolean requiresWiFi() {
        return false;
    }
}
