package org.piax.ov.jmes.von;

import org.piax.ov.jmes.authz.AccessKey;
import org.piax.ov.jmes.ols.OLSObject;

public class VONObject {
    public boolean isVON;
    public String id;
    public VONEntry entry;
//    PeerInfo pinfo;
    public OLSObject object;
    public int validity;
    public AccessKey signature;
}
