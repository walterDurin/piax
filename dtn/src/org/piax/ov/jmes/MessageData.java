/*
 * jmes - JSON based message handling
 */

package org.piax.ov.jmes;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.piax.ov.jmes.authz.AccessKey;
import org.piax.trans.common.PeerId;
import org.piax.trans.util.Base64;

/**
 * {@.en This Message class treats message information required for DTN.}
 * {@.ja 本 Message クラスは，DTN で用いるメッセージ情報を扱います．}
 * <p>
 * {@.en It includes sender's peer id, recipient's peer id, message body text, relayed peer info, VON info, etc. DTN processes the message according to these information.}
 * {@.ja 送信元，送信先のピアID，本文情報，経由ピア情報，VON情報などを保持します．DTN はこれらの情報をもとにメッセージの転送処理を行ないます．}
 * {@.ja 最も簡単なコンストラクタの利用例は以下であり，本文テキストを指定するのみです．}
 * {@.en An example of the simplest constructor is following. Only the message body text is specified.}
 * <pre>
 * Message mes = new Message("Hello, world.");
 * </pre>
 * 
 */
public class MessageData implements Serializable {
    private static final long serialVersionUID = 7725479046899329612L;
    /**
     * {@.ja JSON, DB においてメッセージIDを指すキーです．}{@.en A key for message id in JSON, DB.}
     */
    public static final String KEY_MESSAGE_ID = "id";
    /**
     * {@.ja JSON, DB において内容の型を指すキーです．}{@.en A key for content type in JSON, DB.}
     */
    public static final String KEY_CONTENT_TYPE = "content_type";
    /**
     * {@.ja JSON, DB において内容のテキストを指すキーです．}{@.en A key for content text in JSON, DB.}
     */
    public static final String KEY_TEXT = "text";
    /**
     * {@.ja JSON, DB において生成日時を指すキーです．}{@.en A key for creation time in JSON, DB.}
     */
    public static final String KEY_CREATED_AT = "created_at";
    /**
     * {@.ja JSON, DB において有効期限を指すキーです．}{@.en A key for expiration time in JSON, DB.}
     */
    public static final String KEY_EXPIRES_AT = "expires_at";
    /**
     * {@.ja JSON, DB において送信者の表示名を指すキーです．}{@.en A key for screen name in JSON, DB.}
     */
    public static final String KEY_SCREEN_NAME = "screen_name";
    /**
     * {@.ja JSON, DB において送信元ピアIDを指すキーです．}{@.en A key for sender peer id in JSON, DB.}
     */
    public static final String KEY_SOURCE_ID = "source_id";
    /**
     * {@.ja JSON, DB において受信日時を指すキーです．}{@.en A key for received time in JSON, DB.}
     */
    public static final String KEY_RECEIVED_AT = "received_at";
    /**
     * {@.ja JSON, DB においてメッセージの状態を指すキーです．}{@.en A key for status in JSON, DB.}
     */
    public static final String KEY_STATUS = "status";
    /**
     * {@.ja JSON, DB においてメッセージの受信条件を指すキーです．}{@.en A key for recipient condition in JSON, DB.}
     */
    public static final String KEY_CONDITION = "condition";
    /**
     * {@.ja JSON, DB においてメッセージの受信ピアIDを指すキーです．}{@.en A key for recipient peer id in JSON, DB.}
     */
    public static final String KEY_RECIPIENT_ID = "recipient_id";
    /**
     * {@.ja JSON, DB においてメッセージ受信者の表示名を指すキーです．}{@.en A key for recipient screen name in JSON, DB.}
     */
    public static final String KEY_RECIPIENT_SCREEN_NAME = "recipient_screen_name";
    /**
     * {@.ja JSON, DB において引用元メッセージIDを指すキーです．}{@.en A key for source message id of the reply in JSON, DB.}
     */
    public static final String KEY_REPLY_TO = "in_reply_to";
    /**
     * {@.ja JSON, DB において引用元ピアIDを指すキーです．}{@.en A key for source peer id of the reply in JSON, DB.}
     */
    public static final String KEY_REPLY_TO_ID = "in_reply_to_id";
    /**
     * {@.ja JSON, DB において引用元メッセージの送信者の表示名を指すキーです．}{@.en A key for source message sender's screen name of the reply in JSON, DB.}
     */
    public static final String KEY_REPLY_TO_SCREEN_NAME = "in_reply_to_screen_name";
    /**
     * {@.ja JSON, DB においてメッセージの VON などのセキュリティ情報を指すキーです．}{@.en A key for security information of the message (VON etc) in JSON, DB.}
     */
    public static final String KEY_SECURE_MESSAGE = "secure_message";
    /**
     * {@.ja Message の VON ID です．}{@.en The VON Id of the message.}
     */
    public static final String KEY_VON_ID = "von_id";
    /**
     * {@.ja JSON, DB 中でメッセージ経由ピアのIDリストを指すキーです．}{@.en A key for relay peer id list of the message in JSON, DB.}
     */
    public static final String KEY_VIA = "via";
    /**
     * {@.ja JSON, DB 中で経由ピアの残り数を指すキーです．}{@.en A key for relay ttl(time to live) of the message in JSON, DB.}
     */
    public static final String KEY_TTL = "ttl";
    /**
     * {@.ja DB 中で未読かどうかを指すキーです．}{@.en A key for new  of the message in DB.}
     */
    public static final String KEY_NEW_FLAG = "new_flag";
    /**
     * {@.ja デフォルトの有効期間(ミリ秒)}{@.en The default expiration interval in milliseconds.}
     */
    public static final int DEFAULT_EXPIRE_INTERVAL = 24 * 3600 * 1000;
    /**
     * {@.ja デフォルトの経由ピア数の上限}{@.en The default relay peer TTL.}
     */
    public static final int DEFAULT_TTL = 7;

