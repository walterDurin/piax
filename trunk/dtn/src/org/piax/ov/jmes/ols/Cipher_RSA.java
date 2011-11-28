package org.piax.ov.jmes.ols;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;

import org.piax.ov.jmes.authz.RSAKeyUtil;
import org.piax.trans.util.Base64;

public class Cipher_RSA extends CipherAlgorithm {
    public byte[] encrypt(byte[] data, PublicKey publicKey) {
        byte[] cipherData = null;
        Cipher cipher;
        try {
            cipher = Cipher.getInstance("RSA/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            cipherData = cipher.doFinal(data);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
        return cipherData;
      }

    public byte[] decrypt(byte[] data, PrivateKey privateKey) {
        byte[] cipherData = null;
        Cipher cipher;
        try {
            cipher = Cipher.getInstance("RSA/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            cipherData = cipher.doFinal(data);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
        return cipherData;
    }
    
    public String encrypt(String data, String keyStr) {
        byte[] encrypted = encryptBytes(data.getBytes(), keyStr);
        return new String(encrypted);
    }
    
    public String decrypt(String data, String keyStr) {
        try {
            byte[] decrypted = decryptBytes(Base64.decode(data), keyStr);
            return new String(decrypted);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public byte[] decryptBytes(byte[] data, String keyStr) {
        try {
            PrivateKey privateKey = RSAKeyUtil.getPrivateFromPKCS8(Base64.decode(keyStr));
            if (privateKey == null) {
                return null;
            }
            byte[] decrypted = decrypt(data, privateKey);
            return decrypted;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public byte[] encryptBytes(byte[] data, String keyStr) {
        try {
            PublicKey publicKey = RSAKeyUtil.getPublicFromX509(Base64.decode(keyStr));
            byte[] crypted = encrypt(data, publicKey);
            return crypted;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}