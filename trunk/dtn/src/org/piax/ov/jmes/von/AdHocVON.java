package org.piax.ov.jmes.von;

import java.util.ArrayList;
import java.util.HashMap;

import org.piax.ov.jmes.authz.AccessToken;

/**
 * {@.en AdHocVON is a class for Ad-hoc type VON.}
 * {@.ja AdHocVON はアドホック型の VON を形成するためのクラスです．}
 *
 * {@.ja authorityKey には，ソフトウェア埋め込みの共通鍵を用います．}
 */
public class AdHocVON extends VON {
    /**
     * {@.en The constructor of AdHocVON.}
     * {@.ja AdHocVON を生成するコンストラクタ．}
     * @param authorityKey {@.ja 承認機関の鍵}{@.en The key for the authority.}
     * @param vonKey {@.ja VON キー}{@.en The VON key.}
     * @param peerId {@.ja ピアIDの文字列}{@.en The peer id.}
     */
    public AdHocVON(String authorityKey, String vonKey, String peerId) {
        this.authorityId = null;
        this.authorityKey = authorityKey;
        this.vonId = vonKey; // vonKey = vonId;
        this.secretKey = vonKey;
        this.peerId = peerId;
        //AccessKey.main(null);
    }

    /**
     * {@.en Signs the VON key, public key etc by shared secret key.}
     * {@.ja 共通鍵により VON キー，公開鍵などの署名をします．}
     * <p>
     * {@.en This method must be called after <code>setupKeyPair()</code> is called.}
     * {@.ja <code>setupKeyPair()</code> のあとで呼ばれる必要があります．}
     */
    @Override
    public void updateAccessToken() {
        HashMap<String,String> map = new HashMap<String,String>();
        map.put("peer_id", peerId);
        map.put("von_key", secretKey);
        ArrayList<String> secret = new ArrayList<String>();
        secret.add("von_key");
        token = new AccessToken(map, secret, "HMAC-SHA1", authorityId, userPublicKey, null);
        token.updateTokenSignature(authorityKey);
    }
}
