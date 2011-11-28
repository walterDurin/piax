package org.piax.ov.jmes.von;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.piax.ov.jmes.MessageData;
import org.piax.ov.jmes.authz.AccessKey;
import org.piax.trans.util.Base64;

/**
 * {@.en AdHocPlainVON is an Ad-hoc, non-encrypted VON for closed network.}
 * {@.ja AdHocPlainVON はクローズドな環境むけの暗号化なしのアドホック型 VON です．}
 *
 * {@.ja 暗号化や署名は用いません．タグ付けによるメッセージの分類のみ行なわれます．}
 */
public class AdHocPlainVON extends VON {
    public AdHocPlainVON(String vonKey) {
        this.vonId = vonKey;
        this.secretKey = vonKey;
    }

    @Override
    public AccessKey getAccessKey(MessageData message) {
        HashMap<String,String> attrs = new HashMap<String,String>();
        attrs.put("von_id", vonId);
        return new AccessKey(attrs, null);
    }

    @Override
    public boolean needEncryption() {
        return false;
    }

    /**
     * {@.en Checks the tag of the message.}
     * {@.ja メッセージにつけられたタグが一致するか調べます．}
     * @param message {@.en the message to verify.}{@.ja 検証する対象のメッセージ．}
     * @return {@.en If the tag is matched, return <code>true</code>}{@.ja タグがマッチしたら<code>true</code>}
     */
    @Override
    public int acceptMessage(MessageData message, AccessKey key) {
        String mvon = key.getAttribute("von_id");
        if (secretKey != null && secretKey.equals(mvon)) {
            return VON_VALIDITY_OK;
        }
        return VON_VALIDITY_INVALID;
    }
    
}
