package org.piax.ov.jmes.authz;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.piax.trans.util.Base64;

public class AccessKey extends AccessToken {
    static final String FIELD_SIGNATURE_ALGORITHM = "sig_alg";
    static final String FIELD_NONCE = "nonce";
    static final String FIELD_ATTRIBUTES = "attributes"; // "text"
    static final String FIELD_SIGNATURE = "signature";
    
    static final String SIGNATURE_ALGORITHM_ASYNMETRIC = "RSA-SHA1";
    public String sigAlg;
    public String nonce;
    public Map<String,String> attributes;
    public String signature;

    public AccessKey() {
        super();
        this.sigAlg = null;
        this.nonce = null;
        this.attributes = null;
        this.signature = null;
    }

    // same as parent.
    public AccessKey(AccessToken token) {
        super();
        this.version = token.version;
        this.tokenSigAlg = token.tokenSigAlg;
        this.publicKey = token.publicKey;
        this.authority = token.authority;
        this.tokenAttributes = token.tokenAttributes;
        this.tokenSecretAttributes = token.tokenSecretAttributes;
        this.tokenExpires = token.tokenExpires;
        this.tokenSignature = token.tokenSignature;
        this.sigAlg = null;
        this.nonce = null;
        this.attributes = null;
        this.signature = null;
    }

    public AccessKey(String nonce, Map<String,String> attributes, AccessToken token) {
        this(nonce, attributes, SIGNATURE_ALGORITHM_ASYNMETRIC, token);
    }

    public AccessKey(String nonce, Map<String,String> attributes, String sigAlg, AccessToken token) {
        this(token);
        this.tokenSignature = token.tokenSignature;
        this.sigAlg = sigAlg;
        this.nonce = nonce;
        this.attributes = attributes;
        this.signature = null;
    }    

    // simple access key consists with only attributes.
    public AccessKey(Map<String,String> attributes) {
        super();
        this.attributes = attributes;
    }
    
    public AccessKey(Map<String,String> tokenAttributes, Map<String,String> attributes) {
        super(tokenAttributes);
        this.attributes = attributes;
    }

    public static AccessKey fromJSONObject(JSONObject obj) {
        AccessKey ret = new AccessKey();
        ret.initWithJSONObject(obj);
        return ret;
    }
    
    protected void initWithJSONObject(JSONObject obj) {
        super.initWithJSONObject(obj);
        try {
            sigAlg = (String) obj.get(FIELD_SIGNATURE_ALGORITHM);
        } catch (JSONException e) {
            sigAlg = null;
        } 
        
        String attributesStr = null;
        try {
            attributesStr = (String) obj.get(FIELD_ATTRIBUTES);
        } catch (JSONException e) {
        }
        if (attributesStr != null) {
            if (attributesStr.length() != 0) {
                attributes = new HashMap<String,String>();
                String[] attrs = attributesStr.split(",");
                for (String attr : attrs) {
                    try {
                        attributes.put(attr, ((String)obj.get(attr)).trim());
                    } catch (JSONException e) {
                    }
                }
            }
        }
        try {
            signature = (String) obj.get(FIELD_SIGNATURE);
        } catch (JSONException e) {
        }
        try {
            nonce = (String) obj.get(FIELD_NONCE);
        } catch (JSONException e) {
        }
    }

    private ArrayList<String> getSignatureBaseArray() {
        ArrayList<String> array = new ArrayList<String>();
        if (tokenSignature != null) {
            array.add(FIELD_TOKEN_SIGNATURE + "=" + tokenSignature);
        }
        if (nonce != null) {
            array.add(FIELD_NONCE + "=" + nonce);
        }
        if (attributes != null) {
            for (String attr : attributes.keySet()) {
                array.add(attr + "=" + attributes.get(attr));
            }
        }
        return array;
    }

    public String getAttribute(String name) {
        String attr = null;
        if (attributes != null) {
            attr = attributes.get(name);
        }
        if (attr == null && tokenAttributes != null) {
            return tokenAttributes.get(name);
        }
        else {
            return attr;
        }
    }
    
    public void removeAttribute(String name) {
        if (attributes != null) {
            attributes.remove(name);
        }
        if (tokenAttributes != null) {
            tokenAttributes.remove(name);
        }
    }

