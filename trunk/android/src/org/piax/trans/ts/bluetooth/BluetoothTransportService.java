package org.piax.trans.ts.bluetooth;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

import org.piax.trans.common.PeerLocator;
import org.piax.trans.ts.BytesReceiver;
import org.piax.trans.ts.LocatorTransportSpi;
import org.piax.trans.util.ByteBufferUtil;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

public class BluetoothTransportService implements LocatorTransportSpi {
    BluetoothLocator peerLocator;
    BytesReceiver bytesReceiver;
    ConnectionMgr cmgr;
    boolean isTerminated;
    // This is our UUID.
    //public static final UUID SERVICE_UUID = UUID.fromString("322AAA69-5E51-4167-891F-6951C3FC9A49"); 
    BluetoothServerSocket aSocket;
    
    public BluetoothTransportService(
            BytesReceiver bytesReceiver,
            BluetoothLocator peerLocator) throws IOException {
        this.peerLocator = peerLocator;
        this.bytesReceiver = bytesReceiver;
        cmgr = new ConnectionMgr(this);
        isTerminated = false;
        new Listener(this).start();
    }
    
    public static int BLUETOOTH_PORT = 1;
    public static final int TRY_RECONNECTING_TIMES = 3;
    public static final double MAX_WAITING_TIME = 2000;
    public static final double MIN_WAITING_TIME = 1000;
    
    class Listener extends Thread {
        BluetoothTransportService bts;
        public Listener(BluetoothTransportService bts) {
            this.bts = bts;
        }
        @Override
        public void run() {
            try {
                while (!isTerminated) {
                    aSocket = //InsecureBluetooth.listenUsingRfcommWithServiceRecord(BluetoothAdapter.getDefaultAdapter(), "org.piax.trans.bluetooth", BluetoothLocator.SERVICE_UUID, true);
                    BluetoothAdapter.getDefaultAdapter().listenUsingInsecureRfcommWithServiceRecord("org.piax.trans.bluetooth", BluetoothLocator.SERVICE_UUID);
                //aSocket = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord("org.piax.trans.bluetooth", BluetoothLocator.SERVICE_UUID);
                //aSocket = InsecureBluetooth.listenUsingRfcomm(1, false);
                //aSocket = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord("org.piax.trans.bluetooth", SERVICE_UUID);

                    
                    BluetoothSocket sock;
                    sock = aSocket.accept();
                    BluetoothLocator loc = new BluetoothLocator(sock.getRemoteDevice().getAddress());
                    //System.out.println("*** ACCEPTED FROM:" + loc.address);
                    Connection conn = new Connection(loc, sock, true, bts);
                    cmgr.map(loc, conn);
                    aSocket.close();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
        
    static public boolean cancelDiscovery() {
        if (BluetoothAdapter.getDefaultAdapter().isDiscovering()) {
            BluetoothLocator.cancelDiscovery();
            return true;
        }
        return false;
    }
    
    static public void resumeDiscovery() {
        BluetoothLocator.resumeDiscovery();
    }
    
    static public BluetoothSocket getConnectedSocket(BluetoothDevice remoteDevice) throws IOException {
        boolean cancelled = cancelDiscovery();
        BluetoothSocket socket = null;
        socket = //InsecureBluetooth.createRfcommSocketToServiceRecord(remoteDevice, BluetoothLocator.SERVICE_UUID, true);
                remoteDevice.createInsecureRfcommSocketToServiceRecord(BluetoothLocator.SERVICE_UUID);
        //socket = InsecureBluetooth.createRfcommSocket(remoteDevice, 1, false);
      //  socket = remoteDevice.createRfcommSocketToServiceRecord(BluetoothLocator.SERVICE_UUID);
        socket.connect();
//        if (cancelled) {
//            resumeDiscovery();
//        }
        return socket;
    }
    
    @Override
    public boolean canSend(PeerLocator target) {
        return peerLocator.sameClass(target);
    }

    @Override
    public boolean canSet(PeerLocator target) {
        return target instanceof BluetoothLocator && peerLocator == null;
    }

    @Override
    public void fin() {
        isTerminated = true;
        try {
            if (aSocket != null) {
                aSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        cmgr.fin();
    }

    @Override
    public PeerLocator getLocator() {
        return peerLocator;
    }

    @Override
    public void sendBytes(boolean isSend, PeerLocator toPeer, ByteBuffer msg)
            throws IOException {
        // sendBytes is called synchronously...?
        // TS need not to take care about simultaneous send requests since they are queued in the caller side.
        Connection conn = cmgr.getConnectionCreate((BluetoothLocator)toPeer);
        if (conn == null || conn.isUseless()) {
            return;
        }
        int len = msg.remaining();
        ByteBuffer bb = ByteBufferUtil.reserve(4, msg);
        bb.putInt(len);
        ByteBufferUtil.rewind(bb);
        conn.write(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining());
        //System.out.println("*** SENT " + len + " BYTES to " + toPeer.getPeerNameCandidate());
    }

    @Override
    public void setLocator(PeerLocator locator) {
        peerLocator = (BluetoothLocator) locator;
    }
    
    void receiveBytes(ByteBuffer msg) {
        bytesReceiver.receiveBytes(this, msg);
    }
    
    void locatorUnavailable(PeerLocator locator) {
        bytesReceiver.locatorUnavailable(locator);
    }
    
    void locatorAvailable(PeerLocator locator) {
        bytesReceiver.locatorAvailable(locator);
    }

}
