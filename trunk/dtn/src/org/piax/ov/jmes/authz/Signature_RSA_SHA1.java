package org.piax.ov.jmes.authz;

public class Signature_RSA_SHA1 extends SignatureAlgorithm {
    public String getSignature(String baseString, String keyStr) {
        return RSASHA1Util.getSignature(baseString, keyStr);
    }
    public boolean isValid(String signature, String baseString, String keyStr) {
        return RSASHA1Util.isValid(signature, baseString, keyStr);
    }
}