    static public AccessKey parse(String jsonStr) {
        try {
            AccessKey accessKey = new AccessKey();
            accessKey.initWithJSONObject(new JSONObject(jsonStr));
            return accessKey;
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateSignature(String privateKeyStr) {
        if (sigAlg != null) {
            SignatureAlgorithm alg = SignatureAlgorithm.getInstance(SIGNATURE_ALGORITHM_ASYNMETRIC);
            if (alg != null) {
                String base = getSignatureBase(getSignatureBaseArray());
                //System.out.println("&&& base=" + base);
                //System.out.println("&&& priv key=" + privateKeyStr);
                signature = RSASHA1Util.getSignature(base, privateKeyStr);
            }
        }
    }
    
    public boolean isValid(String keyStr) {
        return isValid(keyStr, null);
    }
    
    public boolean isValid(String keyStr, Map<String,String> map, Map<String,String> secAttrs) {
        attributes = map;
        return isValid(keyStr, secAttrs);
    }

    public boolean isValid(String keyStr, Map<String,String> secAttrs) {
        boolean sigOK = false;
        boolean tokenOK = false;
        SignatureAlgorithm tAlg = null;
        SignatureAlgorithm sAlg = null;
        if (sigAlg == null) {
            //sigOK = false;
        }
        else {
            sAlg = SignatureAlgorithm.getInstance(sigAlg);
        }
        if (tokenSigAlg == null) {
            //tokenOK = false;
        }
        else {
            tAlg = SignatureAlgorithm.getInstance(tokenSigAlg);
        }
        //        if (sigOK // tokenOK) {
        //            return true;
        //        }
        if (sAlg == null) {
            //System.out.println("reason1");
            sigOK = false;
        }
        else {
            if (signature == null)  {
                //System.out.println("reason2");
                sigOK = false;
            }
            else {
                if (publicKey == null) {
                    publicKey = secAttrs.get("public_key");
                }
                String base = getSignatureBase(getSignatureBaseArray());
                sigOK = sAlg.isValid(signature,
                        base,
                        publicKey);
              //  System.out.println("&&& key signature=" + this.signature);
              //  System.out.println("&&& key base=" + base);
              //  System.out.println("&&& is valid key?=" + sigOK);                
                //if (!sigOK) {
                   // System.out.println("reason5");
                //}
            }
        }
        if (tAlg == null) {
            //System.out.println("reason3");
            tokenOK = false;
        }
        else {
            if (tokenSignature == null) {
               // System.out.println("reason4");
                tokenOK = false;
            }
            else {
                String base = getSignatureBase(getTokenSignatureBaseArray(secAttrs));
                tokenOK = tAlg.isValid(tokenSignature, base, keyStr);
              //  System.out.println("&&& token signature(" + tokenSigAlg +")="+ this.tokenSignature);
            //    System.out.println("&&& token base=" + base);
             //   System.out.println("&&& keyStr=" + keyStr);
             //   System.out.println("&&& is valid token?=" + tokenOK);
            }
        }
        return sigOK && tokenOK;
    }

    public JSONObject makeJSONObject() {
        JSONObject untyped = super.makeJSONObject();
        try {
            if (sigAlg != null) {
                untyped.put(FIELD_SIGNATURE_ALGORITHM, sigAlg);
            }
            if (nonce != null)
                untyped.put(FIELD_NONCE, nonce);
            String attributes_field = null;
            if (attributes != null) {
                for (String field : attributes.keySet()) {
                    if (attributes_field == null) {
                        attributes_field = field;
                    }
                    else {
                        attributes_field += "," + field;
                    }
//                    untyped.put(field, attributes.get(field));
                }
                untyped.put(FIELD_ATTRIBUTES, attributes_field);
            }
            if (signature != null)
                untyped.put(FIELD_SIGNATURE, signature);
            return untyped;
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
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
        
        // user key pair;
        kp = RSAKeyUtil.genKeyPair();
        prk = kp.getPrivate();
        puk = kp.getPublic();
        prkb = RSAKeyUtil.getEncoded(prk);
        pukb = RSAKeyUtil.getEncoded(puk);
        String userPrivateKeyStr = Base64.encodeBytes(prkb);
        String userPublicKeyStr = Base64.encodeBytes(pukb);

        HashMap<String,String> map = new HashMap<String,String>();
        map.put("von_key", "vonkey-1");
        //        map.put("commands", "addKey,removeKey");
        
        ArrayList<String> secretAttr = new ArrayList<String>();
        secretAttr.add("von_key");
        //        secretAttr.add("public_key");
        
        AccessToken token = new AccessToken(map, secretAttr, "RSA-SHA1", "authority-id-1", userPublicKeyStr, new Date());
        token.updateTokenSignature(authorityPrivateKeyStr);

        map = new HashMap<String,String>();
        map.put("command", "addKey");
        
        AccessKey accessKey = new AccessKey("nonce-1", map, token);
        accessKey.updateSignature(userPrivateKeyStr);
        accessKey = AccessKey.parse(accessKey.toString());
        //System.out.println("accessKey=" + accessKey);
        
        HashMap<String,String> smap = new HashMap<String,String>();
        smap.put("von_key", "vonkey-1");
        //smap.put("public_key", userPublicKeyStr);
        
        if (accessKey.isValid(authorityPublicKeyStr,smap)) {
            //System.out.println("OK");
        }
        else {
            //System.out.println("NG");
        }
        // modified!!
        //accessKey.attributes.remove("command");
        //        if (!accessKey.isValid(authorityPublicKeyStr)) {
        //            System.out.println("OK");
        //        }
        //        else {
        //            System.out.println("NG");
        //        }
        
        map = new HashMap<String,String>();
        map.put("von_key", "vonkey-2");
        token = new AccessToken("authority-1", map);
        accessKey = new AccessKey(new AccessToken("authority-1", map));
        //System.out.println("accessKey=" + accessKey);
        accessKey = AccessKey.parse(accessKey.toString());
        if (accessKey.isValid(null)) {
            //            System.out.println("OK");
        }
        else {
            //            System.out.println("NG");
        }
    }
    
}
