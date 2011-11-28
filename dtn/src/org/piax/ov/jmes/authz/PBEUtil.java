package org.piax.ov.jmes.authz;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.piax.trans.util.Base64;

public class PBEUtil {
    private static final String KEY_FACTORY = "PBEWITHSHA-256AND256BITAES-CBC-BC";
    //    private static final String KEY_FACTORY="PBEWithMD5AndTripleDES";
    private static final int KEY_ITERATION_COUNT = 100;
    private static final byte[] salt = {
        (byte)0xA1, (byte)0x02, (byte)0xC3, (byte)0x36,
        (byte)0xD9, (byte)0x92, (byte)0xF4, (byte)0x10
    };
    static HashMap<String,Cipher> ecache;
    static HashMap<String,Cipher> dcache;
    
    private static Cipher createCipher(String password, boolean enc) {
        Cipher ret;
        PBEKeySpec keyspec = new PBEKeySpec(password.toCharArray(),
                                            salt,
                                            KEY_ITERATION_COUNT,
                                            32);
        try {      
            SecretKeyFactory skf = SecretKeyFactory.getInstance(KEY_FACTORY);
            SecretKey key = skf.generateSecret(keyspec);
            AlgorithmParameterSpec aps = new PBEParameterSpec(salt,
                                                              KEY_ITERATION_COUNT);
            ret = Cipher.getInstance(KEY_FACTORY);
            if (enc) {
                ret.init(Cipher.ENCRYPT_MODE, key, aps);
            }
            else {
                ret.init(Cipher.DECRYPT_MODE, key, aps);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return ret;
    }

    private static Cipher getCipher(String password, boolean enc) {
        Cipher ret;
        HashMap<String,Cipher> cache = enc ? ecache : dcache;
        if (cache == null) {
            cache = new HashMap<String,Cipher>();
            if (enc) {
                ecache = cache;
            }
            else {
                dcache = cache;
            }
        }
        ret = cache.get(password);
        if (ret == null) {
            ret = createCipher(password, enc);
            if (enc) {
                ecache.put(password, ret);
            }
            else {
                dcache.put(password, ret);
            }
        }
        return ret;
    }

    public static byte[] encrypt (byte[] bytes, String password) {
        Cipher cipher = getCipher(password, true);
        if (cipher != null) {
            try {
                return cipher.doFinal(bytes);
            } catch (Exception e) {
            }
        }
        return null;
    }

    public static byte[] decrypt (byte[] bytes, String password) {
        Cipher cipher = getCipher(password, false);
        if (cipher != null) {
            try {
                return cipher.doFinal(bytes);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    static public void main(String args[]) {
        String str = "hello,world";
        byte[] enc;
        try {
            enc = PBEUtil.encrypt(str.getBytes("UTF8"), "sesame");
            System.out.println("enc=" + Base64.encodeBytes(enc));
            System.out.println("dec=" + new String(PBEUtil.decrypt(enc, "sesame"), "UTF8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
