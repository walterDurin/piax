package org.piax.ov.jmes.authz;

public abstract class SignatureAlgorithm {
    static public SignatureAlgorithm getInstance(String sigAlg) {
        try {
            return (SignatureAlgorithm) Class.forName("org.piax.ov.jmes.authz.Signature_" + sigAlg.replace('-', '_')).newInstance();
        } catch (IllegalAccessException e) {
        } catch (InstantiationException e) {
        } catch (ClassNotFoundException e) {
        }
        return null;
    }
    abstract public String getSignature(String baseString, String keyStr);
    abstract public boolean isValid(String signature, String baseString, String keyStr);
}