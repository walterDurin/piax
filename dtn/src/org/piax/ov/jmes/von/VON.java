package org.piax.ov.jmes.von;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.piax.ov.jmes.MessageData;
import org.piax.ov.jmes.authz.AccessKey;
import org.piax.ov.jmes.authz.AccessToken;
import org.piax.ov.jmes.authz.RSAKeyUtil;
import org.piax.ov.jmes.ols.CipherAlgorithm;

import org.piax.trans.common.Id;
import org.piax.trans.util.Base64;

/**
 * {@.en VON (Virtual Overlay Network) is a class to create a virtual network group called VON on DTN.}
 * {@.ja VON (Virtual Overlay Network;仮想化オーバレイネットワーク)クラスは，DTN 上に仮想的に VON と呼ばれるグループを作ります．}
 * <p>
 * {@.en  class(subclass of DTN) runs by itself and send messages by store and forward manner. By store and forward message handling, messages can be distributed to the peers in the unstable network although it takes time.}
 * {@.en There is a shared key called 'VON key' for each VON. By calling }{@.ja VON ごとに共通の「VON キー」があります．DTN に}{@link org.piax.ov.ovs.dtn.DTN#addVON(VON von) addVON}{@.ja により VON を登録します．}{@.en , VON can be registered to the DTN.}
 * <p>
 * {@.en Only messages with VON that is registered to the DTN peer can be received. Moreover, peer ID spoofing, falsification of the mesasge content can be avoided(depends on the implementation).}
 * {@.ja メッセージに VON が設定されているとき，その VON が登録されている DTN ピアのみメッセージを受信するようにできます．また，メッセージ送信者のなりすまし，メッセージの改竄がないことが保証できます(実装による)．}
 * {@.ja 本文が VON キーで暗号化されます(実装による)．}
 * {@.ja また，上記署名，ピアID等に各ピアの秘密鍵により署名が付けられます(実装による)．}
 * {@.en In the current implementation, signature is added to the VON key of the message(depends on the implementation).}
 * {@.en In addition, a signature by secret private key to the above signature, peer id, etc. is added to the message}
 */
abstract public class VON {
    String authorityId;
    String authorityKey;
    String type;

    protected String vonId;
    // if no parent, null;
    String parentVONId;
    protected String secretKey;
    String userPublicKey;
    String userPrivateKey;
    String peerId;
    //    String name;
    AccessToken token;

//    /**
//     * {@.en The constructor of VON.}
//     * {@.ja VON を生成するコンストラクタ．}
//     * @param authorityId {@.ja 承認機関のID．null でも良い．}{@.en The ID of the authority(issuer). It can be null.}
//     * @param authorityKey {@.ja 承認機関の鍵}{@.en The key for the authority.}
//     * @param vonName {@.ja VON の表示名}{@.en The screen name of the VON.}
//     * @param vonKey {@.ja VON キー}{@.en The VON key.}
//     * @param peerId {@.ja ピアIDの文字列}{@.en The peer id.}
//     */
//    public VON(String authorityId, String authorityKey, String vonName, String vonKey, String peerId) {
//  //      this.authorityId = authorityId;
// //       this.name = vonName;
//  //      this.authorityKey = authorityKey;
//  //      this.vonKey = vonKey;
//  //      this.peerId = peerId;
//        //AccessKey.main(null);
//    }

    /**
     * {@.en Setups key pair of the peer.}
     * {@.ja ピアの鍵ペア(公開鍵，秘密鍵)を設定します．}
     * @param publicKey {@.ja 公開鍵．}{@.en The public key.}
     * @param privateKey {@.ja 秘密鍵．}{@.en The secret key.}
     */
    public void setupKeyPair(String publicKey, String privateKey) {
        this.userPublicKey = publicKey;
        this.userPrivateKey = privateKey;
        //System.out.println("**** SETUP KEYPAIR=" + userPublicKey + "," + userPrivateKey);
    }

