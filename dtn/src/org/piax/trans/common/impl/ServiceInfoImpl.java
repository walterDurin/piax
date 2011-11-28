package org.piax.trans.common.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;

import org.piax.trans.common.PeerId;
import org.piax.trans.common.ServiceInfo;
import org.piax.trans.util.Base64;

public class ServiceInfoImpl extends ServiceInfo {
    String type;
    String version;
    InetSocketAddress addr;
    String host;
    int port;
    PeerId id;
    String name;
    String text;
	
    public ServiceInfoImpl (String type, String host, int port, PeerId id, String name, String text) {
        this.type = type;
        this.host = host;
        this.port = port;
        if (port > 0) {
            addr = new InetSocketAddress(host, port);
        }
        else {
            addr = null;
        }
        this.id = id;
        if (name != null) {
            try {
                this.name = new String(Base64.encodeBytes(name.getBytes("UTF-8")));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                this.name = null;
            }
        }
        else {
            this.name = null;
        }
        this.text = text;
    }
    
    public ServiceInfoImpl (String type, InetSocketAddress addr, PeerId id, String name, String text) {
        this.type = type;
        this.addr = addr;
        this.id = id;
        if (name != null) {
            try {
                this.name = new String(Base64.encodeBytes(name.getBytes("UTF-8")));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                this.name = null;
            }
        }
        else {
            this.name = null;
        }
        this.text = text;
    }

    public ServiceInfoImpl (String type, int port, PeerId id, String name, String text) {
        this.type = type;
        this.port = port;
        this.addr = null;
        this.id = id;
        if (name != null) {
            try {
                this.name = new String(Base64.encodeBytes(name.getBytes("UTF-8")));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                this.name = null;
            }
        }
        else {
            this.name = null;
        }
        this.text = text;
    }

    public ServiceInfoImpl makeClone() {
        return new ServiceInfoImpl (type,
                                    addr,
                                    id,
                                    name,
                                    text);
    }
	
    @Override
    public InetSocketAddress getInetSocketAddress() {
        return addr;
    }
    
    @Override
    public String getHost() {
        if (addr != null) {
            return addr.getAddress().getHostAddress();
        }
        else {
            return host;
        }
    }
    
    @Override
    public PeerId getId() {
        return id;
    }
    
    @Override
    public String getName() {
        if (name == null) return null;
        try {
            return new String(Base64.decode(name), "UTF-8");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    protected String getRawName() {
        return name;
    }
    
    @Override
    public int getPort() {
        if (addr != null) {
            return addr.getPort();
        }
        else {
            return -1;
        }
    }
    
    @Override
    public String getText() {
        return text;
    }
    
    @Override
    public String getType() {
        return type;
    }
    
    @Override
    public void setId(PeerId id) {
        this.id = id;
    }
    
    @Override
    public void setName(String name) {
        if (name != null) {
            try {
                this.name = new String(Base64.encodeBytes(name.getBytes("UTF-8")));
            } catch (UnsupportedEncodingException e) {
                this.name = null;
                e.printStackTrace();
            }
        }
        else {
            this.name = null;
        }
    }

    @Override
    protected void setRawName(String name) {
        this.name = name;
    }

    @Override
    public void setText(String text) {
        this.text = text;
    }
    
    @Override
    public void setHost(String host) {
        this.host = host;
    }
    
}