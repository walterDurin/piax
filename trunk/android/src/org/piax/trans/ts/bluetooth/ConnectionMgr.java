package org.piax.trans.ts.bluetooth;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;

public class ConnectionMgr {

    
    private ConcurrentHashMap<String, Connection> connectionMap;
    private BluetoothTransportService bts;

    public ConnectionMgr(BluetoothTransportService bts) {
        connectionMap = new ConcurrentHashMap<String, Connection>();
        this.bts = bts;
    }
    
    public void map(BluetoothLocator srcLocator, Connection serverConn) {
        Connection conn = connectionMap.get(srcLocator.getPeerNameCandidate());
        if (conn != null && conn.socket != null) {
            try {
                conn.socket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        connectionMap.put(srcLocator.getPeerNameCandidate(), serverConn);
    }
    
    public void sweeper() {
        
    }
    
    public void fin() {
        for(Connection conn : connectionMap.values()) {
            try {
                conn.socket.close();
            } catch (IOException e) {
            }
        }
    }
    
    public Connection getConnectionCreate(BluetoothLocator locator) throws IOException {
        Connection conn = connectionMap.get(locator.address);
        if (BluetoothLocator.getDefaultLocator() == null) {
            return null;
        }
        if (locator.address.equals(BluetoothLocator.getDefaultLocator().address)) {
            // This should not happen.
            return null;
        }
        if (conn == null || conn.isUseless()) {
            BluetoothSocket socket = null;
            socket = BluetoothTransportService.getConnectedSocket(locator.remoteDevice);
            if (socket != null) {
                Connection newConn = new Connection(locator, socket, false, bts);
                connectionMap.put(locator.getPeerNameCandidate(), newConn);
                conn = newConn;
            }
        }
        else {
//            System.out.println("***" + (conn.isServer? " SERVER " : "CLIENT") + " connection to " + locator.address + " already exists");
        }
        return conn;
    }
}
