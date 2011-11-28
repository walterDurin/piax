package org.piax.ov.jmes.authz;

public class Signature_HMAC_SHA1 extends SignatureAlgorithm {

    public String getSignature(String baseString, String keyStr) {
//        System.out.println("SIGN:" + baseString + ":" + keyStr);
        return HMACSHA1Util.getSignature(baseString, keyStr);
    }

    public boolean isValid(String signature, String baseString, String keyStr) {
        //System.out.println("VALID:" + baseString + ":" + keyStr);
        return HMACSHA1Util.isValid(signature, baseString, keyStr);
    }

}
