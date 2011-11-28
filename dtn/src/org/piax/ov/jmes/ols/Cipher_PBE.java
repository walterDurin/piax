package org.piax.ov.jmes.ols;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.piax.trans.util.Base64;

public class Cipher_PBE extends CipherAlgorithm {
    public String encrypt(String data, String keyStr) {
        byte[] crypted = encryptBytes(data.getBytes(), keyStr);
        return Base64.encodeBytes(crypted);
    }
    public String decrypt(String data, String keyStr) {
        byte[] decoded = null;
        try {
            decoded = Base64.decode(data);
            return new String(decryptBytes(decoded, keyStr));
        } catch (IOException e) {
            return null;
        }
    }
    @Override
    public byte[] decryptBytes(byte[] data, String keyStr) {
        return PBEUtil.decrypt(data, keyStr);
    }
    @Override
    public byte[] encryptBytes(byte[] data, String keyStr) {
        return PBEUtil.encrypt(data, keyStr);
    }
}