    /**
     * {@.en Constructs an empty message.}{@.ja 空のメッセージオブジェクトを生成します．}
     */
    public MessageData() {
        this.ttl = DEFAULT_TTL;;
    }

    /**
     * {@.en Constructs a message with text body.}{@.ja 指定された本文テキストを持つメッセージオブジェクトを生成します．}
     * @param text {@.en The message body text.}{@.ja メッセージ本文のテキストです．}
     */
    public MessageData(String text) {
        this.content_type = "text/plain";
        this.text = text;        
        this.ttl = DEFAULT_TTL;;
    }

    /**
     * {@.en Constructs a message with text body for a recpient.}{@.ja 指定された本文テキストを持つ宛先付きメッセージオブジェクトを生成します．}
     * @param recipientId {@.en The recipient peer id.}{@.ja 宛先のピアIDです．}
     * @param text {@.en The message body text.}{@.ja メッセージ本文のテキストです．}
     */
    public MessageData(PeerId recipientId, String text) {
        this(text);
        this.recipient_id = recipientId.toString();
    }
    
//     /**
//      * {@.en Constructs a message with sender information and text body.}{@.ja 送信者情報と本文テキストを指定したメッセージの生成を行ないます．}&nbsp;
//      * {@.en The message is sent to all nodes.}{@.ja メッセージは全ノードに送信されます．}
//      *
//      * @param senderId {@.en The sender PeerId string.}{@.ja 送信者ピアIDの文字列表現．}
//      * @param senderName {@.en The sender name string.}{@.ja 送信者ピアの表示名.}
//      * @param text {@.en The body text of the message.}{@.ja メッセージ本文のテキスト．}
//      */
//     public Message(PeerId senderId, String senderName, String text) {
//         Date now = new Date();
//         Date refDate = new Date(2001 - 1900, 1, 1);
//         this.id = senderId + "." + (now.getTime() - refDate.getTime());
//         this.source_id = senderId.toString();
//         this.screen_name = senderName;
//         this.content_type = "text/plain";
//         this.text = text;
//         this.ttl = DEFAULT_TTL;
//         this.von_key = null;
//         this.created_at = now;
//         this.expires_at = new Date(created_at.getTime() + DEFAULT_EXPIRE_INTERVAL);
//     }

//     /**
//      * {@.en Constructs a message with message id.}{@.ja メッセージIDを指定したメッセージの生成を行ないます．}&nbsp;
//      * {@.en The message is sent to all nodes.}{@.ja メッセージは全ノードに送信されます．}
//      *
//      * @param senderId {@.en The sender PeerId string.}{@.ja 送信者ピアIDの文字列表現．}
//      * @param senderName {@.en The sender name string.}{@.ja 送信者ピアの表示名.}
//      * @param text {@.en The body text of the message.}{@.ja メッセージ本文のテキスト．}
//      * @param messageId {@.en The message id.}{@.ja メッセージID．}
//      * 
//      */
//     public Message(PeerId senderId, String senderName, String text, String messageId) {
//         this.id = messageId;
//         this.source_id = senderId.toString();
//         this.screen_name = senderName;
//         this.content_type = "text/plain";
//         this.text = text;
//         this.ttl = DEFAULT_TTL;
//         this.von_key = null;
//         this.created_at = new Date();
//         this.expires_at = new Date(created_at.getTime() + DEFAULT_EXPIRE_INTERVAL);
//     }
    
