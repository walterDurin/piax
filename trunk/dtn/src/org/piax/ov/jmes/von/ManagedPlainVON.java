package org.piax.ov.jmes.von;

import java.util.HashMap;

import org.piax.ov.jmes.MessageData;
import org.piax.ov.jmes.authz.AccessKey;
import org.piax.ov.jmes.authz.AccessToken;

public class ManagedPlainVON extends VON {
    
    /**
     * {@.en The constructor of ManagedVON.}
     * {@.ja ManagedVON を生成するコンストラクタ．}
     * @param authorityKey {@.ja 承認機関の鍵}{@.en The key for the authority.}
     * @param token {@.ja 承認機関から得たトークン．}{@.en The Access token.}
     */
    public ManagedPlainVON(String authorityKey, AccessToken token) {
        this.authorityKey = authorityKey;
        this.token = token;
        this.vonId = token.getAttribute("von_id");
        this.secretKey = "";
    }

    @Override
    public boolean needEncryption() {
        return false;
    }
}
