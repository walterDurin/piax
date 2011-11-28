package jp.android_group.payforward.monac;

import java.text.SimpleDateFormat;
import java.util.Date;

import jp.android_group.payforward.monac.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.piax.ov.jmes.MessageData;
import org.piax.ov.jmes.Message;
import org.piax.ov.jmes.MessageSecurityManager;
import org.piax.ov.jmes.von.VON;
import org.piax.ov.ovs.dtn.MessageDB;
import org.piax.ov.ovs.dtn.impl.AndroidMessageDB;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class MessageCursorAdapter extends SimpleCursorAdapter {
    String peerId;
    Context ctxt;
    MessageSecurityManager mgr;
    
    public class MessageViewBinder implements SimpleCursorAdapter.ViewBinder
    {
	@Override
	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            int nFlagIndex = cursor.getColumnIndex(MessageData.KEY_NEW_FLAG);
            int nCreatedIndex = cursor.getColumnIndex(MessageData.KEY_CREATED_AT);
            int nTextIndex = cursor.getColumnIndex(MessageData.KEY_TEXT);
            int nTTLIndex = cursor.getColumnIndex(MessageData.KEY_TTL);
            int nSourceIndex = cursor.getColumnIndex(MessageData.KEY_SOURCE_ID);
            int nRecipientIndex = cursor.getColumnIndex(MessageData.KEY_RECIPIENT_ID);
            int nReplyToIndex = cursor.getColumnIndex(MessageData.KEY_REPLY_TO_ID);
            int nStatusIndex = cursor.getColumnIndex(MessageData.KEY_STATUS);

            int nSNIndex = cursor.getColumnIndex(MessageData.KEY_SCREEN_NAME);
            int nIRTIndex = cursor.getColumnIndex(MessageData.KEY_REPLY_TO_SCREEN_NAME);
            int nDTIndex = cursor.getColumnIndex(MessageData.KEY_RECIPIENT_SCREEN_NAME);
            int nVONIndex = cursor.getColumnIndex(MessageData.KEY_VON_ID);
            int nSecureMessageIndex = cursor.getColumnIndex(MessageData.KEY_SECURE_MESSAGE);
            String validityString = "";

            String replyToId = cursor.getString(nReplyToIndex);
            String sourceId = cursor.getString(nSourceIndex);
            String recipientId = cursor.getString(nRecipientIndex);
            
            if (nFlagIndex == columnIndex) {
                int new_flag = cursor.getInt(nFlagIndex);
                if (new_flag > 0) {
                    ((ImageView)view).setImageResource(R.drawable.unseen);
                }
                else {
                    ((ImageView)view).setImageBitmap(null);
                }
                return true;
            }
            if (nStatusIndex == columnIndex) {
                String status = cursor.getString(nStatusIndex);
                if (status.equals("tweet=require")) {
                    ((ImageView)view).setImageResource(R.drawable.twitter_icon);
                }
                else {
                    ((ImageView)view).setImageBitmap(null);
                }
                return true;
            }
            // if (nSourceIndex == columnIndex) {
//                 LinearLayout l = (LinearLayout) view;
//                 if (replyToId.equals(peerId)) {
//                     l.setBackgroundResource(R.drawable.back_reply);
//                 }
//                 else if (recipientId.equals(peerId)) {
//                     l.setBackgroundResource(R.drawable.back_direct);
//                 }                
//             }

            if (nSNIndex == columnIndex) {
                TextView tv = (TextView) view;
                LinearLayout l = (LinearLayout)(tv.getParent().getParent());
                if (!replyToId.equals("")) {
                    tv.setText(cursor.getString(nSNIndex) + " " + ctxt.getString(R.string.repliedto) + " " + cursor.getString(nIRTIndex) + " " + ctxt.getString(R.string.repliedto_tail));
                }
                else if (!recipientId.equals("")) {
                    tv.setText(cursor.getString(nSNIndex) + " " + ctxt.getString(R.string.to) + " " + cursor.getString(nDTIndex) + " " + ctxt.getString(R.string.to_tail));
                }
                else {
                    tv.setText(cursor.getString(nSNIndex));
                }
                if (replyToId.equals(peerId)) {
                    l.setBackgroundResource(R.drawable.back_reply);
                    tv.setTextColor(Color.parseColor("#800000"));
                }
                else if (recipientId.equals(peerId)) {
                    l.setBackgroundResource(R.drawable.back_direct);
                    tv.setTextColor(Color.parseColor("#000080"));
                }
                else {
                    l.setBackgroundResource(R.drawable.back);
                    tv.setTextColor(Color.parseColor("#000080"));
                }
                return true;
            }
            if (nTextIndex == columnIndex) {
                // text color is blue only when the source is myself.
                TextView tv = (TextView) view;
                MessageData capsulated = AndroidMessageDB.messageByCursor(cursor);
                Message mi = mgr.decapsulate(capsulated);
                if (mi == null) {
                    tv.setText(ctxt.getString(R.string.encrypted_text));
                }
                else if (mi.allDecrypted) {
                    //System.out.println("DECAPSULATED TEXT=" + mi.decrypted.text);
                    tv.setText(mi.data.text);
                }
                else {
                    tv.setText(ctxt.getString(R.string.encrypted_text));
                }
                if (mi != null) {
                    if (mi.validity == VON.VON_VALIDITY_SIGNATURE_FAILURE) {
                        validityString = "(SIG NG)";
                    }
                    else if (mi.validity == VON.VON_VALIDITY_MESSAGE_TOKEN_EXPIRED) {
                        validityString = "(Expired)";
                    }
                }
                
                if (replyToId.equals(peerId)) {
                    tv.setTextColor(Color.BLACK);
                }
                else if (recipientId.equals(peerId)) {
                    tv.setTextColor(Color.BLACK);
                }
                else if (sourceId.equals(peerId)) {
                    tv.setTextColor(Color.BLUE);
                }
                else {
                    tv.setTextColor(Color.BLACK);
                }
                return true;
            }
            if (nCreatedIndex == columnIndex) {
                TextView tv = (TextView) view;
                
                SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String date = dateFormatter.format(new Date(cursor.getLong(nCreatedIndex)));
                String ttl = "TTL:" + cursor.getInt(nTTLIndex);
                tv.setText(date + " " + ttl + " " + validityString);
                return true;
            }
            return false;
	}
    }

    public void setPeerIdString (String pids) {
        this.peerId = pids;
    }
    
    public void setVONManager(MessageSecurityManager mgr) {
        this.mgr = mgr;
    }

    public MessageCursorAdapter(Context context,
            int layout, Cursor c, String from[], int to[],
            String peerId, MessageSecurityManager mgr) {
        super(context, layout, c, from, to);
        this.peerId = peerId;
        this.ctxt = context;
        this.mgr = mgr;
        setViewBinder (new MessageViewBinder());
    }

}
