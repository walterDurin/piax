package org.piax.ov.jmes.ols;

// OLS - Objects with Layered Security;
public interface OLSKeyManager {
    public String getParentId(String id);
    public OLSAlgAndKey getEncryptionAlgAndKey(String id);
    public String getDecryptionKey(String alg, String id);
}