    /**
     * {@.en Generates key pair List of the peer.}
     * {@.ja ピアの鍵ペア(公開鍵，秘密鍵)のリストを新規生成します．}
     */    
    static public List<String> generateKeyPair() {
        KeyPair kp = RSAKeyUtil.genKeyPair();
        PrivateKey prk = kp.getPrivate();
        PublicKey puk = kp.getPublic();
        byte[] prkb = RSAKeyUtil.getEncoded(prk);
        byte[] pukb = RSAKeyUtil.getEncoded(puk);
        List<String> ret = new ArrayList<String>();
        ret.add(Base64.encodeBytes(pukb));
        ret.add(Base64.encodeBytes(prkb));
        return ret;
    }
    
    /**
     * {@.en Update an Access token. }
     * {@.ja アクセストークンをアップデートします．}
     * <p>
     * {@.en This method must be called after <code>setupKeyPair()</code> is called.}
     * {@.ja <code>setupKeyPair()</code> のあとで呼ばれる必要があります．}
     */
    public void updateAccessToken() {}

    /**
     * {@.en Updates signature of the message by VON.}
     * {@.ja メッセージに VON で署名を付与します．}
     * @param message {@.en the message to sign.}{@.ja 署名をつけるメッセージ．}
     */
//    public void updateMessage(Message message) {
//        String nonce = Id.newId(3).toString();
//        HashMap<String,String> map = new HashMap<String,String>();
//        // map.put("command", "send");
//        map.put("message_id", message.id);
//        map.put("source_id", message.source_id);
//        map.put("von_id", vonId);
//        //        map.put("text", message.text); // no meaning when encrypted
//        AccessKey accessKey = new AccessKey(nonce, map, token);
//        accessKey.updateSignature(userPrivateKey);
//        message.secure_message = accessKey;
//    }

    /**
     * {@.en Generates the signature(AccessKey) for the message on VON.}
     * {@.ja メッセージに対する VON による署名(AccessKey)を生成します．}
     * @param message {@.en the message to sign.}{@.ja 署名をつけるメッセージ．}
     */
    public AccessKey getAccessKey(MessageData message) {
        String nonce = Id.newId(3).toString();
        HashMap<String,String> map = new HashMap<String,String>();
        // map.put("command", "send");
        map.put("message_id", message.id);
        //map.put("source_id", message.source_id);
        //map.put("von_id", vonId);
        map.put("text", message.text); // no meaning when encrypted?
        AccessKey accessKey = new AccessKey(nonce, map, token);
        if (userPrivateKey == null) {
            //System.out.println("**** NO PRIVATE KEY!");
        }
        accessKey.updateSignature(userPrivateKey);
        return accessKey;
    }
    
    public void setParentVONId(String parentVONId) {
        this.parentVONId = parentVONId;
    }
    
    public static final int VON_VALIDITY_OK = 0;
    public static final int VON_VALIDITY_SELF_TOKEN_EXPIRED = 1;
    public static final int VON_VALIDITY_MESSAGE_TOKEN_EXPIRED = 2;
    public static final int VON_VALIDITY_SIGNATURE_FAILURE = 3;
    public static final int VON_VALIDITY_NOT_FOUND = 4;
    public static final int VON_VALIDITY_INVALID = 5;


    /**
     * {@.en Checks the validity of the signature of a message.}
     * {@.ja メッセージの署名を検証します．}
     * @param message {@.en the message to verify.}{@.ja 署名を検証する対象のメッセージ．}
     * @return {@.en Validity check result.}{@.ja 検証結果．}
     */
    public int acceptMessage(MessageData message, AccessKey key) {
        long curTime = new Date().getTime();
        if (token != null) {
            if (token.tokenExpires != null) {
                if (token.tokenExpires.getTime() < curTime + 1000 * 3600) {
                    //System.out.println("At " + curTime + ", Token is EXPIRED!! " + token.tokenExpires.getTime() + "<" + (curTime + 1000 * 3600));
                    return VON_VALIDITY_SELF_TOKEN_EXPIRED;
                }
            }
            else {
                //System.out.println("TokenExpires is NULL!");
            }
        }
        else {
           // System.out.println("Token is NULL!");
        }   
        if (key != null) {
            if (key.tokenExpires != null) {
                if (key.tokenExpires.getTime() < curTime) {
                   // System.out.println("Token is EXPIRED!!");
                    return VON_VALIDITY_MESSAGE_TOKEN_EXPIRED;
                }
            }
            else {
                //System.out.println("Key's TokenExpires is NULL");
            }
        }
        else {
            //System.out.println("Key is NULL");
        }

        HashMap<String,String> attrs = new HashMap<String,String>();
        attrs.put("message_id", message.id);
        attrs.put("text", message.text);
        //        map.put("von_key", vonKey);
       //                Log.d("VON", "mes=" + message.getJson());
       //                Log.d("VON", "key=" + key);
       //                Log.d("VON", "vonKey=" + vonKey);
        
        HashMap<String,String> secAttrs = new HashMap<String,String>();
        secAttrs.put("von_key", secretKey);
        if (key.isValid(authorityKey, attrs, secAttrs)) {
            return VON_VALIDITY_OK;
        }
        else {
            return VON_VALIDITY_SIGNATURE_FAILURE;
        }
    }

