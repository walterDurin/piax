package jp.android_group.payforward.monac;
 
import java.text.SimpleDateFormat;

import jp.android_group.payforward.monac.R;

import org.json.JSONException;
import org.piax.ov.jmes.MessageData;
import org.piax.ov.jmes.Message;
import org.piax.ov.jmes.MessageSecurityManager;

import android.app.Dialog;
import android.content.Context;
import android.text.Html;
import android.text.method.LinkMovementMethod;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
 
public class MessageDialog extends Dialog implements OnClickListener {
    Button replyButton;
    Button citeReplyButton;
    Button directButton;
    TextView body;
    MessageData mes;
    Monac monac;
 
    public MessageDialog(Context ctxt, MessageData capsulated, Monac monac) {
        super(ctxt);
        this.mes = capsulated;
        this.monac = monac;
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        setContentView(R.layout.message_dialog);

        body = (TextView) findViewById(R.id.mes_text);
        
        Message mi = ((MessageSecurityManager)monac.dtn.getSecurityManager()).decapsulate(capsulated);
        if (mi == null) {
            body.setText(ctxt.getString(R.string.encrypted_text));
        }
        else if (mi.allDecrypted) {
            body.setText(mi.data.text);
        }
        else {
            body.setText(ctxt.getString(R.string.encrypted_text));
        }
        
        //        body.setMovementMethod(new ScrollingMovementMethod()); 
        body.setMovementMethod(LinkMovementMethod.getInstance());

        TextView tv = (TextView) findViewById(R.id.message_detail);
        CharSequence source = Html.fromHtml("<html><body><small>" +
                                            (mes.in_reply_to_screen_name == null? "" : "Reply to:" + mes.in_reply_to_screen_name + "<BR>") +
                                            (mes.recipient_screen_name == null? "" : "Direct to:" + mes.recipient_screen_name + "<BR>") +
                                            "Created at " + dateFormatter.format(mes.created_at) + "<BR>" + "Received at " + dateFormatter.format(mes.received_at) + "<BR>" + "Expires at " + dateFormatter.format(mes.expires_at) + "<BR>" + 
                                            "TTL: " + mes.ttl + "<BR>"
                                            + "</small></body></html>");
        tv.setText(source);
        
        replyButton = (Button) findViewById(R.id.mes_reply_button);
        citeReplyButton = (Button) findViewById(R.id.mes_citereply_button);
        directButton = (Button) findViewById(R.id.mes_direct_button);
        replyButton.setOnClickListener(this);
        citeReplyButton.setOnClickListener(this);
        directButton.setOnClickListener(this);
        setTitle(mes.screen_name);
    }
    @Override
    public void onClick(View v) {
        if (v == replyButton) {
            monac.draftForReply(mes);
        }
        else if (v == citeReplyButton) {
            monac.draftForCiteReply(mes);
        }
        else if (v == directButton) {
            monac.draftForDirect(mes);
        }
    }
}