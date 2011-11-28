package org.piax.ov.jmes.authz;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.piax.trans.util.Base64;

//import android.util.Log;

public class AccessToken {
    static final String VERSION = "1.0";

    // public_key-based signature algorithm
    static final String SIGNATURE_ALGORITHM_PUBKEY = "RSA-SHA1";

    // shared_key-based signature algorithm
    static final String SIGNATURE_ALGORITHM_SHAREDKEY = "HMAC-SHA1";

    // no signature

    static final String FIELD_VERSION = "version";
    static final String FIELD_TOKEN_SIGNATURE_ALGORITHM = "token_sig_alg"; // "RSA-SHA1", ..
    static final String FIELD_PUBLIC_KEY = "public_key";
    static final String FIELD_AUTHORITY = "authority";
    static final String FIELD_TOKEN_EXPIRES = "token_expires";
    static final String FIELD_TOKEN_ATTRIBUTES = "token_attr";
    static final String FIELD_TOKEN_SECRET_ATTRIBUTES = "token_secret_attr";
    static final String FIELD_TOKEN_SIGNATURE = "token_signature";

    public String version;
    public String tokenSigAlg;
    public String publicKey;
    public String authority;
    public Date tokenExpires;
    public String tokenSignature;
    public Map<String,String> tokenAttributes;
    public List<String> tokenSecretAttributes;
    
    public AccessToken() {
        this.version = VERSION;
        this.tokenSigAlg = null;
        this.publicKey = null;
        this.authority = null;
        this.tokenAttributes = null;
        this.tokenSecretAttributes = null;
        this.tokenExpires = null;
        this.tokenSignature = null;
    }

    public AccessToken(String authority, Map<String,String> tokenAttributes) {
        version = VERSION;
        tokenSigAlg = null;
        this.publicKey = null;
        this.authority = authority;
        this.tokenAttributes = tokenAttributes;
        this.tokenSecretAttributes = null;
        this.tokenExpires = null;
        this.tokenSignature = null;
    }
    
    // Full spec constructor.
    public AccessToken(Map<String,String> tokenAttributes,
            List<String> secretTokenAttributes,
            String sigAlg, String authority, String publicKeyStr,
            Date tokenExpires) {
        version = VERSION;
        this.tokenSigAlg = sigAlg;
        this.publicKey = publicKeyStr;
        this.authority = authority;
        this.tokenAttributes = tokenAttributes;
        this.tokenSecretAttributes = secretTokenAttributes;
        this.tokenExpires = tokenExpires;
        this.tokenSignature = null;
    }
    
    // NO secret attributes.
    public AccessToken(Map<String,String>tokenAttributes,
            String sigAlg, String authority, String publicKeyStr,
            Date tokenExpires) {
        version = VERSION;
        this.tokenSigAlg = sigAlg;
        this.publicKey = publicKeyStr;
        this.authority = authority;
        this.tokenAttributes = tokenAttributes;
        this.tokenSecretAttributes = null;
        this.tokenExpires = tokenExpires;
        this.tokenSignature = null;
    }
    
    // Just an attribute pack.
    public AccessToken(Map<String,String>tokenAttributes) {
        version = null;
        this.tokenAttributes = tokenAttributes;
        this.tokenSigAlg = null;
        this.publicKey = null;
        this.authority = null;
        this.tokenSecretAttributes = null;
        this.tokenExpires = null;
        this.tokenSignature = null;
    }

    protected ArrayList<String> getTokenSignatureBaseArray(Map<String,String> secAttrs) {
        ArrayList<String> attrs = new ArrayList<String>();
        if (secAttrs != null) {
            for (String key : secAttrs.keySet()) {
                attrs.add(key + "=" + secAttrs.get(key));
            }
        }
        for (String attr : tokenAttributes.keySet()) {
            attrs.add(attr + "=" + tokenAttributes.get(attr));
        }
        if (publicKey != null) {
            attrs.add(FIELD_PUBLIC_KEY + "=" + publicKey);
        }
        // XXX what matters?
        //else {
        //    return null;
       // }
        if (authority != null) {
            attrs.add(FIELD_AUTHORITY + "=" + authority);
        }
        if (tokenExpires != null) {
            attrs.add(FIELD_TOKEN_EXPIRES + "=" + tokenExpires.getTime());
        }
        return attrs;
    }
    
