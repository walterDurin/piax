package org.piax.ov.jmes;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.piax.ov.jmes.von.VON;
import org.piax.ov.jmes.von.VONObject;
import org.piax.trans.Target;
import org.piax.trans.common.PeerId;
import org.piax.trans.target.RecipientId;

public class Message implements Serializable {
    private static final long serialVersionUID = 5343188220973423656L;
    // message data itself
    public MessageData data;
    // security information.
    public String von_id;
    public boolean allDecrypted;
    public boolean allValid;
    public int validity;
    public String senderPublicKey;
    public Date senderPublicKeyExpiresAt;
    public List<VONObject> elements; 
    
    public Message() {
        data = null;
        von_id = null;
        allDecrypted = false;
        allValid = false; 
        validity = VON.VON_VALIDITY_INVALID; 
        senderPublicKey =null; 
        senderPublicKeyExpiresAt = null; 
        elements = null; 
    }
    
    public Message(MessageSecurityManager smgr, MessageData data, String ovId) {
        Message mi = ((MessageSecurityManager)smgr).makeMessageInfo(data, ovId);
        this.data = mi.data;
        von_id = mi.von_id;
        allDecrypted = mi.allDecrypted;
        allValid = mi.allValid;
        validity = mi.validity;
        senderPublicKey = mi.senderPublicKey;
        senderPublicKeyExpiresAt = mi.senderPublicKeyExpiresAt;
        elements = mi.elements;
    }
    
    public boolean isValid() {
        return allValid && allDecrypted;
    }
    
    public Target getTarget() {
        if (data.recipient_id != null) {
            return new RecipientId(new PeerId(data.recipient_id));
        }
        else {
            return RecipientId.broadcastId();
        }
    }
}
