package org.piax.trans.ts.bluetooth;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.UUID;

import org.piax.trans.common.PeerId;
import org.piax.trans.common.PeerLocator;
import org.piax.trans.common.ServiceInfo;
import org.piax.trans.ts.BytesReceiver;
import org.piax.trans.ts.LocatorTransportSpi;
import org.piax.trans.util.ByteUtil;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

public class BluetoothLocator extends PeerLocator {
    /**
     * 
     */
    private static final long serialVersionUID = -9208014968749554482L;
    
    // Original UUID does not work ?
    //public static final UUID SERVICE_UUID = UUID.fromString("322AAA69-5E51-4167-891F-6951C3FC9A49"); 
    
    public static final UUID SERVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    
    
    BluetoothDevice remoteDevice;
    String address;
    static final public byte ID = 20; 
    static private boolean cancelling;
    static private boolean stopping;
    
    static public boolean cancelDiscovery() {
        if (BluetoothAdapter.getDefaultAdapter().isDiscovering()) {
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            cancelling = true;
            //System.out.println("*** CANCELED.");
            return true;
        }
        return false;
    }
    
    static public void stopDiscovery() {
        if (BluetoothAdapter.getDefaultAdapter().isDiscovering()) {
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            stopping = true;
            //System.out.println("*** STOPPED.");
        }
    }
    
    static public void startDiscovery() {
        BluetoothAdapter.getDefaultAdapter().startDiscovery();
        //System.out.println("*** STARTED.");
        cancelling = false;
        stopping = false;
    }
    
    static public void resumeDiscovery() {
        if (cancelling) {
            if (!stopping) {
                BluetoothAdapter.getDefaultAdapter().startDiscovery();
                //System.out.println("*** RESUMED.");
                cancelling = false;
            }
            else {
                //System.out.println("*** STOPPING. Not RESUMED.");
            }
        }
    }

    static public void ensureDiscovery() {
        if (!BluetoothAdapter.getDefaultAdapter().isDiscovering() &&               
                !stopping && !cancelling) {
            if (!BluetoothAdapter.getDefaultAdapter().isDiscovering()) {
                //System.out.println("*** NOT STARTED. Start.");
            }
            startDiscovery();
            if (!BluetoothAdapter.getDefaultAdapter().isDiscovering()) {
                //System.out.println("*** NOT STARTED. WHY??");
            }
        }
    }
    
    static public boolean isCancelling() {
        return cancelling;
    }
    
    static private boolean isValidAddress(String address) {
        try {
            macStr2Bytes(address);
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }
    
    static public BluetoothLocator getDefaultLocator() {
        // It assumes one device per peer.
        String address = BluetoothAdapter.getDefaultAdapter().getAddress();
        if (isValidAddress(address)) {
            return new BluetoothLocator(address);
        }
        return null;
    }
    
    public BluetoothLocator(String address) {
        this.address = address;
        remoteDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
    }
    
    public BluetoothLocator(ByteBuffer bb) {
        byte[] macBytes = new byte[6];
        bb.get(macBytes);
        this.address = bytes2macStr(macBytes);
        remoteDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
    }
    
    // 3C:5A:37:91:B9:01 => byte[6]
    public static byte[] macStr2Bytes(String macStr) throws IllegalArgumentException {
        String s = new String(macStr).replaceAll(":", "");
        return ByteUtil.hex2Bytes(s);
    }
    
    public static String bytes2macStr(byte[] b) {
        return String.format("%02X:%02X:%02X:%02X:%02X:%02X", b[0], b[1], b[2], b[3], b[4], b[5]);
    }
    
    public byte[] getBytes() {
        return macStr2Bytes(address);
    }
   
    public BluetoothDevice getBluetoothDevice() {
        return remoteDevice;
    }
    
    @Override
    public byte getId() {
        return ID;
    }

    @Override
    public int getPackLen() {
        return 6;
    }

    @Override
    public String getPeerNameCandidate() {
        return address;
    }

    @Override
    public ServiceInfo getServiceInfo() {
        return ServiceInfo.create(this.getClass().getName(), address, -1, null, null, "");
    }

    @Override
    public LocatorTransportSpi newLocatorTransportService(
            BytesReceiver bytesReceiver, Collection<PeerLocator> relays)
            throws IOException {
        return new BluetoothTransportService(bytesReceiver, this);
    }

    @Override
    public void pack(ByteBuffer bbuf) {
        byte[] macBytes = macStr2Bytes(address);
        bbuf.put(macBytes);
    }

    @Override
    public boolean sameClass(PeerLocator target) {
        if (target == null) return false;
        return this.getClass().equals(target.getClass());
    }
    
    @Override
    public boolean equals(Object target) {
        return (sameClass((PeerLocator)target) && address.equals(((BluetoothLocator)target).address));
    }

}
