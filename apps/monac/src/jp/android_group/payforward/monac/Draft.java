package jp.android_group.payforward.monac;

import java.util.Date;

import jp.android_group.payforward.monac.R;

import org.piax.ov.jmes.MessageData;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;

import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class Draft extends Activity {
    private EditText mBodyText;
    private CheckBox draftTweet;
    private String sourceId;
    private String screenName;

    private String inReplyTo;
    private String inReplyToId;
    private String inReplyToScreenName;

    private String recipientId;
    private String recipientScreenName;

    //    String vonEntries;
    //JSONArray vonEntries;
    
    //String vonId;

    int DRAFT_LIMIT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.draft);

        DRAFT_LIMIT = Integer.parseInt(getString(R.string.text_limit));

        //        TextView tv = (TextView) findViewById(R.id.draft_title);
        //        tv.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

        Bundle extras = getIntent().getExtras();
        String text = null;
        String action = null;
        sourceId = null;
        screenName = null;

        inReplyTo = null;
        inReplyToId = null;
        inReplyToScreenName = null;

        recipientId = null;
        recipientScreenName = null;
       // vonId = null;

        draftTweet = (CheckBox) findViewById(R.id.draft_tweet);
        draftTweet.setChecked(true); // default is tweet;
      //      Spinner spinner = (Spinner) findViewById(R.id.von_spinner);

        if (extras != null) {
            text = extras.getString(Monac.KEY_DRAFT_TEXT);
            action = extras.getString(Monac.KEY_DRAFT_ACTION);

            sourceId = extras.getString(Monac.KEY_DRAFT_SOURCE_ID);
            screenName = extras.getString(Monac.KEY_DRAFT_SCREEN_NAME);
            
            recipientId = extras.getString(Monac.KEY_DRAFT_RECIPIENT_ID);
            recipientScreenName = extras.getString(Monac.KEY_DRAFT_RECIPIENT_SCREEN_NAME);

            inReplyTo = extras.getString(Monac.KEY_DRAFT_IN_REPLY_TO);
            inReplyToId = extras.getString(Monac.KEY_DRAFT_IN_REPLY_TO_ID);
            inReplyToScreenName = extras.getString(Monac.KEY_DRAFT_IN_REPLY_TO_SCREEN_NAME);
        }
        
        TextView tv = (TextView) findViewById(R.id.draft_prompt);
        if (action.equals("new")) {
            tv.setText(getString(R.string.new_from));
        }
        else if (action.equals("reply") || action.equals("citeReply")) {
            tv.setText(getString(R.string.replyto));
        }
        else if (action.equals("direct")) {
            tv.setText(getString(R.string.directto));
        }
        mBodyText = (EditText) findViewById(R.id.body);
        mBodyText.addTextChangedListener (new TextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                }
                @Override
                public void beforeTextChanged(CharSequence s, int start,
                                              int count, int after) {
                }
                @Override
                public void onTextChanged(CharSequence s, int start,
                                          int before, int count) {
                    TextView cv = (TextView) findViewById(R.id.draft_count);
                    cv.setText("" + (DRAFT_LIMIT - s.toString().length()));
                }
            });
        if (text != null) {
            mBodyText.setText(text);
        }

        if (action != null && action.equals("citeReply")) {
            // XXX Issue 9051
            mBodyText.setSelection(0, mBodyText.getText().length());
            mBodyText.setSelection(0);
        }

        Button sendButton = (Button) findViewById(R.id.draft_send);
        Button cancelButton = (Button) findViewById(R.id.draft_cancel);
       
        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Bundle bundle = new Bundle();
                bundle.putString(Monac.KEY_DRAFT_ACTION, "send");
                MessageData mes = new MessageData();
                Date now = new Date();
                //                mes.expires_at = new Date(now.getTime() + Message.DEFAULT_EXPIRE_INTERVAL);
                // mes.created_at = now;
                // duplicated constructor..
                //Date refDate = new Date(2001 - 1900, 1, 1);
                //                mes.id = sourceId + "." + (now.getTime() - refDate.getTime());
                //                mes.source_id = sourceId;
                //mes.screen_name = screenName;

                mes.in_reply_to = inReplyTo;
                mes.in_reply_to_id = inReplyToId;
                mes.in_reply_to_screen_name = inReplyToScreenName;

                mes.recipient_id = recipientId;
                mes.recipient_screen_name = recipientScreenName;

                mes.text = mBodyText.getText().toString();
                mes.content_type = "text/plain";
                // mes.ttl = Message.DEFAULT_TTL;

                //
                mes.received_at = now;
                bundle.putSerializable(Monac.KEY_DRAFT_MESSAGE, mes);
                bundle.putString(Monac.KEY_DRAFT_TWEET_STATUS, (draftTweet.isChecked() ? "require" : "none"));

                Intent intent = new Intent();
                intent.putExtras(bundle);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Bundle bundle = new Bundle();
                bundle.putString(Monac.KEY_DRAFT_ACTION, "cancel");
                bundle.putString(Monac.KEY_DRAFT_TEXT, mBodyText.getText().toString());
                Intent intent = new Intent();
                intent.putExtras(bundle);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }
}
