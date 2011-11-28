package jp.android_group.payforward.monac;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Date;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.piax.ov.jmes.MessageData;
import org.piax.ov.ovs.dtn.MessageDB;
import org.piax.trans.util.PeriodicRunner;

public class MessageSynchronizer implements Runnable {
    PeriodicRunner pr;
    static public final String PATH_MESSAGE_DIFF = "/monac/MonacGateway/message_d";
    static public final String PATH_MESSAGE_RECEIVER = "/monac/MonacGateway/message_r";
    // Should adjust dynamically.
    static public final int SYNCHRONIZE_INTERVAL = 30 * 1000;
    static public final int SYNCHRONIZE_MESSAGE_LIMIT = 50;
    MessageDB mdb;

    public MessageSynchronizer (MessageDB mdb) {
        pr = new PeriodicRunner(SYNCHRONIZE_INTERVAL);
        pr.addTask(this);
        this.mdb = mdb;
    }
    
    public void run() {
        doSynchronize();
    }
    
    public void stop() {
        pr.stop();
    }
    
    private JSONArray listNonExpiredMessageIds() {
        return mdb.getMessageIdArray(new Date(), SYNCHRONIZE_MESSAGE_LIMIT, "status=\"tweet=require\"");
    }
    
    private void pushMessages(JSONArray missings) {
        
        if (missings.length() == 0) {
            return;
        }
        HttpClient client = new DefaultHttpClient();
        // Should it be HTTP/1.1?
        //  client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpPost post = new HttpPost(Monac.GATEWAY_URL + PATH_MESSAGE_RECEIVER);
        MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        
        JSONArray mess = new JSONArray();
        // reverse order.
        for (int i = missings.length() - 1; i >= 0; i--) {
            String mid = null;
            try {
                mid = missings.getString(i);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            MessageData mes = null;
            if (mid != null) {
                mes = mdb.fetchMessage(mid);
            }
            if (mes != null) { 
                mess.put(mes.getJSONObject());
            }
        }
        try {
            try {
                entity.addPart("messages", new StringBody(mess.toString(), "text/plain",
                        Charset.forName("UTF-8")));
            } catch (IllegalCharsetNameException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (UnsupportedCharsetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            post.setEntity(entity);
            String response = null;
            try {
                response = EntityUtils.toString(client.execute(post).getEntity(), "UTF-8");
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (response != null) {
                System.out.println("RESPONSE=" + response);
                JSONArray posteds = new JSONArray(response);
                //pushMessages(missings);
            }
        } catch (JSONException e) {

            e.printStackTrace();
        }
    }
    
    public void doSynchronize() {
        HttpClient client = new DefaultHttpClient();
        // Should it be HTTP/1.1?
        //  client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpPost post = new HttpPost(Monac.GATEWAY_URL + PATH_MESSAGE_DIFF);
        MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        JSONArray arr = listNonExpiredMessageIds();
        
        
        
        
        try {
            try {
                entity.addPart("available", new StringBody(arr.toString(), "text/plain",
                                                            Charset.forName("UTF-8")));
            } catch (IllegalCharsetNameException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (UnsupportedCharsetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            post.setEntity(entity);
            String response = null;
            try {
                response = EntityUtils.toString(client.execute(post).getEntity(), "UTF-8");
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            System.out.println("RESPONSE=" + response);
            if (response != null) {
                JSONArray missings = new JSONArray(response);
                if (missings.length() != 0) {
                    pushMessages(missings);
                }
            }
        } catch (JSONException e) {

            e.printStackTrace();
        }
    }
}