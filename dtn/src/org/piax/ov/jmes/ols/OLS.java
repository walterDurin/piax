package org.piax.ov.jmes.ols;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONException;
import org.piax.trans.util.Base64;

// OLS - JSON Objects with Layered Security;

/* {type: "encrypted",
 * alg: "PBE",
 * id: "von-3-id",
 * data: "ABCDEF..."}
 * {type: "encrypted",
 * alg: "RSA",
 * id: "recipient-1-id",
 * data: "ABCDEF..."}
 * {type: "text/plain",
 * data: "dirty little secret"}
*/

public class OLS {
    OLSKeyManager manager;
    
    public OLS(OLSKeyManager manager) {
        this.manager = manager;
    }
    
    public OLSObject encapsulate (String id, OLSObject obj) {
        OLSAlgAndKey aak = manager.getEncryptionAlgAndKey(id);
        if (aak != null && aak.alg != null && aak.key != null) {
            CipherAlgorithm ca = CipherAlgorithm.getInstance(aak.alg);
            return new OLSObject(id, aak.alg, ca.encryptBytes(obj.toBytes(), aak.key));
        }
        else {
            // Return null on failure.
            return null;
        }
    }
    
    public String encapsulate(String id, String text) {
        ArrayList<String> ids = new ArrayList<String>();
        String i = id;
        while (i != null) {
            ids.add(i);
            i = manager.getParentId(id);
        }
        OLSObject obj = new OLSObject(text);
        for (String sid : ids) {
            if (obj != null) {
                obj = encapsulate(sid, obj);
            }
        }
        return obj.toString();
    }

    public OLSObject decapsulate(OLSObject obj) throws JSONException {
        CipherAlgorithm ca = CipherAlgorithm.getInstance(obj.alg);
        String key = manager.getDecryptionKey(obj.alg, obj.id);
        if (obj.data != null && key != null) {
            byte[] decrypted = ca.decryptBytes(obj.data, key);
            if (decrypted == null) {
                return obj;
            }
            return OLSObject.parseBytes(decrypted);
        }
        return obj;
    }

    public String decapsulate(String text) throws JSONException {
        OLSObject obj = OLSObject.parse(text);
        while (obj.type != null && obj.type.equals("encrypted")) {
            obj = decapsulate(obj);
        }
        if (obj.type != null && obj.type.equals("text/plain")) {
            return obj.getText();
        }
        return null;
    }
}