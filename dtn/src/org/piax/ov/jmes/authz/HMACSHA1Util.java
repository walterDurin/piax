package org.piax.ov.jmes.authz;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.piax.trans.util.Base64;

public class HMACSHA1Util {
    public static final String ALGORITHM = "HmacSHA1";
    public static final String ENCODING = "UTF-8";

    private static byte[] sign(String baseString, SecretKey key)
        throws GeneralSecurityException, UnsupportedEncodingException {
        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(key);
        byte[] text = baseString.getBytes(ENCODING);
        return mac.doFinal(text);
    }
    
    static public String getSignature(String baseString, String keyString) {
        try {
            byte[] keyBytes = Base64.decode(keyString);
            SecretKey key = new SecretKeySpec(keyBytes, ALGORITHM);
            String signature = Base64.encodeBytes(sign(baseString, key));
            return signature;
        } catch (IOException e) {
        } catch (GeneralSecurityException e) {
        }
        return null;
    }

    public static boolean isValid(String signature, String baseString,
            String keyString) {
        try {
            byte[] keyBytes = Base64.decode(keyString);
            SecretKey key = new SecretKeySpec(keyBytes, ALGORITHM);
            byte[] expected = sign(baseString, key);
            byte[] actual = null;
            actual = Base64.decode(signature);
            return equals(expected, actual);
        } catch (IOException e) {
        } catch (GeneralSecurityException e) {
        }
        return false;
    }
    
    private static boolean equals(byte[] a, byte[] b) {
        if (a == null)
            return b == null;
        else if (b == null)
            return false;
        else if (b.length <= 0)
            return a.length <= 0;
        byte diff = (byte) ((a.length == b.length) ? 0 : 1);
        int j = 0;
        for (int i = 0; i < a.length; ++i) {
            diff |= a[i] ^ b[j];
            j = (j + 1) % b.length;
        }
        return diff == 0;
    }

    public boolean equals(String x, String y) {
        if (x == null)
            return y == null;
        else if (y == null)
            return false;
        else if (y.length() <= 0)
            return x.length() <= 0;
        char[] a = x.toCharArray();
        char[] b = y.toCharArray();
        char diff = (char) ((a.length == b.length) ? 0 : 1);
        int j = 0;
        for (int i = 0; i < a.length; ++i) {
            diff |= a[i] ^ b[j];
            j = (j + 1) % b.length;
        }
        return diff == 0;
    }

    static public void main(String args[]) {
        String sigStr = HMACSHA1Util.getSignature("hello","world");
        System.out.println(sigStr);
        if (HMACSHA1Util.isValid(sigStr,"hello","world")) {
            System.out.println("OK");
        }
        if (!HMACSHA1Util.isValid(sigStr,"hello2","world")) {
            System.out.println("OK");
        }
        
    }

}
