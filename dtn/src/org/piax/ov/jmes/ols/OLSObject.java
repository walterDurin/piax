package org.piax.ov.jmes.ols;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.json.JSONException;
import org.json.JSONObject;
import org.piax.trans.util.Base64;

public class OLSObject {
    public String type = null;
    public String id = null;
    public String alg = null;
    public byte data[] = null;
    
    public OLSObject() {
    }
    
    public OLSObject(String text) {
        type = "text/plain";
        this.data = text.getBytes();
    }
    
    public OLSObject(String id, String alg, String data) {
        type = "encrypted";
        this.id = id;
        this.alg = alg;
        try {
            this.data = Base64.decode(data);
        } catch (IOException e) {
        }
    }
    
    public OLSObject(String id, String alg, byte[] data) {
        type = "encrypted";
        this.id = id;
        this.alg = alg;
        this.data = data;
    }
    
    public String getText() {
        if (type.equals("text/plain")) {
            try {
                return new String(data, "UTF-8");
            } catch (UnsupportedEncodingException e) {      
            }
        }
        if (type.equals("encrypted")) {
            return Base64.encodeBytes(data);
        }
        return null;
    }
    
    public static OLSObject parse(String text) throws JSONException {
        OLSObject obj = new OLSObject(); 
        JSONObject json = new JSONObject(text);
        if (json.has("type")) {
            obj.type = json.getString("type");
        }
        if (json.has("id")) {
            obj.id = json.getString("id");
        }
        if (json.has("alg")) {
            obj.alg = json.getString("alg");
        }
        if (json.has("data")) {
            try {
                obj.data = Base64.decode(json.getString("data"));
            } catch (IOException e) {
            }
        }
        return obj;
    }
    
    static byte MAGIC = (byte)0xca;

    public static OLSObject parseBytes(byte[] dataBytes) {
        if (dataBytes == null) {
            return null;
        }
        OLSObject obj = new OLSObject();
        ByteBuffer buff = ByteBuffer.allocateDirect(dataBytes.length);
        buff.put(dataBytes);
        buff.flip();
        byte magick = buff.get();
        if (magick != MAGIC) {
            return null;
        }
        int len = buff.getInt();
        byte[] strBuf = new byte[len];
        buff.get(strBuf);
        obj.type = new String(strBuf);
        len = buff.getInt();
        strBuf = new byte[len];
        buff.get(strBuf);
        obj.id = new String(strBuf);
        len = buff.getInt();
        strBuf = new byte[len];
        buff.get(strBuf);
        obj.alg = new String(strBuf);
        len = buff.getInt();
        obj.data = new byte[len];
        buff.get(obj.data);
        return obj;
    }

    public byte[] toBytes() {
        byte[] typeByte = type.getBytes();
        byte[] idByte = (id == null) ? null : id.getBytes();
        byte[] algByte = (alg == null) ? null : alg.getBytes();
        int idByteLength = (id == null) ? 0 : idByte.length;
        int algByteLength = (alg == null) ? 0 : algByte.length;
        int length = 1 + 4 + 4 + 4 + 4 + typeByte.length + idByteLength + algByteLength + data.length;
        ByteBuffer buff = ByteBuffer.allocateDirect(length);
        buff.put(MAGIC);
        buff.putInt(typeByte.length);
        buff.put(typeByte);
        buff.putInt(idByteLength);
        if (idByte != null) {
            buff.put(idByte);
        }
        buff.putInt(algByteLength);
        if (algByte != null) {
            buff.put(algByte);           
        }
        buff.putInt(data.length);
        buff.put(data);
        buff.flip();
        byte[] obtained = new byte[length];
        buff.get(obtained);
        return obtained;
    }
    
    public String toString() { 
        JSONObject json = new JSONObject();
        try {
            if (type != null) {
                json.put("type", type);
            }
            if (id != null) {
                json.put("id", id);
            }
            if (alg != null) {
                json.put("alg", alg);
            }
            if (data != null) {
                json.put("data", Base64.encodeBytes(data));
            }
            return json.toString();
        } catch (JSONException e) {
            return null;
        }
    }
}
