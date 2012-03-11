package org.piax.ov.jmes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.piax.gnt.SecurityManager;
import org.piax.gnt.handover.Peer;
import org.piax.gnt.handover.PeerManager;
import org.piax.ov.jmes.authz.AccessKey;
import org.piax.ov.jmes.authz.AccessToken;
import org.piax.ov.jmes.authz.SecureSession;
import org.piax.ov.jmes.ols.OLS;
import org.piax.ov.jmes.ols.OLSAlgAndKey;
import org.piax.ov.jmes.ols.OLSKeyManager;
import org.piax.ov.jmes.ols.OLSObject;
import org.piax.ov.jmes.von.AdHocPlainVON;
import org.piax.ov.jmes.von.AdHocVON;
import org.piax.ov.jmes.von.ManagedVON;
import org.piax.ov.jmes.von.SessionVON;
import org.piax.ov.jmes.von.VON;
import org.piax.ov.jmes.von.VONEntry;
import org.piax.ov.jmes.von.VONException;
import org.piax.ov.jmes.von.VONObject;

import org.piax.trans.common.PeerId;

public class MessageSecurityManager implements OLSKeyManager, SecurityManager {
    String peerId;
    String publicKey;
    String privateKey;
    String authorityKey;
    /// XXX for test.
    ArrayList<VONEntry> vonEntries;
//    ArrayList<PeerStat> peerInfos;
    Map<String,SecureSession> secureSessions;
    PeerManager psm;

    public String peerId() {
        return peerId;
    }
    
    
    public void setPeerManager(PeerManager psm) {
        this.psm = psm;
    }
    
