package org.piax.ov.jmes.von;

import org.piax.ov.jmes.authz.AccessToken;

/**
 * {@.en ManagedVON is a class for managed type VON.}
 * {@.ja ManagedVON はリポジトリ管理型の VON を形成するためのクラスです．}
 * {@.ja authorityKey には，ソフトウェア埋め込みの公開鍵を用います．}
 */
public class ManagedVON extends VON {
    /**
     * {@.en The constructor of ManagedVON.}
     * {@.ja ManagedVON を生成するコンストラクタ．}
     * @param authorityKey {@.ja 承認機関の鍵}{@.en The key for the authority.}
     * @param token {@.ja 承認機関から得たトークン．}{@.en The Access token.}
     * @param vonKey {@.ja VON キー}{@.en The VON key.}
     */
    public ManagedVON(String authorityKey, AccessToken token, String vonKey) {
        this.authorityKey = authorityKey;
        this.token = token;
        
        this.vonId = token.getAttribute("von_id");
        this.secretKey = vonKey;
        
        //AccessKey.main(null);
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