package org.piax.ov.jmes.ols;

public abstract class CipherAlgorithm {
    static public CipherAlgorithm getInstance(String cipherAlg) {
        try {
            return (CipherAlgorithm) Class.forName("org.piax.ov.jmes.ols.Cipher_" + cipherAlg.replace('-', '_')).newInstance();
        } catch (IllegalAccessException e) {
        } catch (InstantiationException e) {
        } catch (ClassNotFoundException e) {
        }
        return null;
    }
    abstract public String encrypt(String data, String keyStr);
    abstract public byte[] encryptBytes(byte[] data, String keyStr);
    abstract public String decrypt(String data, String keyStr);
    abstract public byte[] decryptBytes(byte[] data, String keyStr);
}