package org.piax.ov.ovs.dtn.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.piax.ov.jmes.MessageData;
import org.piax.ov.jmes.von.VONEntry;
import org.piax.ov.ovs.dtn.MessageDB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class AndroidMessageDB extends MessageDB {

    private static final String TAG = "AndroidMessageDB";
    private DatabaseHelper mDbHelper;
    //    private SQLiteDatabase mDb;
    private static final String KEY_NEW_FLAG = MessageData.KEY_NEW_FLAG;
    private static final String KEY_ROWID = "_id";
    private static final String DATABASE_CREATE =
        "create table if not exists MessageStore(_id INTEGER PRIMARY KEY AUTOINCREMENT, id TEXT UNIQUE NOT NULL, created_at DOUBLE PRECISION, expires_at DOUBLE PRECISION, screen_name TEXT, source_id TEXT, received_id TEXT, received_at DOUBLE PRECISION, status TEXT, condition TEXT, von_id TEXT, recipient_id TEXT, recipient_screen_name TEXT, in_reply_to TEXT, in_reply_to_id TEXT, in_reply_to_screen_name TEXT, via TEXT, content_type TEXT, text TEXT, secure_message TEXT, ttl INTEGER, new_flag INTEGER)";

    private static final String DATABASE_NAME = "piax-dtn-msgdb";
    private static final String DATABASE_TABLE = "MessageStore";
    private static final int DATABASE_VERSION = 2;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                  + newVersion + ", which will destroy all old data");
            //if (oldVersion == 1) {
//                db.execSQL("alter table MessageStore add column content_type TEXT");
//                db.execSQL("alter table MessageStore add column von_id TEXT");
               // db.execSQL("vacuum");
