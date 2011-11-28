/*
 * ServiceInfo.java
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

package org.piax.trans.common;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.lang.NumberFormatException;

import org.json.JSONException;
import org.json.JSONObject;
import org.piax.trans.common.impl.ServiceInfoImpl;
import org.piax.trans.util.AddressUtil;

public abstract class ServiceInfo {
    Date lastObserved;
    public static ServiceInfo create(String type, String host, int port, PeerId id, String name, String text) {
        return new ServiceInfoImpl(type, host, port, id, name, text);
    }
    
    public static ServiceInfo create(String type, InetSocketAddress addr, PeerId id, String name, String text) {
        return new ServiceInfoImpl(type, addr, id, name, text);
    }
    public static ServiceInfo create(String type, InetSocketAddress addr, String name, String text) {
        return new ServiceInfoImpl(type, addr, null, name, text);
    }
    public static ServiceInfo create(String type, int port, String name, String text) {
        return new ServiceInfoImpl(type, port, null, name, text);
    }
    public static ServiceInfo createByDictionary(JSONObject dict) {
        String type    = null;
        String name    = null;
        String host = null;
        Integer port   = null;
        PeerId id      = null;
        String text    = null;
        try {
            try {
                type = (String) dict.get("type");
            } catch (JSONException e) {}
            try {
                host = (String) dict.get("host");
            } catch (JSONException e) {}
            try {
                host = (String) dict.get("address"); // for compatibility
            } catch (JSONException e) {}
            try {
                port = (Integer) dict.get("port");
            } catch (JSONException e) {}
            try {
                id = new PeerId((String) dict.get("id"));
            } catch (JSONException e) {}
            try {
                text = (String) dict.get("text");
            } catch (JSONException e) {}
            InetSocketAddress a;
            a = AddressUtil.getAddress(host, port);
            ServiceInfo info = ServiceInfo.create(type, a, id, name, text);
            try {
                info.setRawName((String) dict.get("name"));
            } catch (JSONException e) {}
            return info;
        } catch (UnknownHostException e) {
            // XXX This means ServiceInfo can be null.
            e.printStackTrace();
            return null;
        }

    }
    abstract public ServiceInfo makeClone();
    public static ServiceInfo createByJson(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            return createByDictionary(obj);
        } catch (JSONException e) {
            e.printStackTrace();
        }     
        return null;
    }
    public JSONObject getDictionary() {
    	try {
            JSONObject untyped = new JSONObject();
            untyped.put("type", getType());
            untyped.put("name", getRawName());
            untyped.put("host", getHost());
            untyped.put("port", new Integer(getPort()));
            untyped.put("id", getId().toString());
            untyped.put("text", getText());
            return untyped;
    	} catch (JSONException e) {
            e.printStackTrace();
            return null;
    	}
    }
    public String getJson() {
        return getDictionary().toString();
    }
    public void observed () {
        lastObserved = new Date ();
    }
    public Date lastObserved () {
        return lastObserved;
    }
    public abstract String getType();
    public abstract PeerId getId();
    public abstract InetSocketAddress getInetSocketAddress();
    public abstract String getHost();
    public abstract int getPort();
    public abstract void setId(PeerId id);
    public abstract String getName();
    protected abstract String getRawName();
    public abstract void setName(String id);
    protected abstract void setRawName(String id);
    public abstract String getText();
    public abstract void setText(String id);
    public abstract void setHost(String host);
    //public abstract LinkStatus getLinkStatus ();
    
}
