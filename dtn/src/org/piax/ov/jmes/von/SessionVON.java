package org.piax.ov.jmes.von;

import java.util.ArrayList;
import java.util.HashMap;

import org.piax.ov.jmes.MessageData;
import org.piax.ov.jmes.authz.AccessKey;
import org.piax.ov.jmes.authz.SecureSession;
import org.piax.trans.common.PeerId;

public class SessionVON extends VON {
    SecureSession ss;
    
    public SessionVON(SecureSession ss) {
        this.ss = ss;
        this.secretKey = ss.key;
    }
    
    public String vonId() {
        //return extractVONId(ss.id);
        return ss.id;
    }

//    @Override
//    public void updateMessage(Message message) {
//        HashMap<String,String> attrs = new HashMap<String,String>();
//        attrs.put("von_session_id", ss.id);
//        message.secure_message = new AccessKey(attrs);
//    }
    
    public String newVONSessionId(String vonId) {
        return vonId + "/" + PeerId.newId(5).toString();
    }
    
    static public String extractVONId(String vonSessionId) {
        String[] pair = vonSessionId.split("/");
        return pair[0];
    }
    
    @Override
    public AccessKey getAccessKey(MessageData message) {
        HashMap<String,String> attrs = new HashMap<String,String>();
        attrs.put("von_session_id", ss.id);
        return new AccessKey(attrs, null);
    }
    
    
    @Override
    public boolean needEncryption() {
        return true;
    }

    @Override
    public int acceptMessage(MessageData message, AccessKey key) {
        String mvon  = key.getAttribute("von_session_id");
        if (ss.id != null && ss.id.equals(mvon)) {
            return VON_VALIDITY_OK;
        }
        return VON_VALIDITY_INVALID;
    }
}
