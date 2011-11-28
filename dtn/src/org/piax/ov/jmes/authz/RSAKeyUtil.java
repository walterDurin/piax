package org.piax.ov.jmes.authz;

import java.security.InvalidKeyException;
import java.security.KeyPairGenerator;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.piax.trans.util.Base64;


public class RSAKeyUtil {
    // RSA keypair
    static public final String KEY_ALGORITHM = "RSA";
    static public final int NUM_BITS = 1024;
    static public final String PRIVATE_KEY_FORMAT = "PKCS#8";
    static public final String PUBLIC_KEY_FORMAT = "X.509";

    static public KeyPair genKeyPair() {
        KeyPairGenerator keyGen;
        try {
            keyGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyGen.initialize(NUM_BITS);
            return keyGen.genKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    static public PrivateKey getPrivateFromPKCS8(byte[] pkcs8){
        EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(pkcs8);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            return keyFactory.generatePrivate(privateKeySpec);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    static public PublicKey getPublicFromX509(byte[] x509){
        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(x509);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            return keyFactory.generatePublic(publicKeySpec);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }

    static public byte[] getEncoded(PrivateKey privateKey) {
        return privateKey.getEncoded();
    }

    static public byte[] getEncoded(PublicKey publicKey) {
        return publicKey.getEncoded();
    }
    


    public static void main(String[] args) {
        KeyPair kp = RSAKeyUtil.genKeyPair();
        PrivateKey prk = kp.getPrivate();
        PublicKey puk = kp.getPublic();
        byte[] prkb = RSAKeyUtil.getEncoded(prk);
        byte[] pukb = RSAKeyUtil.getEncoded(puk);
        System.out.println("Private=" + Base64.encodeBytes(prkb));
        System.out.println("Public=" + Base64.encodeBytes(pukb));
    }

}