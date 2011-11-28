package org.piax.ov.ovs.dtn;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.piax.ov.jmes.MessageData;
import org.piax.ov.jmes.von.VONEntry;

/**
 * {@.en MessageDB class corresponds to the storage of the DTN store&forward.}
 * {@.ja MessageDB クラスは，DTN 蓄積転送におけるメッセージ蓄積用データベースです．}
 * <p>
 * {@.en Usually it is not necessary to access this class. But only if the status stored messages are required, this class is used. The instance of MessageDB class can be obtained by method }
 * {@.ja 通常このクラスを直接アクセスする必要はありませんが，蓄積されているメッセージの状態を得たい場合などに用います．MessageDB クラスのインスタンスは，DTN クラスの }{@link DTN#getDB() getDB} {@.ja により得られます．}{@.en of DTN class.}
 */
public abstract class MessageDB {
    /**
     * {@.ja メッセージを蓄積する．}{@.en Stores a message.}
     * @param mes {@.ja 蓄積するメッセージ．}{@.en The message to store}
     * @return {@.ja メッセージの蓄積された列番号．エラーの場合 -1．}{@.en The row id of the database. -1 if error.}
     */
    public abstract long storeMessage(MessageData mes);
    /**
     * {@.ja メッセージを取り出す．}{@.en Fetches a message.}
     * @param id {@.ja メッセージID．}{@.en The message ID.}
     * @return {@.ja メッセージオブジェクト．エラーなら null．}{@.en Message object. null if error.}
     */
    public abstract MessageData fetchMessage(String id);
    
    /**
     * {@.ja 全てのメッセージIDリストを得ます．}{@.en Fetches a message id list.}
     * @param limit {@.ja メッセージIDの上限数．}{@.en The max number of message ids}
     * @return {@.ja メッセージID文字列の配列．}{@.en An array of message id strings.}
     */
    public abstract ArrayList<String> getAllMessageIdArray(int limit);
    /**
     * {@.ja 無効ではないメッセージIDリストを得ます．}{@.en Fetches a message id list.}
     * @param expireTime {@.ja 有効期限時刻(現在時刻)の指定．}{@.en The expiration time, which is, current time.}
     * @param limit {@.ja メッセージIDの上限数．}{@.en The max number of message ids}
     * @return {@.ja メッセージID文字列の配列．}{@.en An array of message id strings.}
     */
    public abstract ArrayList<String> getMessageIdArray(Date expireTime, int limit);

    /**
     * {@.ja すべてのメッセージ数を得ます．}{@.en Returns number of all messages.}
     * @return {@.ja メッセージ数．}{@.en number of messages.}
     */
    public abstract long countMessages();
    /**
     * {@.ja 無効ではないメッセージ数を得ます．}{@.en Returns number of messages.}
     * @param expireTime {@.ja 有効期限時刻(現在時刻)の指定．}{@.en The expiration time, which is, current time.}
     * @return {@.ja メッセージ数．}{@.en number of messages.}
     */
    public abstract long countMessages(Date expireTime);
    
    /**
     * {@.ja メッセージを削除する．}{@.en Removes a message.}
     * @param id {@.ja メッセージID．}{@.en The message ID.}
     * @return {@.ja 成功ならtrue．}{@.en true if removed successfully.}
     */
    public abstract boolean removeMessage(String id);
    /**
     * {@.ja メッセージIDリストを得ます．}{@.en Fetches a message id list.}
     * @param expireTime {@.ja 有効期限時刻(現在時刻)の指定．}{@.en The expiration time, which is, current time.}
     * @param limit {@.ja メッセージIDの上限数．}{@.en The max number of message ids}
     * @param extraCond {@.ja 付加的な検索条件．}{@.en The extra search condition}
     * @return {@.ja メッセージID文字列の JSON 配列．}{@.en A JSON Array of message id strings.}
     */
    public abstract JSONArray getMessageIdArray(Date expireTime, int limit, String extraCond);
    
    /**
     * {@.ja メッセージを既読とする．}{@.en Marks a message as read.}
     * @param rowId {@.ja 列番号．}{@.en The row id.}
     * @return {@.ja エラーなら false．}{@.en false if error.}
     */
    public abstract boolean markAsRead(long rowId);
    /**
     * {@.ja 複数のメッセージを既読とする．}{@.en Marks messages as read.}
     * @param limit {@.ja 最近のいくつまでを対象とするかの上限}{@.en The limit of the number of recent messages.}
     * @return {@.ja エラーがひとつでもあれば false．}{@.en false if there is an error.}
     */
    public abstract boolean markAsReadAll(int limit);
    /**
     * {@.ja 未読のメッセージ数を得る．}{@.en Counts unread messages.}
     * @param limit {@.ja 最近のいくつまでを対象とするかの上限}{@.en The limit of the number of recent messages.}
     * @param vons {@.ja 登録している VON のリスト．null でも良い．} {@.en A list of VONs. It can be null.}
     * @return {@.ja 未読数．}{@.en the number of unread messages.}
     */
    public abstract int countUnread(int limit, List<VONEntry> vonEntries);

    //protected static final String KEY_NEW_FLAG = "new_flag";  
}