    public MessageSecurityManager(String peerId, String authorityKey, String jsonStr) {
        this.peerId = peerId;
        this.authorityKey = authorityKey;
        //this.psm = psm;
        JSONArray vons = null;
        vonEntries = new ArrayList<VONEntry>();
//        peerInfos = new ArrayList<PeerStat>();
        secureSessions = new HashMap<String,SecureSession>();
        if (jsonStr != null && jsonStr.length() > 0) {
            try {
                vons = new JSONArray(jsonStr);
            } catch (JSONException e) {
            }
        }
        if (vons != null) {
            for (int i = 0; i < vons.length(); i++) {
                JSONObject obj = null;
                try {
                    obj = (JSONObject) vons.get(i);
                    String vkey = obj.getString("von_key");
                    String vtype = obj.getString("type");
                    VON v = null;
                    if (vtype.equals("ad_hoc")) {
                        v = new AdHocVON(authorityKey, vkey, peerId);
                    }
                    else if (vtype.equals("plain")) {
                        v = new AdHocPlainVON(vkey);
                    }
                    else if (vtype.equals("managed")) {
                        //String expires_at = obj.getString("expires_at");
                        AccessToken token = AccessToken.parse(obj.getString("token"));
                        v = new ManagedVON(authorityKey, token, vkey);
                    }
                    if (v != null) {
                        // XXX suspicious.
                        v.setupKeyPair(publicKey, privateKey);
                        v.updateAccessToken();
                        VONEntry entry = new VONEntry(obj.getString("name"), v);
                        // not in the VON itself;
                        entry.type = vtype;
                        //if (obj.getLong("expires_at") > 0) {
                        //    entry.expiresAt = new Date(obj.getLong("expires_at"));
                        //}
                        entry.name = obj.getString("name"); // name is not in the VON;
                        
                        vonEntries.add(entry);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                
            } 
        }
    }
    
    public void setVONEntries(ArrayList<VONEntry> entries) {
        vonEntries = entries;
    }
    
    public SecureSession getVONSession(String vonId) {
        SecureSession ret = secureSessions.get(vonId);
        if (ret == null) {
            VONEntry entry = getVONEntryById(vonId);
            ret = new SecureSession();
            if (entry.expiresAt != null) {
                ret.expires = entry.expiresAt;
            }
            ret.key = PeerId.newId(5).toString();
            ret.id = vonId + "/" + PeerId.newId(5).toString();
            secureSessions.put(vonId, ret);
        }
        return ret;
    }
    
    public boolean hasVONSession(Peer pi, String vonId) {
        return (pi.getSecureSession(vonId) != null);
    }
    
    /* 
     * 隣接ピアが von_session に属していることを設定
     */
    public SecureSession getVONSessionCreate(String peerId, String vonId) {
        Peer pi = psm.getPeer(new PeerId(peerId));
        if (pi != null) {
            SecureSession ret = pi.getSecureSession(vonId);
            // XXX check (ret.expires)
            if (ret == null) {
                ret = getVONSession(vonId);
                pi.putSecureSession(vonId, ret);
            }
            return ret;
        }
        return null;
    }
    
    public void setVONSession(String peerId, String vonId, SecureSession ss) {
        secureSessions.put(vonId, ss);
        Peer pi = psm.getPeer(new PeerId(peerId));
        if (pi != null) {
            pi.putSecureSession(vonId, ss);
        }
    }

    public void clearVONSession(String peerId, String vonId) {
        Peer pi = psm.getPeer(new PeerId(peerId));
        if (pi != null) {
            pi.putSecureSession(vonId, null);
        }
    }
    
////    public MessageInfo examineMessage(Message mes) {
//      //  MessageInfo ret = new MessageInfo();
//        ret.decrypted = decrypt(mes);
//        ret.allDecrypted = !(ret.decrypted.isEncrypted());
//
//        if (mes.secure_message == null) {
//            ret.allValid = true;
//            ret.validities = null;
//        }
//        else {
//            VONDependency vd = dependedVONEntries(getVONId(mes));
//            if (vd == null) {
//                ret.allValid = false;
//                ret.validities = null;
//                return ret;
//            }
//            ArrayList<VONValidity> validities = new ArrayList<VONValidity>();
//            ret.allValid = true;
//            if (vd.ves == null) {
//                ret.allValid = false;
//                ret.validities = null;
//            }
//            for (VONEntry ve : vd.ves) {
//                VONValidity v = new VONValidity();
//                v.validity = ve.von.acceptMessage(mes);
//                if (v.validity != VON.VON_VALIDITY_OK) {
//                    ret.allValid = false;
//                }
//                v.vonId = ve.vonId;
//                validities.add(v);
//            }
//            if (ret.allValid && ret.allDecrypted){ 
//                VONEntry top = vd.ves.get(0);
//                if (top != null) {
//                    ret.vonId = top.vonId;
//                }
//            }
//            ret.validities = validities;
//        }
//        return ret;
//    }
    
    private class VONDependency {
        public ArrayList<VONEntry> ves;
        public boolean foundAll;
    }
    
    private VONDependency dependedVONEntries(String vonId) {
        VONDependency result = new VONDependency();
        ArrayList<VONEntry> ves = new ArrayList<VONEntry>();
        VONEntry ve = getVONEntryById(vonId);
        while (ve != null) {
            ves.add(ve);
            if (ve.parentVONId != null) {
                ve = getVONEntryById(ve.parentVONId);
                // failed to find parent entry...
                if (ve == null) {
                    result.ves = ves;
                    result.foundAll = false;
                }
            }
            else {
                ve = null;
                result.ves = ves;
                result.foundAll = true;
            }
        }
        return result;
    }

    // This method is for draft.
    public Message makeMessageInfo(MessageData mes, String vonId) {
        OLS ols = new OLS(this);
        ArrayList<VONObject> elements = new ArrayList<VONObject>();
        Message ret = new Message();
        VONDependency vd = null;
        if (vonId != null) {
            VONEntry entry = getVONEntryById(vonId);
            if (entry == null) {
                throw new VONException("Not all VONs are registerd.");
            }
            vd = dependedVONEntries(entry.vonId);
            // cannot encapsulate all.
            if (!vd.foundAll) {
                throw new VONException("Not enough VONs are registerd.");
            }
        }
        OLSObject cur = null;
        ret.allValid = false;
        ret.validity = VON.VON_VALIDITY_INVALID;
        if (mes.recipient_id != null) {
            cur = new OLSObject(mes.text);
            cur = ols.encapsulate(mes.recipient_id, cur);
            VONObject vo = new VONObject();
            vo.object = cur;
            vo.entry = null;
            vo.id = mes.recipient_id;
            vo.isVON = false;
            vo.signature = null;
            vo.validity = VON.VON_VALIDITY_OK;
            elements.add(vo);
        }
        if (vd != null) {
            for (VONEntry ve : vd.ves) {
                if (cur == null) {
                    cur = new OLSObject(mes.text);
                }
                cur = ols.encapsulate(ve.vonId, cur);
                VONObject vo = new VONObject();
                vo.object = cur;
                vo.entry = ve;
                vo.id = ve.vonId;
                vo.isVON = true;
                HashMap<String,String> map = new HashMap<String,String>();
                map.put("von_id", vo.id);
                // vo.signature = new AccessKey(map, null);
                MessageData copy = mes.copy();
                copy.text = vo.object.toString();
                vo.signature = vo.entry.von.getAccessKey(copy);
                vo.validity = ve.von.acceptMessage(copy, vo.signature);
                ret.allValid = (vo.validity == VON.VON_VALIDITY_OK);
                elements.add(vo);
            }
        }
        ret.data = mes;
        ret.allDecrypted = true;
        //ret.allValid = true;
        //ret.validity = VON.VON_VALIDITY_OK;
        ret.von_id = vonId;
        Collections.reverse(elements);
        ret.elements = elements;
        return ret;
    }
    
    public String getText(MessageData mes) {
        Message mi = decapsulate(mes);
        return mi.data.text;
    }

    // obtain a message object.
    public MessageData convert(Message m) {
        MessageData ret = m.data.copy();
        if (m.elements.size() != 0) {
            ret.text = m.elements.get(0).object.toString();
            ret.content_type = "text/ols";
        }
        JSONArray arr = new JSONArray();
        for (VONObject vo : m.elements) {
            if (vo.signature != null) {
                arr.put(vo.signature.makeJSONObject());
            }
        }
        if (arr.length() > 0) {
            ret.secure_message = arr.toString();
        }
        return ret;
    }
    
    public MessageData encapsulate(Message minfo) {
        MessageData ret = minfo.data;
        if (minfo.elements.size() != 0) {
            ret.text = minfo.elements.get(0).object.toString();
            ret.content_type = "text/ols";
        }
        JSONArray arr = new JSONArray();
        for (VONObject vo : minfo.elements) {
            AccessKey ak = null;
            if (vo.signature != null && vo.entry == null) {
                ak = vo.signature;
            }
            else {
                if (vo.entry != null) {
                    MessageData copy = minfo.data.copy();
                    copy.text = vo.object.toString();
                    ak = vo.entry.von.getAccessKey(copy);
                }
            }
            if (ak != null) {
                arr.put(ak.makeJSONObject());
            }
        }
        return ret;
    }
    
    public MessageData encapsulate(Peer target, Message minfo) {
        MessageData ret = minfo.data;
        if (minfo.elements.size() != 0) {
            ret.text = minfo.elements.get(0).object.toString();
            ret.content_type = "text/ols";
        }
        JSONArray arr = new JSONArray();
        OLS ols = new OLS(this);
        SecureSession ss = null;
        for (VONObject vo : minfo.elements) {
            
            if (vo.entry != null) {
                ss = target.getSecureSession(vo.entry.vonId);
            }
        }
        if (ss != null) {
            arr = new JSONArray();
            VONEntry sve = getVONEntryById(ss.id);
            AccessKey ak = sve.von.getAccessKey(ret);
            ret.text = ols.encapsulate(sve.vonId, minfo.elements.get(0).object).toString();
            arr.put(ak.makeJSONObject());
        }
        else {
            for (VONObject vo : minfo.elements) {
                AccessKey ak = null;
                if (vo.signature != null && vo.entry == null) {
                    // von signature that this node does not have.
                    ak = vo.signature;
                }
                else {
                    if (vo.entry != null) {
                        MessageData copy = minfo.data.copy();
                        copy.text = vo.object.toString();
                        ak = vo.entry.von.getAccessKey(copy);
                    }
                }
                if (ak != null) {
                    arr.put(ak.makeJSONObject());
                }
            }
        }
        if (arr.length() > 0) {
            ret.secure_message = arr.toString();
        }
        return ret;
    }
    
    public Message decapsulate(MessageData mes) {
        return decapsulate(mes, null);
    }
    
    public Message decapsulate(MessageData mes, Peer sender) {
        // ex.
        // vd: von-4,von-3,von-2
        // mes: von-4,von-3,von-2,von-1
        
        try {
        Message minfo = new Message();
        JSONArray arr = new JSONArray(mes.secure_message);
        OLSObject cur = null;
        OLS ols = new OLS(this);
        if (mes.content_type.equals("text/ols")) {
            cur = OLSObject.parse(mes.text);
        }
        ArrayList<AccessKey> keys = new ArrayList<AccessKey>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            AccessKey key = AccessKey.fromJSONObject(obj);
            key.removeAttribute("_von_session_verified");
            keys.add(key);
        }
        
        minfo.allValid = true;
        minfo.validity = VON.VON_VALIDITY_OK;
        ArrayList<VONObject> elements = new ArrayList<VONObject>();
        
        if (keys.size() > 0) {
            // check if it has VON Session or not.
            AccessKey sskey = keys.get(0);
            String sessionId = sskey.getAttribute("von_session_id");
            if (sessionId != null && sender != null) {
                
                ArrayList<AccessKey> newKeys = new ArrayList<AccessKey>();
                String vonId = SessionVON.extractVONId(sessionId);
                if (this.hasVONSession(sender, vonId)) {
                    // to obtain securet key for session key, do following.
                    //SecureSession forwarder.getSecureSession(vonId);
                    // decode with von_session_id;
                    if (cur.id.equals(sessionId)) { // is this OK?
                        cur = ols.decapsulate(cur);
                    }
                    ArrayList<String> parents = new ArrayList<String>();
                    String pvid = vonId;
                    parents.add(pvid); // add itself;
                    while ((pvid = getParentId(pvid)) != null) {
                        parents.add(pvid);
                    }
                    Collections.reverse(parents);
                    for (String vid : parents) {
                        HashMap<String,String> map = new HashMap<String,String>();
                        map.put("von_id", vid);
                        // XXX the key should be removed. 
                        map.put("_von_session_verified", "true");
                        AccessKey skey = new AccessKey(map, null);
                        newKeys.add(skey);
                    }
                    // Add keys for vons that are at higher layer than the session.
                    for (int i = 1; i < keys.size(); i++) {
                        newKeys.add(keys.get(i));
                    }
                    keys = newKeys;
                    minfo.von_id = vonId;
                }
            }
        }
        MessageData copy = mes.copy();
        for (AccessKey key : keys) {
            String vid = key.getAttribute("von_id");                        
            VONObject vo = new VONObject();
            VONEntry ve = getVONEntryById(vid);
            if (mes.source_id != null && mes.source_id.equals(key.getAttribute("peer_id"))) {
                minfo.senderPublicKey = key.publicKey;
                minfo.senderPublicKeyExpiresAt = key.tokenExpires;
            }
            if (ve == null) {
                minfo.allDecrypted = false;
                minfo.allValid = false;
                vo.validity = VON.VON_VALIDITY_NOT_FOUND;
                vo.object = cur;
                vo.entry = null;
                vo.isVON = true;
                vo.signature = key;
            }   
            else {
                String verified = key.getAttribute("_von_session_verified");
                vo.object = cur;
                if (verified != null && verified.equals("true")) {
                    vo.validity = VON.VON_VALIDITY_OK;   
                }
                else {
                    vo.validity = ve.von.acceptMessage(copy, key);
                }
                if (ve.von.needEncryption() && cur.id.equals(vid)) {
                    cur = ols.decapsulate(cur);
                }
                if (cur != null) {
                    copy.text = cur.toString();
                }
                vo.entry = ve;
                vo.isVON = true;
                vo.id = vid;
                vo.signature = key;
                minfo.von_id = vo.id;
            }
            if (vo.validity != VON.VON_VALIDITY_OK) {
                minfo.allValid = false;
                minfo.validity = vo.validity;
            }
            elements.add(vo);
        }
        if (cur != null && !cur.type.equals("text/plain") && cur.id.equals(mes.recipient_id)) {
            // Recipient is specified.
            VONObject vo = new VONObject();
            vo.entry = null;
            vo.id = mes.recipient_id;
            vo.isVON = false;
            vo.validity = VON.VON_VALIDITY_OK;
            cur = ols.decapsulate(cur);
            elements.add(vo);
        }
        if (cur == null) {
            return null;
        }
        minfo.allDecrypted = !cur.type.equals("text/ols") && !cur.type.equals("encrypted");
        minfo.data = copy;
        minfo.data.text = cur.getText();
        minfo.data.content_type = cur.type;
        //Collections.reverse(elements);
        minfo.elements = elements;
        return minfo;
        }
        catch (JSONException e) {
        }
        return null;
    }
    
    /// followings are outdated.
//    private Message encapsulateMessage(Message mes, VONEntry entry) {
//        VONDependency vd = dependedVONEntries(entry.vonId);
//        if (!vd.foundAll) {
//            return null;
//        }
//        Message ret = mes.copy();
//        List<VONEntry> ves = vd.ves;
//        for (int i = 0; i < ves.size(); i++) {
//            VONEntry ve = ves.get(i);
//            if (ve.von.needEncryption()) {
//                if (!ret.isEncrypted()) {
//                    encryptMessage(ret, "von", "PBE", entry.von.encryptionKey());
//                }
//                else {
//                    ret = encapsulateMessageEach(ret, ve);
//                }
//                ve.von.updateMessage(ret);
//            }
//            else {
//                ve.von.updateMessage(ret);
//            }
//        }
//        return ret;
//    }
//
//    private Message encapsulateMessageEach(Message mes, VONEntry entry) {
//        Message ret = mes.copy();
//        mes.content_type = "message/json";
//        mes.text = ret.getJson();
//        // mes.von_id = vonId;
//        encryptMessage(mes, "von", "PBE", entry.von.encryptionKey());
//        return mes;
//    }
//
//    private void encryptMessage(Message mes, String encType, String alg, String cryptKey) {
//        if (cryptKey != null) {
//            CipherAlgorithm ca = CipherAlgorithm.getInstance(alg);
//            String crypted = ca.encrypt(mes.text, cryptKey);
//            if (crypted != null) {
//                mes.text = crypted;
//                mes.setEncryptedContentType(encType);        
//            }
//            System.out.println("CONTENT_TYPE=" + mes.content_type);
//        }
//    }
//    
//    public Message encrypt(PeerInfo target, Message mes, String von_id) throws VONException {
//        String encType;
//        String alg;
//        String cryptKey = null;
//        if (mes.recipient_id != null) {
//            encType = "recipient";
//            alg = "RSA";
//            PeerInfo pi = getPeerInfo(mes.recipient_id);
//            if (pi != null) {
//                cryptKey = pi.publicKey;
//                encryptMessage(mes, encType, alg, cryptKey);
//            }
//            else {
//                throw new VONException("Recipient not found");
//                //System.out.println("Public key for encryption was not found. Continue with no encryption.");
//            }
//        }
//        return encapsulateMessage(mes, getVONEntryById(von_id));
//    }
//
//    public Message decrypt(Message mes) {
//        String alg = null;
//        String decryptKey = null;
//        Message m = mes.copy();
//        
//        while (m.isEncrypted()) {
//            String oct = m.content_type;
//            String enc_type = m.extractEncryptedContentType();
//            if (enc_type == null) {
//                // no encryption;
//                return mes;
//            }
//            if (enc_type.equals("recipient")) {
//                alg = "RSA";
//                decryptKey = privateKey;
//            }
//            else if (enc_type.equals("von")){
//                alg = "PBE";
//                String vonId = getVONId(m);
//                for (VONEntry entry : vonEntries) {
//                    if (entry.vonId.equals(vonId) && entry.von.needEncryption()) {
//                        decryptKey = entry.vonKey;
//                        break;
//                    }
//                }
//            }
//            if (decryptKey != null && alg != null) {
//                CipherAlgorithm ca = CipherAlgorithm.getInstance(alg);
//                String decrypted = ca.decrypt(m.text, decryptKey);
//                if (decrypted != null) {
//                    if (m.content_type.equals("message/json")) {
//                        m = Message.fromJson(decrypted); 
//                    }
//                    else {
//                        m.text = decrypted;
//                    }
//                }
//                else {
//                    System.out.println("Decrypt failed?? Hello??");
//                    m.content_type = oct;
//                    return m;
//                }
//            }
//            else {
//                return m;
//            }
//        }
//        return m;
//    }

    public void setupKeyPair(String publicKey, String privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        if (vonEntries != null) {
            for (VONEntry ve : vonEntries) {
                VON v = ve.von;
                v.setupKeyPair(publicKey, privateKey);
                v.updateAccessToken();
            }
        }
        
    }

    public String getPrivateKey() {
        return this.privateKey;
    }
    
    public String getPublicKey() {
        return this.privateKey;
    }
    
    /**
     * {@.ja メッセージのセキュリティ情報としてVONを設定します．}{@.en Sets a VON as the security information of the message.}
     * @param mes {@.en the message object.}{@.ja メッセージオブジェクト}
     * @param von_id {@.en The VON Id.}{@.ja VON Id．}
     */
//    public void setVON(Message mes, String vonId) {
//        VONEntry ve = null;
//        for (VONEntry entry : vonEntries) {
//            if (vonId.equals(entry.vonId)) {
//                ve = entry;
//            }
//        }        
//        if (ve != null && ve.von != null) {
//            VON v = ve.von;
//            v.updateMessage(mes);
////            if (v.needEncryption()) {
////  encrypt(mes);                
////            }
//            // XXX should this be here?
//            mes.von_id = v.vonId();
//        }
//    }
    
//    private List<PeerStat> getPeerInfos() {
//        return peerInfos;
//    }
//    
//    private void sortPeerInfos() {
//        Collections.sort(peerInfos, new Comparator<PeerStat>() {
//            public int compare(PeerStat o1, PeerStat o2) {
//                Date date1 = o1.lastSeen;
//                Date date2 = o2.lastSeen;
//                return date2.compareTo(date1);
//            }
//        });
//    }
//    
//    private void addPeerInfo(PeerStat pi) {
//        peerInfos.add(pi);
//    }
//    
//    private PeerStat getPeerInfoByHost(String host) {
//        for (PeerStat pi: peerInfos) {
//            if (host.equals(pi.locator)) {
//                return pi;
//            }
//        }
//        return null;    
//    }
//    
//    private PeerStat getPeerInfo(String peerId) {
//        for (PeerStat pi: peerInfos) {
//            if (peerId.equals(pi.peerIdString)) {
//                return pi;
//            }
//        }
//        return null;
//    }
    
//    public void addVONEntry(VONEntry ve) {
//        vonEntries.add(ve);
//    }

    public VONEntry getLatestVONEntry() {
        VONEntry ret = null;
        for (VONEntry entry: vonEntries) {
            if (ret == null) {
                ret = entry;
            }
            else {
                if (entry.expiresAt.after(ret.expiresAt)) {
                    ret = entry;
                }
            }
        }
        return ret;
    }
    
    public void addVON(String name, VON von) {
        von.setupKeyPair(publicKey, privateKey);
        von.updateAccessToken();
        
        VONEntry ve = getVONEntryById(von.vonId());
        if (ve != null) {
            vonEntries.remove(ve);
        }
        vonEntries.add(new VONEntry(name, von));
    }

    public void addVON(VON von) {
        addVON(von.vonId(), von);
    }
    
    public List<VONEntry> getVONEntries() {
        return vonEntries;
    }

    public void clearVONEntries() {
        vonEntries.clear();
    }
    
    public VONEntry getVONEntryById(String vonId) {
        if (vonId == null) {
            return null;
        }
        for (VONEntry entry : vonEntries) {
            if (vonId.equals(entry.vonId)) {
                return entry;
            }
        }
        return null;
    }

    public String getVONKeyById(String vonId) {
        VONEntry entry = getVONEntryById(vonId);
        if (entry != null) {
            return entry.vonKey;
        }
        return null;
    }

    public String getVONNameById(String vonId) {
        VONEntry entry = getVONEntryById(vonId);
        if (entry != null) {
            return entry.name;
        }
        return null;
    }

    public String toString() {
        JSONArray array = new JSONArray();
        for (VONEntry entry : vonEntries) {
            JSONObject obj = entry.toJSONObject();
            array.put(obj);
        }
        return array.toString();
    }

    // Act as OLSKeyManager
    @Override
    public String getDecryptionKey(String alg, String id) {
        if (alg.equals("PBE")) {
            VONEntry ve = this.getVONEntryById(id);
            if (ve != null) {
                return ve.vonKey;
            }
        }
        else if (alg.equals("RSA")) {
            if (id.equals(peerId)) {
                return privateKey;
            }
        }
        return null;
    }

    @Override
    public OLSAlgAndKey getEncryptionAlgAndKey(String id) {
        OLSAlgAndKey aak = new OLSAlgAndKey();
        VONEntry ve = this.getVONEntryById(id);
        if (ve != null) {
            aak.alg = "PBE";
            aak.key = ve.vonKey;
        }
        else {
            Peer pi = psm.getPeer(new PeerId(id));
            if (pi != null) {
                aak.alg = "RSA";
                aak.key = pi.publicKey;
            }
            else {
                return null;
            }
        }
        return aak;
    }

    @Override
    public String getParentId(String id) {
        // skip no encryption VONs;
        VONEntry ve = null;
        while ((ve = getVONEntryById(id)) != null && ve.von != null && !ve.von.needEncryption()) {
            id = ve.parentVONId;
        }
        if (ve != null) {
            return ve.parentVONId;
        }
        return null;
    }

    @Override
    public byte[] wrap(Object src) {
        return ((Command)src).wrap(this);
    }

    @Override
    public Object unwrap(byte[] src) {
        return new Command(this, src);
    }
}