    /**
     * {@.ja Message DB 内での行IDです．}{@.en The row id in the message database.}
     */
    public long row_id;
    /**
     * {@.ja メッセージIDです．}{@.en The message id.}
     */
    public String id;
    /**
     * {@.ja メッセージ本文の型情報です．RFC2045 の Content-Type フィールドを想定しています．}{@.en The content type.}{@.en It assumes RFC2045 Content-Type.}
     */
    public String content_type;
    /**
     * {@.ja 内容のテキストです．}{@.en The content text.}
     */
    public String text; 
    /**
     * {@.ja 生成日時です．}{@.en The creation time.}
     */
    public Date created_at; 
    /**
     * {@.ja 有効期限です．}{@.en The expiration time.}
     */
    public Date expires_at; 
    /**
     * {@.ja 送信者の表示名です．}{@.en The screen name.}
     */
    public String screen_name;
    /**
     * {@.ja 送信元ピアIDです．}{@.en The sender peer id.}
     */
    public String source_id;
    /**
     * {@.ja 送信先ピアIDです．}{@.en The recipient peer id.}
     */
    public String received_id;
    /**
     * {@.ja 受信日時です．}{@.en The received time.}
     */
    public Date received_at;
    /**
     * {@.ja メッセージの状態です．通常は null．}{@.en The status of the message.}{@.en Usually null.}
     */
    public String status;
    /**
     * {@.ja メッセージの受信条件です．通常は null．}{@.en The recipient condition.}{@.en Usually null.}
     */
    public String condition;
    /**
     * {@.ja VON Id です．}{@.en VON Id.}
     */
    public String von_id;
    /**
     * {@.ja 受信ピアIDです．全員宛なら null．}{@.en The recipient peer id.}{@.en If broadcast, null.}
     */
    public String recipient_id;
    /**
     * {@.ja 受信者の表示名です．}{@.en The recipient screen name. }
     */
    public String recipient_screen_name;
    /**
     * {@.ja 引用元メッセージIDです．引用元がないなら null．}{@.en The source message id.}{@.en If no source message, null.}
     */
    public String in_reply_to;
    /**
     * {@.ja 引用元メッセージ送信ピアIDです．引用元がないなら null．}{@.en The peer id of the sender of the source message. }{@.en If no source message, null.}
     */   
    public String in_reply_to_id;
    /**
     * {@.ja 引用元メッセージ送信ピアの表示名です．引用元がないなら null．}{@.en The screen name of the sender of the source message.}{@.en If no source message, null.}
     */   
    public String in_reply_to_screen_name;
    /**
     * {@.ja AccessKey などのセキュリティ情報です．セキュリティ指定なしなら null．}{@.en The security information of message(VON information etc.). }{@.en If no security, null.}
     */   
    public String secure_message;
    /**
     * {@.ja メッセージ経由ピアのIDリストです．}{@.en The relay peer id list of the message.}
     */
    public List<String> via;
    /**
     * {@.ja 経由ピアの残り数です．}{@.en The relay ttl(time to live) of the message.}
     */
    public int ttl;
    /**
     * {@.ja 未読なら <code>true</code>．}{@.en If unread, <code>true</code>.}
     */
    public boolean isNew;