    protected String getSignatureBase(ArrayList<String> attrs) {
        if (attrs == null) return null;
        // sort by attribute name and value.
        Collections.sort(attrs, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((String) o1).compareTo((String)o2);
            }
        });
        
        String baseString = null;
        for (String attr : attrs) {
            if (baseString == null) {
                baseString = attr;
            }
            else {
                baseString += "&" + attr;
            }
        }
        //System.out.println("BASE=" + baseString);
        return baseString;
    }

    public String getAttribute(String name) {
        return tokenAttributes.get(name);
    }

    public void updateTokenSignature(String privateKeyStr) {
        if (tokenSigAlg != null) {
            SignatureAlgorithm alg = SignatureAlgorithm.getInstance(tokenSigAlg);
            if (alg != null) {
                String base = getSignatureBase(getTokenSignatureBaseArray(null));
                tokenSignature = alg.getSignature(base, privateKeyStr);
                //System.out.println("&&& token base=" + base);
                //System.out.println("&&& tokenSigAlg=" + tokenSigAlg);
                //System.out.println("&&& token private key =" + privateKeyStr);
                //System.out.println("&&& token genetareed signature =" + tokenSignature);
            }
        }
    }

    public boolean isValidToken(String keyStr, Map<String,String> secAttrs) {
        if (tokenSigAlg != null) {
            if (tokenSignature == null) {
                return false;
            }
            SignatureAlgorithm alg = SignatureAlgorithm.getInstance(tokenSigAlg);
            if (alg != null) {
                String base = getSignatureBase(getTokenSignatureBaseArray(secAttrs));
                boolean result = alg.isValid(tokenSignature,
                                             base,
                                             keyStr);
                //System.out.println("&&& token signature=" + tokenSignature);
                //System.out.println("&&& token base=" + base);
                //System.out.println("&&& token public key=" + keyStr);
                //System.out.println("&&& is valid token?=" + result);
                return result;
            }
            else {
                return false;
            }
        }
        else {
            return true;
        }
    }

    static public AccessToken parse(String jsonStr) {
        try {
            AccessToken accessToken = new AccessToken();
            accessToken.initWithJSONObject(new JSONObject(jsonStr));
            return accessToken;
        }
        catch (JSONException e) {}
        return null;
    }

    protected void initWithJSONObject(JSONObject obj) {
        try {
            version = (String) obj.get(FIELD_VERSION);
        } catch (JSONException e) {
            version = null;
        }            
        try {
            tokenSigAlg = (String) obj.get(FIELD_TOKEN_SIGNATURE_ALGORITHM);
        } catch (JSONException e) {
            tokenSigAlg = null;
        }            
        try {
            publicKey = (String) obj.get(FIELD_PUBLIC_KEY);
        } catch (JSONException e) {
            publicKey = null;
        }   
        try {
            tokenSecretAttributes = new ArrayList<String>();
            String secAttrsStr = (String) obj.get(FIELD_TOKEN_SECRET_ATTRIBUTES);
            String[] secAttrs = secAttrsStr.split(",");
            for (String field : secAttrs) {
                tokenSecretAttributes.add(field.trim());
            }
        } catch (JSONException e) {}
        try {
            authority = (String) obj.get(FIELD_AUTHORITY);
        } catch (JSONException e) {
            authority = null;
        }
        String attributesStr = null;
        try {
            attributesStr = (String) obj.get(FIELD_TOKEN_ATTRIBUTES);
        } catch (JSONException e) {
        }
        if (attributesStr != null) {
            if (attributesStr.length() != 0) {
                tokenAttributes = new HashMap<String,String>();
                String[] attributes = attributesStr.split(",");
                for (String field : attributes) {
                    try {
                        tokenAttributes.put(field, ((String)obj.get(field)).trim());
                    } catch (JSONException e) {
                    }
                }
            }
        }
        String tmp = null;
        try {
            tmp = (String)obj.get(FIELD_TOKEN_EXPIRES);
        } catch (JSONException e) {
        }
        tokenExpires = (tmp == null || tmp.length() == 0) ? null : new Date(Long.parseLong(tmp));
        try {
            tokenSignature = (String) obj.get(FIELD_TOKEN_SIGNATURE);
        } catch (JSONException e) {
            tokenSignature = null;
        }
    }

    private void checkAdd(JSONObject obj, String attr, String value) {
        if (tokenSecretAttributes == null ||
            !tokenSecretAttributes.contains(attr)) {
            try {
                obj.put(attr, value);
            } catch (JSONException e) {
            }
        }
    }
    
    protected JSONObject makeJSONObject() {
        JSONObject untyped = new JSONObject();
        checkAdd(untyped, FIELD_VERSION, version);
        if (tokenSigAlg != null) {
            checkAdd(untyped, FIELD_TOKEN_SIGNATURE_ALGORITHM, tokenSigAlg);
        }
        if (publicKey != null) {
            checkAdd(untyped, FIELD_PUBLIC_KEY, publicKey);
        }
        if (authority != null) {
            checkAdd(untyped, FIELD_AUTHORITY, authority);
        }
        if (tokenSecretAttributes != null) {
            String secAttributes = null;
            for (String attr : tokenSecretAttributes) {
                if (secAttributes == null) {
                    secAttributes = attr;
                }
                else {
                    secAttributes += "," + attr;
                }
                try {
                    untyped.put(FIELD_TOKEN_SECRET_ATTRIBUTES, secAttributes);
                } catch (JSONException e) {
                }
            }
        }
        String attributes = null;
        if (tokenAttributes != null) {
            for (String attr : tokenAttributes.keySet()) {
                if (attributes == null) {
                    attributes = attr;
                }
                else {
                    attributes += "," + attr;
                }
                checkAdd(untyped, attr, tokenAttributes.get(attr));
            }
            checkAdd(untyped, FIELD_TOKEN_ATTRIBUTES, attributes);
        }
        if (tokenExpires != null) { 
            checkAdd(untyped, FIELD_TOKEN_EXPIRES, tokenExpires.getTime() + "");
        }
        if (tokenSignature != null) {
            checkAdd(untyped, FIELD_TOKEN_SIGNATURE, tokenSignature);
        }
        return untyped;
    }

    public String toString() {
        JSONObject obj = makeJSONObject();
        return (obj == null) ? null : obj.toString();
    }

    public static void main(String[] arg) {
        // authority key pair;
        KeyPair kp = RSAKeyUtil.genKeyPair();
        PrivateKey prk = kp.getPrivate();
        PublicKey puk = kp.getPublic();
        byte[] prkb = RSAKeyUtil.getEncoded(prk);
        byte[] pukb = RSAKeyUtil.getEncoded(puk);
        String authorityPrivateKeyStr = Base64.encodeBytes(prkb);
        String authorityPublicKeyStr = Base64.encodeBytes(pukb);
        String authority = "authority-1";
        
        // user key pair;
        kp = RSAKeyUtil.genKeyPair();
        prk = kp.getPrivate();
        puk = kp.getPublic();
        prkb = RSAKeyUtil.getEncoded(prk);
        pukb = RSAKeyUtil.getEncoded(puk);
        String userPrivateKeyStr = Base64.encodeBytes(prkb);
        String userPublicKeyStr = Base64.encodeBytes(pukb);

        HashMap<String,String> map = new HashMap<String,String>();
        map.put("von_key", "von_key-1");
        map.put("name", "test");
        
        ArrayList<String> secretAttr = new ArrayList<String>();
        secretAttr.add("von_key");

        // Use HMAC-SHA1
        AccessToken token = new AccessToken(map,
                secretAttr,
                "HMAC-SHA1",
                authority,
                userPublicKeyStr,
                new Date());
        //System.out.println("sigbase:" + token.getSignatureBase(token.getTokenSignatureBaseArray(null)));
        token.updateTokenSignature(authorityPrivateKeyStr);
        //System.out.println("token=" + token);
        token = AccessToken.parse(token.toString());
        
        HashMap<String,String> smap = new HashMap<String,String>();
        smap.put("von_key", "von_key-1");
        
        if (token.isValidToken(authorityPrivateKeyStr, smap)) {
            //System.out.println("OK");
        }
        else {
            //System.out.println("NG");
        }

        // no authority id, RSA-SHA1
        token = new AccessToken(map, null, "RSA-SHA1", null, userPublicKeyStr, new Date());
        //System.out.println("sigbase:" + token.getSignatureBase(token.getTokenSignatureBaseArray(null)));
        token.updateTokenSignature(authorityPrivateKeyStr);
        //System.out.println("token=" + token);
        token = AccessToken.parse(token.toString());
        if (token.isValidToken(authorityPublicKeyStr, null)) {
            //System.out.println("OK");
        }
        else {
            //System.out.println("NG");
        }

        map = new HashMap<String,String>();
        map.put("von_key", "von_key-1");
        token = new AccessToken(authority, map);
        //System.out.println("token=" + token);
        if (token.isValidToken(null, null)) {
            //System.out.println("OK");
        }
        else {
            //System.out.println("NG");
        }
    }
}