    /**
     * {@.en Returns a VON Identifier.}
     * {@.ja VON 識別子を得ます．}
     * @return {@.en The VON Identifier}{@.ja VON 識別子}
     */
    public String vonId() {
        return vonId;
    }

    /**
     * {@.en Returns a parent VON Identifier.}
     * {@.ja 親 VON の識別子を得ます．}
     * @return {@.en The VON Identifier}{@.ja VON 識別子}
     */
    public String parentVONId() {
        return parentVONId;
    }
    
    
    /**
     * {@.en Returns a VON key.}
     * {@.ja VON キーを得ます．}
     * @return {@.en The VON key}{@.ja VON キー}
     */
    public String encryptionKey() {
        return secretKey;
    }
    
    /**
     * {@.en Returns a token}
     * {@.ja アクセストークンを返却します．} 
     * @return {@.en The access token}{@.ja アクセストークン} 
     */
    public AccessToken token() {
        return token;
    }
    
    /**
     * {@.en Returns a VON name.}
     * {@.ja VON の表示名を得ます．}
     * @return {@.en The VON name}{@.ja VON 表示名}
     */
    //public String vonName() {
    //return name;
    //}

    /**
     * {@.en Returns whether the VON requires encryption or not.}
     * {@.ja 暗号化が必要かどうかを返却します．}
     * @return {@.en true if it requires encryption}{@.ja 暗号化が必要なら true}
     */
    public boolean needEncryption() {
        return true;
    }

    /**
     * {@.en Returns a public key.}
     * {@.ja 公開鍵を得ます．}
     * @return {@.en public key string.} {@.ja 公開鍵}
     */
    public String publicKey() {
        return userPublicKey;
    }

    /**
     * {@.en Returns a private key.}
     * {@.ja 秘密鍵を得ます．}
     * @return {@.en private key string.} {@.ja 秘密鍵}
     */
    public String privateKey() {
        return userPrivateKey;
    }
    
    /**
     * *
     * @return 
     */
    
    public Date expiresAt() {
        if (token != null) {
            return token.tokenExpires;
        }
        return null;
    }
    
//     public static void main(String[] args) {
//         // authority key pair;
//         KeyPair kp = RSAKeyUtil.genKeyPair();
//         PrivateKey prk = kp.getPrivate();
//         PublicKey puk = kp.getPublic();
//         byte[] prkb = RSAKeyUtil.getEncoded(prk);
//         byte[] pukb = RSAKeyUtil.getEncoded(puk);
//         String privateKeyStr = Base64.encodeBytes(prkb);
//         String publicKeyStr = Base64.encodeBytes(pukb);

//         VON von = new VON(null, "secret", "name", "vonkey-1", "pid-string");
//         von.setupKeyPair(publicKeyStr, privateKeyStr);
//         von.updateAccessToken();
//         Message mes = new Message();
//         mes.created_at = new Date();
//         mes.expires_at = new Date();
//         mes.id = "abc";
//         mes.screen_name = "hello";
//         mes.text = "text";
//         von.updateMessage(mes);
//         if (von.acceptMessage(mes)) {
//             System.out.println("OK");
//         }
//         else {
//             System.out.println("NG");
//         }
//     }
}