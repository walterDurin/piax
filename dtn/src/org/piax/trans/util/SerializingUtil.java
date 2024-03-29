/*
 * SerializingUtil.java
 * 
 * Copyright (c) 2006- Osaka University
 * Copyright (c) 2004-2005 BBR Inc, Osaka University
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
 * Revision History:
 * ---
 * 2007/01/18 designed and implemented by M. Yoshida.
 * 
 * $Id: SerializingUtil.java 183 2010-03-03 11:41:21Z yos $
 */
package org.piax.trans.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.nio.ByteBuffer;

import org.grlea.log.SimpleLogger;

/**
 * バイト列の serialize/deserialize をサポートするユーティリティ。
 * 
 * @author     Mikio Yoshida
 * @version    1.0.0
 */
public class SerializingUtil {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(SerializingUtil.class);

    private static final int INIT_BUF_SIZE = 1024;

    public static byte[] serialize(Object obj)
            throws ObjectStreamException {
        byte[] data = null;
        
        ByteArrayOutputStream bOut = new ByteArrayOutputStream(INIT_BUF_SIZE);
        ObjectOutputStream oOut = null;
        try {
            oOut = new ObjectOutputStream(bOut);
            oOut.writeObject(obj);
            oOut.flush();
            data = bOut.toByteArray();
        } catch (ObjectStreamException e) {
            throw e;
        } catch (IOException e) {
            // IOException does not occurred!
            log.error("Unexpected IOException occurred: " + e.getMessage());
        } finally {
            try {
                if (oOut != null) 
                    oOut.close();
            } catch (IOException e) {}
        }
        return data;
    }

    public static Serializable deserialize(ByteBuffer bytes)
            throws ClassNotFoundException, ObjectStreamException {
        return deserialize(bytes, null);
    }
    
    /**
     * ByteBufferからオブジェクトをdeserializeする。
     * ByteBufferのpositionは、deserializeされたバイト分進む。
     * 
     * @param bytes ByteBuffer
     * @param loader loader
     * @return deserializeされたオブジェクト
     * @throws ClassNotFoundException クラスが存在しない場合
     * @throws ObjectStreamException ObjectStreamに不整合を検知した場合
     */
    public static Serializable deserialize(ByteBuffer bytes, ClassLoader loader) 
            throws ClassNotFoundException, ObjectStreamException {
        byte[] b = bytes.array();
        int off = bytes.arrayOffset() + bytes.position();
        int len = bytes.limit() - bytes.position();
        Serializable obj = null;
        
        ByteArrayInputStream bIn = new ByteArrayInputStream(b, off, len);
        ObjectInputStream oIn = null;
        try {
            if (loader == null) {
                oIn = new ObjectInputStream(bIn);
            } else {
                oIn = new ObjectInputStreamX(bIn, loader);
            }
            obj = (Serializable) oIn.readObject();
            
            // 残りbyte数を求めて、bytes.postitionを進める
            bytes.position(bytes.position() + len - oIn.available());
        } catch (ObjectStreamException e) {
            throw e;
        } catch (IOException e) {
            // IOException does not occurred!
            log.error("Unexpected IOException occurred: " + e.getMessage());
        } finally {
            try {
                if (oIn != null) 
                    oIn.close();
            } catch (IOException e) {}
        }
        return obj;
    }

    public static Serializable deserialize(byte[] bytes) 
            throws ClassNotFoundException, ObjectStreamException {
        return deserialize(bytes, 0, bytes.length, null);
    }
    
    public static Serializable deserialize(byte[] bytes, ClassLoader loader) 
            throws ClassNotFoundException, ObjectStreamException {
        return deserialize(bytes, 0, bytes.length, loader);
    }
    
    public static Serializable deserialize(byte[] bytes, int offset, int length) 
            throws ClassNotFoundException, ObjectStreamException {
        return deserialize(bytes, offset, length, null);
    }

    public static Serializable deserialize(byte[] bytes, int offset, int length,
            ClassLoader loader) 
            throws ClassNotFoundException, ObjectStreamException {
        Serializable obj = null;
        
        ByteArrayInputStream bIn = new ByteArrayInputStream(bytes, offset, length);
        ObjectInputStream oIn = null;
        try {
            if (loader == null) {
                oIn = new ObjectInputStream(bIn);
            } else {
                oIn = new ObjectInputStreamX(bIn, loader);
            }
            obj = (Serializable) oIn.readObject();
        } catch (ObjectStreamException e) {
            throw e;
        } catch (IOException e) {
            // IOException does not occurred!
            log.error("Unexpected IOException occurred: " + e.getMessage());
        } finally {
            try {
                if (oIn != null) 
                    oIn.close();
            } catch (IOException e) {}
        }
        return obj;
    }
}
