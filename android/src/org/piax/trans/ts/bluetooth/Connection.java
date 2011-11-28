package org.piax.trans.ts.bluetooth;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.piax.trans.util.ByteBufferUtil;
import org.piax.trans.util.ByteUtil;

import android.bluetooth.BluetoothSocket;

public class Connection extends Thread {
    public BluetoothLocator locator;
    public BluetoothSocket socket;
    public boolean isServer;
    private BluetoothTransportService bts;
    private InputStream mmInStream;
    private OutputStream mmOutStream;
    private boolean isUseless;

    public Connection(BluetoothLocator locator, BluetoothSocket socket, boolean isServer, BluetoothTransportService bts) {
        this.locator = locator;
        this.socket = socket;
        this.isServer = isServer;
        this.isUseless = false;
        
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) { 
            e.printStackTrace();
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
        this.bts = bts;
        start();
    }
    
    private static final int SIZE_HEADER = 4;
    
    private ByteBuffer readFromStream(InputStream inStream) {
        byte[] msgLen = new byte[SIZE_HEADER];
        try {
            // header (4 bytes)
            int n = inStream.read(msgLen);
            if (n == -1) {
                return null;
            }
            if (n != SIZE_HEADER) {
                return null;
            }
            int len = ByteUtil.bytes2Int(msgLen);
            //System.out.println("*** LEN=" + len);
            if (len <= 0) {
                // broken message.
                return null;
            }
            ByteBuffer bb = ByteBufferUtil.newByteBuffer(len);
            int b = 0;
            for (int i = 0; i < len; i++) {
                b = inStream.read();
                if (b == -1) break;
                bb.put((byte) b);
            }
            if (b == -1) {
                return null;
            }
            ByteBufferUtil.flip(bb);
            return bb;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    
    private void reconnect() {
        BluetoothSocket socket = null;
        try {
            socket = BluetoothTransportService.getConnectedSocket(locator.remoteDevice);
        } catch (IOException e) {
            try {
                this.socket.close();
            } catch (IOException e1) {
                // ignore.
            }
            isUseless = true;
        }
        if (socket != null) {
            isServer = false;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e2) { }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
    }
    
    public void write(byte[] data, int offset, int length) {
        boolean canceled = BluetoothTransportService.cancelDiscovery();
        try {
            mmOutStream.write(data, offset, length);
            mmOutStream.flush();
            bts.locatorAvailable(locator);
        } catch (IOException e) {
            reconnect();
            try {
                mmOutStream.write(data, offset, length);
                mmOutStream.flush();
                bts.locatorAvailable(locator);
            } catch (IOException e1) {
                bts.locatorUnavailable(locator);
                //System.out.println("QUIT Sending.");
            }
        }
        if (canceled) {
            BluetoothTransportService.resumeDiscovery();
        }
    }
    
    public boolean isUseless() {
        return isUseless;
    }

    public void run() {
        while (!isUseless) {
            ByteBuffer bb = readFromStream(mmInStream);
            if (bb != null) {
                bts.receiveBytes(bb);
            }
            else {
                bts.locatorUnavailable(locator);
                isUseless = true;
            }   
        }
    }
}
