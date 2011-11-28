package org.piax.ov.jmes.von;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

public class VONEntry {
    public String vonId;
    public String parentVONId;
    public String type;
    public String vonKey;
    public String name;
    public Date expiresAt;
    
    // only for managed von;
    public VON von;
    
    public VONEntry(String name, VON von) {
        this.name = name;
        vonId = von.vonId();
        vonKey = von.secretKey;
        parentVONId = von.parentVONId();
        expiresAt = von.expiresAt();
        this.von = von;
    }
    
    // Initialization without name
    public VONEntry(VON von) {
        this(von.vonId(), von);
    }
   
    public JSONObject toJSONObject() {
        JSONObject entry = new JSONObject();
        try {
            if (von instanceof AdHocPlainVON) {
                entry.put("type", "plain");
            }
            else if (von instanceof AdHocVON) {
                entry.put("type", "ad_hoc");
            }
            else if (von instanceof ManagedVON) {
                entry.put("type", "managed");
                entry.put("token", von.token().toString());
            }
            else if (von instanceof SessionVON) {
                entry.put("type", "session");
            }
            if (expiresAt != null) {
                entry.put("expires_at", expiresAt.getTime());
            }
            else {
                entry.put("expires_at", -1);
            }
            entry.put("name", name == null? "" : name);
            entry.put("von_id", vonId == null? "" : vonId);
            entry.put("parent_von_id", parentVONId == null? "" : parentVONId);
            entry.put("von_key", vonKey == null? "" : vonKey);
            
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return entry;
    }
}