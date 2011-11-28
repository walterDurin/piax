package org.piax.ov.jmes.authz;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

import org.piax.trans.util.Base64;

public class RSASHA1Util {
    public static final String ALGORITHM = "SHA1withRSA";
    public static final String ENCODING="UTF-8";

    static public String getSignature(String baseString, String privateKeyString) {
        if (baseString == null || privateKeyString == null) {
            return null;
        }
        try {
            PrivateKey privateKey = RSAKeyUtil.getPrivateFromPKCS8(Base64.decode(privateKeyString));
            byte[] signature = sign(baseString.getBytes(ENCODING), privateKey);
            return Base64.encodeBytes(signature);
        } catch (IOException e) {
                e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return null;
    }

    static public boolean isValid(String signature, String baseString, String publicKeyString) {
        try {
            //System.out.println("PUBKEY=" + publicKeyString);
            PublicKey publicKey = RSAKeyUtil.getPublicFromX509(Base64.decode(publicKeyString));
            return verify(Base64.decode(signature),
                          baseString.getBytes(ENCODING),
                          publicKey);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return false;
    }

    static private byte[] sign(byte[] message, PrivateKey privateKey) throws GeneralSecurityException {
        Signature signer = Signature.getInstance(ALGORITHM);
        signer.initSign(privateKey);
        signer.update(message);
        return signer.sign();
    }

    static private boolean verify(byte[] signature, byte[] message, PublicKey publicKey)
        throws GeneralSecurityException {
        if (publicKey == null) {
            throw new IllegalStateException("need to set public key with " +
                                            " OAuthConsumer.setProperty when " +
                                            "verifying RSA-SHA1 signatures.");
        }
        Signature verifier = Signature.getInstance("SHA1withRSA");
        verifier.initVerify(publicKey);
        verifier.update(message);
        return verifier.verify(signature);
    }
    
    public static void main(String args[]) {
        KeyPair kp = RSAKeyUtil.genKeyPair();
        PrivateKey prk = kp.getPrivate();
        PublicKey puk = kp.getPublic();
        byte[] prkb = RSAKeyUtil.getEncoded(prk);
        byte[] pukb = RSAKeyUtil.getEncoded(puk);
        String privateKeyStr = Base64.encodeBytes(prkb);
        String publicKeyStr = Base64.encodeBytes(pukb);
        System.out.println("Private=" + privateKeyStr);
        System.out.println("Public=" + publicKeyStr);
        String sigStr = RSASHA1Util.getSignature("hello", privateKeyStr);
        System.out.println("Signature=" + sigStr);
        if (RSASHA1Util.isValid(sigStr, "hello", publicKeyStr)) {
            System.out.println("OK");
        }
        if (!RSASHA1Util.isValid(sigStr, "hello2", publicKeyStr)) {
            System.out.println("OK");
        }
    }
}