    /**
     * {@.ja JSON 形式の文字列表現を得ます．}{@.en Returns the JSON serialized string.}
     * @return {@.ja JSON 形式の文字列．}{@.en JSON serialized string.}
     */
    public String toString() {
        JSONObject untyped = new JSONObject();
        try {
            untyped.put(KEY_MESSAGE_ID, id);
            if (content_type != null) {
                untyped.put(KEY_CONTENT_TYPE, content_type);
            }
            if (von_id != null) {
                untyped.put(KEY_VON_ID, von_id);
            }
            if (text != null) {
                untyped.put(KEY_TEXT, text);
            }
            if (created_at != null) {
                untyped.put(KEY_CREATED_AT, created_at.getTime() + "");
            }
            if (expires_at != null) {
                untyped.put(KEY_EXPIRES_AT, expires_at.getTime() + "");
            }
            if (screen_name != null) {
                untyped.put(KEY_SCREEN_NAME, screen_name);
            }
            if (source_id != null) {
                untyped.put(KEY_SOURCE_ID, source_id);
            }
            if (received_at != null) {
                untyped.put(KEY_RECEIVED_AT, received_at.getTime() + "");
            }
            if (status != null) {
                untyped.put(KEY_STATUS, status);
            }
            if (condition != null) {
                untyped.put(KEY_CONDITION, condition);
            }
            if (recipient_id != null) {
                untyped.put(KEY_RECIPIENT_ID, recipient_id);
            }
            if (recipient_screen_name != null) {
                untyped.put(KEY_RECIPIENT_SCREEN_NAME, recipient_screen_name);
            }
            if (in_reply_to != null) {
                untyped.put(KEY_REPLY_TO, in_reply_to);
            }
            if (in_reply_to_id != null) {
                untyped.put(KEY_REPLY_TO_ID, in_reply_to_id);
            }
            if (in_reply_to_screen_name != null) {
                untyped.put(KEY_REPLY_TO_SCREEN_NAME, in_reply_to_screen_name);
            }
            //            String sm = "";
            if (secure_message != null) {
                untyped.put(KEY_SECURE_MESSAGE, secure_message);
            }

            JSONArray arr = new JSONArray();
            if (via != null) {
                for (String v : via) {
                    arr.put(v);
                }
            }
            untyped.put(KEY_VIA, arr.toString());
            untyped.put(KEY_TTL, ttl);
            
            return untyped.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public JSONObject getJSONObject() {
        JSONObject untyped = new JSONObject();
        try {
            untyped.put(KEY_MESSAGE_ID, id);
            if (content_type != null) {
                untyped.put(KEY_CONTENT_TYPE, content_type);
            }
            if (von_id != null) {
                untyped.put(KEY_VON_ID, von_id);
            }
            if (text != null) {
                untyped.put(KEY_TEXT, text);
            }
            if (created_at != null) {
                untyped.put(KEY_CREATED_AT, created_at.getTime() + "");
            }
            if (expires_at != null) {
                untyped.put(KEY_EXPIRES_AT, expires_at.getTime() + "");
            }
            if (screen_name != null) {
                untyped.put(KEY_SCREEN_NAME, screen_name);
            }
            if (source_id != null) {
                untyped.put(KEY_SOURCE_ID, source_id);
            }
            if (received_at != null) {
                untyped.put(KEY_RECEIVED_AT, received_at.getTime() + "");
            }
            if (status != null) {
                untyped.put(KEY_STATUS, status);
            }
            if (condition != null) {
                untyped.put(KEY_CONDITION, condition);
            }
            if (recipient_id != null) {
                untyped.put(KEY_RECIPIENT_ID, recipient_id);
            }
            if (recipient_screen_name != null) {
                untyped.put(KEY_RECIPIENT_SCREEN_NAME, recipient_screen_name);
            }
            if (in_reply_to != null) {
                untyped.put(KEY_REPLY_TO, in_reply_to);
            }
            if (in_reply_to_id != null) {
                untyped.put(KEY_REPLY_TO_ID, in_reply_to_id);
            }
            if (in_reply_to_screen_name != null) {
                untyped.put(KEY_REPLY_TO_SCREEN_NAME, in_reply_to_screen_name);
            }
            if (secure_message != null) {
                untyped.put(KEY_SECURE_MESSAGE, secure_message);
            }
            
            JSONArray arr = new JSONArray();
            if (via != null) {
                for (String v : via) {
                    arr.put(v);
                }
            }
            untyped.put(KEY_VIA, arr.toString());
            untyped.put(KEY_TTL, ttl);
            return untyped;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * {@.ja JSON 形式の文字列表現からメッセージを生成します．}{@.en Constructs a Message from JSON serialized string.}
     * @param json {@.ja JSON 形式の文字列表現．}{@.en The JSON serialized string.}
     * @return {@.ja メッセージオブジェクト．}{@.en The message object.}
     */    
    public static MessageData fromJson(String json) {
        JSONObject obj = null;
        try {
            obj = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return fromJsonObject(obj);
    }

    /**
     * {@.ja JSON オブジェクトから生成します．}{@.en Constructs a message from JSON object.}
     * 
     * @param obj {@.en The object to construct from.}{@.ja 生成の元とするオブジェクト}
     */
    public static MessageData fromJsonObject(JSONObject obj) {
        MessageData m = new MessageData();
        try {
            if (obj.has(KEY_SCREEN_NAME)) {
                m.screen_name = (String)obj.get(KEY_SCREEN_NAME);
            }
            if (obj.has(KEY_TEXT)) {
                m.text = (String)obj.get(KEY_TEXT);
            }
            String tmp = null;
            if (obj.has(KEY_CREATED_AT)) {
                tmp = (String)obj.get(KEY_CREATED_AT);
                m.created_at = tmp.length() == 0 ? null : new Date(Long.parseLong(tmp));
            }
            if (obj.has(KEY_RECEIVED_AT)) {
                tmp = (String)obj.get(KEY_RECEIVED_AT);
                m.received_at = tmp.length() == 0 ? null : new Date(Long.parseLong(tmp));
            }
            if (obj.has(KEY_EXPIRES_AT)) {
                tmp = (String)obj.get(KEY_EXPIRES_AT);
                m.expires_at = tmp.length() == 0 ? null : new Date(Long.parseLong(tmp));
            }
            if (obj.has(KEY_MESSAGE_ID)) {
                tmp = (String)obj.get(KEY_MESSAGE_ID);
                m.id = tmp.length() == 0 ? null : tmp;
            }
            if (obj.has(KEY_CONTENT_TYPE)) {
                tmp = (String)obj.getString(KEY_CONTENT_TYPE);
                m.content_type = tmp.length() == 0? null : tmp;
            }
            if (obj.has(KEY_VON_ID)) {
                tmp = (String)obj.getString(KEY_VON_ID);
                m.von_id = tmp.length() == 0? null : tmp;
            }
            if (obj.has(KEY_SOURCE_ID)) {
                tmp = (String)obj.get(KEY_SOURCE_ID);
                m.source_id = tmp.length() == 0 ? null : tmp;
            }
            if (obj.has(KEY_STATUS)) {
                tmp = (String)obj.get(KEY_STATUS);
                m.status = tmp.length() == 0 ? null : tmp;
            }
            if (obj.has(KEY_CONDITION)) {
                tmp = (String)obj.get(KEY_CONDITION);
                m.condition = tmp.length() == 0 ? null : tmp;
            }
            if (obj.has(KEY_RECIPIENT_ID)) {
                tmp = (String)obj.get(KEY_RECIPIENT_ID);
                m.recipient_id = tmp.length() == 0 ? null : tmp;
            }
            if (obj.has(KEY_RECIPIENT_SCREEN_NAME)) {
                tmp = (String)obj.get(KEY_RECIPIENT_SCREEN_NAME);
                m.recipient_screen_name = tmp.length() == 0 ? null : tmp;
            }
            if (obj.has(KEY_REPLY_TO)) {
                tmp = (String)obj.get(KEY_REPLY_TO);
                m.in_reply_to = tmp.length() == 0 ? null : tmp;
            }
            if (obj.has(KEY_REPLY_TO_ID)) {
                tmp = (String)obj.get(KEY_REPLY_TO_ID);
                m.in_reply_to_id = tmp.length() == 0 ? null : tmp;
            }
            if (obj.has(KEY_REPLY_TO_SCREEN_NAME)) {
                tmp = (String)obj.get(KEY_REPLY_TO_SCREEN_NAME);
                m.in_reply_to_screen_name = tmp.length() == 0 ? null : tmp;
            }
            if (obj.has(KEY_SECURE_MESSAGE)) {
                tmp = (String)obj.get(KEY_SECURE_MESSAGE);
                m.secure_message = tmp;//.length() == 0 ? null : AccessKey.parse(new String(Base64.decode(tmp)));            
            }
            if (obj.has(KEY_VIA)) {
                ArrayList<String> via = null;
                JSONArray arr = null;
                arr = new JSONArray((String)obj.get(KEY_VIA));
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        String nid = null;
                        try {
                            nid = (String) arr.get(i);
                        } catch (JSONException e) {
                        }
                        if (nid != null) {
                            if (via == null) {
                                via = new ArrayList<String>();
                            }
                            via.add(nid);
                        }
                    }
                }
                m.via = via;
            }
            if (obj.has(KEY_TTL)) {
                m.ttl = ((Integer)obj.get(KEY_TTL)).intValue();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return m;
    }
    
    /**
     * {@.ja クローンオブジェクトを返却します．}{@.en Returns a clone object}
     * @return Message {@.ja オブジェクト}{@.en object} 
     */
    public MessageData copy() {
        MessageData m = new MessageData();
        m.condition = condition; 
        m.content_type = content_type;
        m.created_at = created_at;
        m.expires_at = expires_at;
        m.id = id;
        m.in_reply_to = in_reply_to;
        m.in_reply_to_id = in_reply_to_id;
        m.in_reply_to_screen_name = in_reply_to_screen_name;
        m.isNew = isNew;
        m.received_at = received_at;
        m.received_id = received_id;
        m.recipient_id = recipient_id;
        m.recipient_screen_name = recipient_screen_name;
        m.row_id = row_id;
        m.screen_name = screen_name;
        m.secure_message = secure_message;
        m.source_id = source_id;
        m.status = status;
        m.text = text;
        m.ttl = ttl;
        m.via = via;
        m.von_id = von_id;
        return m;
    }

    /**
     * {@.ja 読み取り可能な本文文字列を取り出します．}{@.en Extract readable message text.}
     */
//    public String getText() {
//        if (content_type != null && content_type.startsWith("text/encrypted")) {
//            try {
//                if (von_id != null && recipient_id == null) {
//                    String encType = extractEncryptedContentType();
//                    if (encType != null) {
//                        if (encType.equals("recipient") && recipient_id != null) {
//                            
//                        }
//                    }
//                    String decrypted = new String(PBEUtil.decrypt(Base64.decode(text),
//                                                                  von_key), "UTF-8");
//                    return decrypted;
//                }
//                if (recipient_id != null) {
//                    
//                }
//            } catch (IOException e) {
//            }
//            return "-- extract failed --";
//        }
//        return text;
//    }

    
    /**
     * {@.ja 暗号化されているかどうか判定します．}{@.en Check whether the message is encrypted or not.} 
     * @return {@.ja 暗号化されているなら} true {@.en if encrypted} 
     */
    public boolean isEncrypted() {
        return content_type != null && content_type.startsWith("text/encrypted");
    }
    
    /**
     * {@.ja content_type を encrypted にします．}{@.en Set content_type to encrypted.}
     */
    public void setEncryptedContentType(String type) {
        this.content_type = "text/encrypted; type=" + type + "; original=" + (this.content_type == null ? "" : this.content_type);
        //System.out.println(this.content_type);
    }
    
    /**
     * {@.ja content_type を encrypted から解除し，暗号化タイプを返します．}{@.en Returns encrypted type and set content_type as un-encrypted.}
     * @return {@.ja 暗号化タイプ(recipient or von)．}{@.en The type of encription(recipient or von).}
     */
    public String extractEncryptedContentType() {
        String enc_type = null;
        if (content_type.startsWith("text/encrypted")) {
            String[] arr = content_type.split("; ");
            for (String param : arr) {
                String[] pair = param.split("=");
                if (pair[0].equals("type")) {
                    enc_type = pair[1];
                }
                if (pair[0].equals("original")) {
                    content_type = pair[1];                    
                }
            }
        }
        return enc_type;
    }

//     static public void main(String args[]) {
//         Message m = new Message();
//         m.id = "abc";
//         m.text = "text";
//         m.via = new ArrayList<String>();
//         m.via.add("def");
//         m.via.add("ghi");
//         m.via.add("jkl");
//         m.created_at = new Date();
//         String j = m.getJson();
//         System.out.println(j);
//         Message m2 = Message.fromJson(j);
//         String j2 = m2.getJson();
//         System.out.println(j2);
//     }
}