//            }
            
            //db.execSQL("DROP TABLE IF EXISTS MessageStore");
            onCreate(db);
        }
    }

    private SQLiteDatabase db() {
        return mDbHelper.getWritableDatabase();
    }


    public AndroidMessageDB(Context ctx) {
        this.mCtx = ctx;
    }

    public Cursor fetchAllMessages() {
        return db().query(DATABASE_TABLE,
                         new String[] {KEY_ROWID, MessageData.KEY_SCREEN_NAME,
                                       MessageData.KEY_TEXT},
                         null, null, null, null, null);
    }

    public AndroidMessageDB open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        SQLiteDatabase mDb = mDbHelper.getWritableDatabase();
        mDb.setLockingEnabled(true);
        return this;
    }
    
    public void close() {
        mDbHelper.close();
    }

    public long storeMessage(MessageData m) {
        ContentValues initialValues = new ContentValues();

        initialValues.put(MessageData.KEY_MESSAGE_ID, m.id);
        initialValues.put(MessageData.KEY_TEXT, m.text == null ? "" : m.text);
        initialValues.put(MessageData.KEY_CONTENT_TYPE, m.content_type == null ? "" : m.content_type);
        initialValues.put(MessageData.KEY_VON_ID, m.von_id == null ? "" : m.von_id);
        initialValues.put(MessageData.KEY_CREATED_AT, m.created_at == null ? "" : m.created_at.getTime() + "");
        initialValues.put(MessageData.KEY_EXPIRES_AT, m.expires_at == null ? "" : m.expires_at.getTime() + "");
        initialValues.put(MessageData.KEY_SCREEN_NAME, m.screen_name == null ? "" : m.screen_name);
        initialValues.put(MessageData.KEY_SOURCE_ID, m.source_id == null ? "" : m.source_id);
        initialValues.put(MessageData.KEY_RECEIVED_AT, m.received_at == null ? "" : m.received_at.getTime() + "");
        initialValues.put(MessageData.KEY_STATUS, m.status == null ? "" : m.status);
        initialValues.put(MessageData.KEY_CONDITION, m.condition == null ? "" : m.condition);
        initialValues.put(MessageData.KEY_RECIPIENT_ID, m.recipient_id == null ? "" : m.recipient_id);
        initialValues.put(MessageData.KEY_RECIPIENT_SCREEN_NAME, m.recipient_screen_name == null ? "" : m.recipient_screen_name);
        initialValues.put(MessageData.KEY_REPLY_TO, m.in_reply_to == null ? "" : m.in_reply_to);
        initialValues.put(MessageData.KEY_REPLY_TO_ID, m.in_reply_to_id == null ? "" : m.in_reply_to_id);
        initialValues.put(MessageData.KEY_REPLY_TO_SCREEN_NAME, m.in_reply_to_screen_name == null ? "" : m.in_reply_to_screen_name);
        initialValues.put(MessageData.KEY_SECURE_MESSAGE, m.secure_message == null ? "" : m.secure_message.toString());
        initialValues.put(KEY_NEW_FLAG, 1); 

        if (m.via != null) {
            JSONArray arr = new JSONArray();
            for (String v : m.via) {
                arr.put(v);
            }
            initialValues.put(MessageData.KEY_VIA, arr.toString());
        }
        else {
            initialValues.put(MessageData.KEY_VIA, "");
        }
        initialValues.put(MessageData.KEY_TTL, m.ttl);

        try {
            long ret = -1;
            SQLiteDatabase mDb = db();
            mDb.beginTransaction();
            try {
                ret = mDb.insert(DATABASE_TABLE, null, initialValues);
                mDb.setTransactionSuccessful();
            } finally {
                mDb.endTransaction();
            }
            return ret;
        }
        catch (Throwable e){
            e.printStackTrace();
            return -1;
        }
    }

    public boolean remove(long rowId) {
        SQLiteDatabase mDb = db();
        mDb.beginTransaction();
        boolean ret = false;
        try {
            ret = mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
        return ret;
    }
    
    public boolean removeMessage(String id) {
        SQLiteDatabase mDb = db();
        mDb.beginTransaction();
        boolean ret = false;
        try {
            ret = mDb.delete(DATABASE_TABLE, "id = ?", new String[] {id}) > 0;
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
        return ret;
        
    }

    public boolean markAsRead(long rowId) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_NEW_FLAG, new Integer(0));
        SQLiteDatabase mDb = db();
        mDb.beginTransaction();
        boolean ret = false;
        try {
            ret = mDb.update(DATABASE_TABLE, cv, KEY_ROWID + "=" + rowId, null) > 0;
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
        return ret;
    }

    static public MessageData messageByCursor (Cursor c) {
        MessageData m = new MessageData();
        int idx = c.getColumnIndex(MessageData.KEY_SCREEN_NAME);
        m.screen_name = c.getString(idx);
        idx = c.getColumnIndex(MessageData.KEY_TEXT);
        m.text = c.getString(idx);

        idx = c.getColumnIndex(MessageData.KEY_CONTENT_TYPE);
        m.content_type = c.getString(idx);
        if (m.content_type != null && m.content_type.length() == 0) {
            m.content_type = null;
        }
        
        idx = c.getColumnIndex(MessageData.KEY_VON_ID);
        m.von_id= c.getString(idx);
        if (m.von_id != null && m.von_id.length() == 0) {
            m.von_id = null;
        }
        
        idx = c.getColumnIndex(MessageData.KEY_CREATED_AT);
        m.created_at = new Date(c.getLong(idx));

        idx = c.getColumnIndex(MessageData.KEY_RECEIVED_AT);
        m.received_at = new Date(c.getLong(idx));

        idx = c.getColumnIndex(MessageData.KEY_EXPIRES_AT);
        m.expires_at = new Date(c.getLong(idx));

        idx = c.getColumnIndex(KEY_ROWID);
        m.row_id = c.getLong(idx);

        idx = c.getColumnIndex(MessageData.KEY_TTL);
        m.ttl = c.getInt(idx);

        idx = c.getColumnIndex(MessageData.KEY_MESSAGE_ID);
        m.id = c.getString(idx);
        if (m.id.length() == 0) {
            m.id = null;
        }
        
        idx = c.getColumnIndex(MessageData.KEY_SOURCE_ID);
        m.source_id = c.getString(idx);
        if (m.source_id.length() == 0) {
            m.source_id = null;
        }

        idx = c.getColumnIndex(MessageData.KEY_STATUS);
        m.status = c.getString(idx);
        if (m.status.length() == 0) {
            m.status = null;
        }

        idx = c.getColumnIndex(MessageData.KEY_CONDITION);
        m.condition = c.getString(idx);
        if (m.condition.length() == 0) {
            m.condition = null;
        }

        idx = c.getColumnIndex(MessageData.KEY_RECIPIENT_ID);
        m.recipient_id = c.getString(idx);
        if (m.recipient_id.length() == 0) {
            m.recipient_id = null;
        }

        idx = c.getColumnIndex(MessageData.KEY_RECIPIENT_SCREEN_NAME);
        m.recipient_screen_name = c.getString(idx);
        if (m.recipient_screen_name.length() == 0) {
            m.recipient_screen_name = null;
        }

        idx = c.getColumnIndex(MessageData.KEY_REPLY_TO);
        m.in_reply_to = c.getString(idx);
        if (m.in_reply_to.length() == 0) {
            m.in_reply_to = null;
        }

        idx = c.getColumnIndex(MessageData.KEY_REPLY_TO_ID);
        m.in_reply_to_id = c.getString(idx);
        if (m.in_reply_to_id.length() == 0) {
            m.in_reply_to_id = null;
        }

        idx = c.getColumnIndex(MessageData.KEY_REPLY_TO_SCREEN_NAME);
        m.in_reply_to_screen_name = c.getString(idx);
        if (m.in_reply_to_screen_name.length() == 0) {
            m.in_reply_to_screen_name = null;
        }

        idx = c.getColumnIndex(MessageData.KEY_SECURE_MESSAGE);
        String smStr = c.getString(idx);
        if (smStr.length() == 0) {
            m.secure_message = null;
        }
        else {
            m.secure_message = smStr;
        }
        
        idx = c.getColumnIndex(KEY_NEW_FLAG);
        int flag = c.getInt(idx);
        m.isNew = (flag == 1);

        idx = c.getColumnIndex(MessageData.KEY_VIA);
        ArrayList<String> via = null;
        JSONArray arr = null;
		try {
			arr = new JSONArray(c.getString(idx));
		} catch (JSONException e) {
			
		}
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
        
        return m;
    }

    public MessageData fetchMessage (long rowId) {
        SQLiteDatabase mDb = db();
        Cursor c = mDb.query(DATABASE_TABLE,
                             new String[] {
                                 KEY_ROWID, MessageData.KEY_MESSAGE_ID,
                                 MessageData.KEY_TEXT, MessageData.KEY_CONTENT_TYPE,
                                 MessageData.KEY_VON_ID,
                                 MessageData.KEY_CREATED_AT, MessageData.KEY_EXPIRES_AT,
                                 MessageData.KEY_SCREEN_NAME, MessageData.KEY_SOURCE_ID,
                                 MessageData.KEY_RECEIVED_AT, MessageData.KEY_STATUS,
                                 MessageData.KEY_CONDITION, MessageData.KEY_RECIPIENT_ID,
                                 MessageData.KEY_RECIPIENT_SCREEN_NAME,
                                 MessageData.KEY_REPLY_TO, MessageData.KEY_REPLY_TO_ID,
                                 MessageData.KEY_REPLY_TO_SCREEN_NAME,
                                 MessageData.KEY_SECURE_MESSAGE, MessageData.KEY_VIA, MessageData.KEY_TTL
                             },
                             "_id = ?", new String[] {"" + rowId}, null, null, null, null);
        MessageData m = AndroidMessageDB.messageByCursor(c);
        c.close();
        return m;
    }

    public boolean memberOfAll(int limit, long row_id) {
        SQLiteDatabase mDb = db();
        Cursor c = mDb.query(DATABASE_TABLE,
                             new String[] { KEY_ROWID },
                             null, null, null, null,
                             MessageData.KEY_CREATED_AT + " desc",
                             "" + limit);
        c.moveToLast();
        long lastRow = c.getLong(0);
        //        System.out.println("last row=" + lastRow);
        c.close();
        return (row_id > lastRow);
    }

    public boolean markAsReadAll(int limit) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_NEW_FLAG, new Integer(0));
        boolean ret = false;
        SQLiteDatabase mDb = db();
        mDb.beginTransaction();
        try {
            ret = mDb.update(DATABASE_TABLE, cv, MessageData.KEY_RECIPIENT_ID + "=''", null) > 0;
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
        return ret;
    }
    
    public boolean markAsReadAll(int limit, List<VONEntry> vonEntries) {
        String von_cond = vonCond(vonEntries);
        ContentValues cv = new ContentValues();
        cv.put(KEY_NEW_FLAG, new Integer(0));
        boolean ret = false;
        SQLiteDatabase mDb = db();
        mDb.beginTransaction();
        try {
            ret = mDb.update(DATABASE_TABLE, cv, MessageData.KEY_RECIPIENT_ID + "=''" + von_cond, null) > 0;
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
        return ret;
    }

    public int countUnread(int limit, List<VONEntry> vonEntries) {
        String von_cond = vonCond(vonEntries);
        SQLiteDatabase mDb = db();
        Cursor c = mDb.query(DATABASE_TABLE,
                             new String[] { "new_flag" },
                             MessageData.KEY_RECIPIENT_ID + "=''" + von_cond,
                             null, null, null,
                             MessageData.KEY_CREATED_AT + " desc",
                             "" + limit);
        int count = 0;
        int isNew = 0;
        c.moveToNext();
        while (!c.isAfterLast()) {
            isNew = c.getInt(0);
            if (isNew > 0) count ++;
            c.moveToNext();
        }
        //        System.out.println("unread count=" + count);
        c.close();
        return count;
    }

    public int countUnread(int limit) {
        SQLiteDatabase mDb = db();
        Cursor c = mDb.query(DATABASE_TABLE,
                             new String[] { "new_flag" },
                             MessageData.KEY_RECIPIENT_ID + "=''",
                             null, null, null,
                             MessageData.KEY_CREATED_AT + " desc",
                             "" + limit);
        int count = 0;
        int isNew = 0;
        c.moveToNext();
        while (!c.isAfterLast()) {
            isNew = c.getInt(0);
            if (isNew > 0) count ++;
            c.moveToNext();
        }
        //        System.out.println("unread count=" + count);
        c.close();
        return count;
    }

    @Override
    public ArrayList<String> getAllMessageIdArray(int limit) {
        ArrayList<String> arr = new ArrayList<String>();
        SQLiteDatabase mDb = db();
        mDb.beginTransaction();
        try {
            Cursor c = mDb.query(DATABASE_TABLE,
                                 new String[] {
                                     KEY_ROWID, MessageData.KEY_MESSAGE_ID,
                                 },
                                 null,
                                 null,
                                 null, null,
                                 MessageData.KEY_CREATED_AT + " desc",
                                 limit > 0 ? "" + limit : null);
            c.moveToNext();
            while (!c.isAfterLast()) {
                int idx = c.getColumnIndex(MessageData.KEY_MESSAGE_ID);
                arr.add(c.getString(idx));
                c.moveToNext();
            }
            c.close();
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
        return arr;
    }
    
    @Override
    public ArrayList<String> getMessageIdArray(Date expire, int limit) {
        ArrayList<String> arr = new ArrayList<String>();
        SQLiteDatabase mDb = db();
        mDb.beginTransaction();
        try {
            Cursor c = mDb.query(DATABASE_TABLE,
                                 new String[] {
                                     KEY_ROWID, MessageData.KEY_MESSAGE_ID,
                                 },
                                 "expires_at > ? and ttl > 0",
                                 new String[] {expire.getTime() + ""},
                                 null, null,
                                 MessageData.KEY_CREATED_AT + " desc",
                                 "" + limit);
            c.moveToNext();
            while (!c.isAfterLast()) {
                int idx = c.getColumnIndex(MessageData.KEY_MESSAGE_ID);
                arr.add(c.getString(idx));
                c.moveToNext();
            }
            c.close();
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
        return arr;
    }

    public JSONArray getMessageIdArray(Date expire, int limit, String extraCond) {
        JSONArray arr = new JSONArray();
        SQLiteDatabase mDb = db();
        mDb.beginTransaction();
        try {
            Cursor c = mDb.query(DATABASE_TABLE,
                                 new String[] {
                                     KEY_ROWID, MessageData.KEY_MESSAGE_ID,
                                 },
                                 "expires_at > ? and ttl > 0 and " + extraCond,
                                 new String[] {expire.getTime() + ""},
                                 null, null,
                                 MessageData.KEY_CREATED_AT + " desc",
                                 "" + limit);
            c.moveToNext();
            while (!c.isAfterLast()) {
                int idx = c.getColumnIndex(MessageData.KEY_MESSAGE_ID);
                arr.put(c.getString(idx));
                c.moveToNext();
            }
            c.close();
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
        return arr;
    }

    public Cursor searchNonExpired(Date date, int limit) {
        SQLiteDatabase mDb = db();
        return mDb.query(DATABASE_TABLE,
                         new String[] {
                             KEY_ROWID, MessageData.KEY_MESSAGE_ID,
                             MessageData.KEY_TEXT, MessageData.KEY_CONTENT_TYPE,
                             MessageData.KEY_VON_ID,
                             MessageData.KEY_CREATED_AT, MessageData.KEY_EXPIRES_AT,
                             MessageData.KEY_SCREEN_NAME, MessageData.KEY_SOURCE_ID,
                             MessageData.KEY_RECEIVED_AT, MessageData.KEY_STATUS,
                             MessageData.KEY_CONDITION, MessageData.KEY_RECIPIENT_ID,
                             MessageData.KEY_RECIPIENT_SCREEN_NAME,
                             MessageData.KEY_REPLY_TO, MessageData.KEY_REPLY_TO_ID,
                             MessageData.KEY_REPLY_TO_SCREEN_NAME,
                             MessageData.KEY_SECURE_MESSAGE, MessageData.KEY_VIA, MessageData.KEY_TTL,
                             KEY_NEW_FLAG
                         },
                         "expires_at > ? and ttl > 0",
                         new String[] {date.getTime() + ""},
                         null, null,
                         MessageData.KEY_CREATED_AT + " desc",
                         "" + limit);
    }

    public Cursor searchWithLimit(int limit) {
        SQLiteDatabase mDb = db();
        return mDb.query(DATABASE_TABLE,
                         new String[] {
                             KEY_ROWID, MessageData.KEY_MESSAGE_ID,
                             MessageData.KEY_TEXT, MessageData.KEY_CONTENT_TYPE,
                             MessageData.KEY_VON_ID,
                             MessageData.KEY_CREATED_AT, MessageData.KEY_EXPIRES_AT,
                             MessageData.KEY_SCREEN_NAME, MessageData.KEY_SOURCE_ID,
                             MessageData.KEY_RECEIVED_AT, MessageData.KEY_STATUS,
                             MessageData.KEY_CONDITION, MessageData.KEY_RECIPIENT_ID,
                             MessageData.KEY_RECIPIENT_SCREEN_NAME,
                             MessageData.KEY_REPLY_TO, MessageData.KEY_REPLY_TO_ID,
                             MessageData.KEY_REPLY_TO_SCREEN_NAME,
                             MessageData.KEY_SECURE_MESSAGE, MessageData.KEY_VIA, MessageData.KEY_TTL,
                             KEY_NEW_FLAG
                         },
                         MessageData.KEY_RECIPIENT_ID + "=''",
                         null, null, null,
                         MessageData.KEY_CREATED_AT + " desc",
                         "" + limit);
    }

    private String vonCond(List<VONEntry> vonEntries) {
        String von_cond = "";
        for (VONEntry ve : vonEntries) {
            if (von_cond.length() != 0) {
                von_cond += " OR ";
            }
            von_cond += MessageData.KEY_VON_ID + "='" + ve.name + "'";
        }
        if (von_cond.length() != 0) {
            von_cond = " AND (" + MessageData.KEY_SECURE_MESSAGE + "='' OR " + von_cond + ")";
        }
        //        System.out.println("VONCOND=" + von_cond);
        return von_cond;
    }
    
    public Cursor searchWithLimit(int limit, List<VONEntry> vonEntries) {
        String von_cond = vonCond(vonEntries);
        SQLiteDatabase mDb = db();
        return mDb.query(DATABASE_TABLE,
                new String[] {
                    KEY_ROWID, MessageData.KEY_MESSAGE_ID,
                    MessageData.KEY_TEXT, MessageData.KEY_CONTENT_TYPE,
                    MessageData.KEY_VON_ID,
                    MessageData.KEY_CREATED_AT, MessageData.KEY_EXPIRES_AT,
                    MessageData.KEY_SCREEN_NAME, MessageData.KEY_SOURCE_ID,
                    MessageData.KEY_RECEIVED_AT, MessageData.KEY_STATUS,
                    MessageData.KEY_CONDITION, MessageData.KEY_RECIPIENT_ID,
                    MessageData.KEY_RECIPIENT_SCREEN_NAME,
                    MessageData.KEY_REPLY_TO, MessageData.KEY_REPLY_TO_ID,
                    MessageData.KEY_REPLY_TO_SCREEN_NAME,
                    MessageData.KEY_SECURE_MESSAGE, MessageData.KEY_VIA, MessageData.KEY_TTL,
                    KEY_NEW_FLAG
                },
                MessageData.KEY_RECIPIENT_ID + "=''" + von_cond,
                null, null, null,
                MessageData.KEY_CREATED_AT + " desc",
                "" + limit);
    }

    public boolean markAsReadAll(String peerIdString) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_NEW_FLAG, new Integer(0));
        boolean ret = false;
        SQLiteDatabase mDb = db();
        mDb.beginTransaction();
        try {
            ret = mDb.update(DATABASE_TABLE, cv, "(" + MessageData.KEY_SOURCE_ID + "=? and " + MessageData.KEY_RECIPIENT_ID + "!='') or (" + MessageData.KEY_RECIPIENT_ID + "=?)", new String[] {peerIdString, peerIdString}) > 0;
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
        return ret;
    }

    public boolean markAsReadAll(String peerIdString, List<VONEntry> vonEntries) {
        String von_cond = vonCond(vonEntries);
        ContentValues cv = new ContentValues();
        cv.put(KEY_NEW_FLAG, new Integer(0));
        boolean ret = false;
        SQLiteDatabase mDb = db();
        mDb.beginTransaction();
        try {
            ret = mDb.update(DATABASE_TABLE, cv, "((" + MessageData.KEY_SOURCE_ID + "=? and " + MessageData.KEY_RECIPIENT_ID + "!='') or (" + MessageData.KEY_RECIPIENT_ID + "=?))" + von_cond, new String[] {peerIdString, peerIdString}) > 0;
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
        return ret;
    }

    public int countUnread(String peerIdString, List<VONEntry> vonEntries) {
        String von_cond = vonCond(vonEntries);
        SQLiteDatabase mDb = db();
        Cursor c = mDb.query(DATABASE_TABLE,
                             new String[] { "new_flag" },
                             "(" + MessageData.KEY_SOURCE_ID + "=? and " + MessageData.KEY_RECIPIENT_ID + "!='') or (" + MessageData.KEY_RECIPIENT_ID + "=?)" + von_cond,
                             new String[] {peerIdString, peerIdString},
                             null, null,
                             null, null);
        int count = 0;
        int isNew = 0;
        c.moveToNext();
        while (!c.isAfterLast()) {
            isNew = c.getInt(0);
            if (isNew > 0) count ++;
            c.moveToNext();
        }        
        //        System.out.println("unread count for inbox=" + count);
        c.close();
        return count;        
    }    

    public int countUnread(String peerIdString) {
        SQLiteDatabase mDb = db();
        Cursor c = mDb.query(DATABASE_TABLE,
                             new String[] { "new_flag" },
                             "(" + MessageData.KEY_SOURCE_ID + "=? and " + MessageData.KEY_RECIPIENT_ID + "!='') or (" + MessageData.KEY_RECIPIENT_ID + "=?)",
                             new String[] {peerIdString, peerIdString},
                             null, null,
                             null, null);
        int count = 0;
        int isNew = 0;
        c.moveToNext();
        while (!c.isAfterLast()) {
            isNew = c.getInt(0);
            if (isNew > 0) count ++;
            c.moveToNext();
        }        
        //        System.out.println("unread count for inbox=" + count);
        c.close();
        return count;        
    }    

    public Cursor searchWithRecipient(String peerIdString, List<VONEntry> vonEntries) {
        String von_cond = vonCond(vonEntries);
        SQLiteDatabase mDb = db();
        return mDb.query(DATABASE_TABLE,
                         new String[] { 
                             KEY_ROWID, MessageData.KEY_MESSAGE_ID,
                             MessageData.KEY_TEXT, MessageData.KEY_CONTENT_TYPE,
                             MessageData.KEY_VON_ID,
                             MessageData.KEY_CREATED_AT, MessageData.KEY_EXPIRES_AT,
                             MessageData.KEY_SCREEN_NAME, MessageData.KEY_SOURCE_ID,
                             MessageData.KEY_RECEIVED_AT, MessageData.KEY_STATUS,
                             MessageData.KEY_CONDITION, MessageData.KEY_RECIPIENT_ID,
                             MessageData.KEY_RECIPIENT_SCREEN_NAME,
                             MessageData.KEY_REPLY_TO, MessageData.KEY_REPLY_TO_ID,
                             MessageData.KEY_REPLY_TO_SCREEN_NAME,
                             MessageData.KEY_SECURE_MESSAGE, MessageData.KEY_VIA, MessageData.KEY_TTL,
                             KEY_NEW_FLAG
                         },
                         "(" + MessageData.KEY_SOURCE_ID + "=? and " + MessageData.KEY_RECIPIENT_ID + "!='') or (" + MessageData.KEY_RECIPIENT_ID + "=?)" + von_cond,
                         new String[] {peerIdString, peerIdString},
                         null, null,                      
                         MessageData.KEY_CREATED_AT + " desc", null);
    }

    public Cursor searchWithRecipient(String peerIdString) {
        SQLiteDatabase mDb = db();
        return mDb.query(DATABASE_TABLE,
                         new String[] { 
                             KEY_ROWID, MessageData.KEY_MESSAGE_ID,
                             MessageData.KEY_TEXT, MessageData.KEY_CONTENT_TYPE,
                             MessageData.KEY_VON_ID,
                             MessageData.KEY_CREATED_AT, MessageData.KEY_EXPIRES_AT,
                             MessageData.KEY_SCREEN_NAME, MessageData.KEY_SOURCE_ID,
                             MessageData.KEY_RECEIVED_AT, MessageData.KEY_STATUS,
                             MessageData.KEY_CONDITION, MessageData.KEY_RECIPIENT_ID,
                             MessageData.KEY_RECIPIENT_SCREEN_NAME,
                             MessageData.KEY_REPLY_TO, MessageData.KEY_REPLY_TO_ID,
                             MessageData.KEY_REPLY_TO_SCREEN_NAME,
                             MessageData.KEY_SECURE_MESSAGE, MessageData.KEY_VIA, MessageData.KEY_TTL,
                             KEY_NEW_FLAG
                         },
                         "(" + MessageData.KEY_SOURCE_ID + "=? and " + MessageData.KEY_RECIPIENT_ID + "!='') or (" + MessageData.KEY_RECIPIENT_ID + "=?)",
                         new String[] {peerIdString, peerIdString},
                         null, null,                      
                         MessageData.KEY_CREATED_AT + " desc", null);
    }

    public Cursor searchWithLimitFromStartSeq(int startSeq, int limit) {
        SQLiteDatabase mDb = db();
        return mDb.query(DATABASE_TABLE,
                         new String[] {
                             KEY_ROWID, MessageData.KEY_MESSAGE_ID,
                             MessageData.KEY_TEXT, MessageData.KEY_CONTENT_TYPE,
                             MessageData.KEY_VON_ID,
                             MessageData.KEY_CREATED_AT, MessageData.KEY_EXPIRES_AT,
                             MessageData.KEY_SCREEN_NAME, MessageData.KEY_SOURCE_ID,
                             MessageData.KEY_RECEIVED_AT, MessageData.KEY_STATUS,
                             MessageData.KEY_CONDITION, MessageData.KEY_RECIPIENT_ID,
                             MessageData.KEY_RECIPIENT_SCREEN_NAME,
                             MessageData.KEY_REPLY_TO, MessageData.KEY_REPLY_TO_ID,
                             MessageData.KEY_REPLY_TO_SCREEN_NAME,
                             MessageData.KEY_SECURE_MESSAGE, MessageData.KEY_VIA, MessageData.KEY_TTL,
                             KEY_NEW_FLAG
                         },
                         "_id < ?", new String[] {"" + startSeq}, null, null,
                         "_id desc",
                         "" + limit);
    }

    @Override
    public MessageData fetchMessage(String id) {
        SQLiteDatabase mDb = db();
        Cursor c = mDb.query(DATABASE_TABLE,
                             new String[] {
                                 KEY_ROWID, MessageData.KEY_MESSAGE_ID,
                                 MessageData.KEY_TEXT, MessageData.KEY_CONTENT_TYPE,
                                 MessageData.KEY_VON_ID,
                                 MessageData.KEY_CREATED_AT, MessageData.KEY_EXPIRES_AT,
                                 MessageData.KEY_SCREEN_NAME, MessageData.KEY_SOURCE_ID,
                                 MessageData.KEY_RECEIVED_AT, MessageData.KEY_STATUS,
                                 MessageData.KEY_CONDITION, MessageData.KEY_RECIPIENT_ID,
                                 MessageData.KEY_RECIPIENT_SCREEN_NAME,
                                 MessageData.KEY_REPLY_TO, MessageData.KEY_REPLY_TO_ID,
                                 MessageData.KEY_REPLY_TO_SCREEN_NAME,
                                 MessageData.KEY_SECURE_MESSAGE, MessageData.KEY_VIA, MessageData.KEY_TTL,
                                 KEY_NEW_FLAG
                             },
                             "id = ?", new String[] {id},
                             null, null, null, null);
        c.moveToNext();
        if (!c.isAfterLast()) {
            MessageData m = messageByCursor(c);
            c.close();
            return m;
        }
        return null;
    }


    @Override
    public long countMessages() {
        return DatabaseUtils.queryNumEntries(db(), DATABASE_TABLE);
    }


    @Override
    public long countMessages(Date expireTime) {
        SQLiteDatabase mDb = db(); 
        String count = "SELECT count(*) FROM " + DATABASE_TABLE + " where expires_at > ? and ttl > 0";
                
        Cursor mcursor = mDb.rawQuery(count, new String[] {expireTime.getTime() + ""});
        mcursor.moveToFirst();
        return mcursor.getLong(0);
    }
}
