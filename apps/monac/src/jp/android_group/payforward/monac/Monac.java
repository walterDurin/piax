package jp.android_group.payforward.monac;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.android_group.payforward.monac.R;

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

import org.piax.gnt.ReceiveListener;
import org.piax.gnt.Target;
import org.piax.gnt.handover.Peer;
import org.piax.ov.Overlay;
import org.piax.ov.jmes.MessageData;
import org.piax.ov.jmes.Message;
import org.piax.ov.jmes.MessageSecurityManager;
import org.piax.ov.jmes.authz.AccessToken;
import org.piax.ov.jmes.von.ManagedVON;
import org.piax.ov.jmes.von.VON;
import org.piax.ov.jmes.von.VONEntry;
import org.piax.ov.ovs.dtn.ebc.EpidemicBroadcast;
import org.piax.ov.ovs.dtn.impl.AndroidDTN;
import org.piax.ov.ovs.dtn.impl.AndroidMessageDB;
import org.piax.ov.ovs.dtn.DTN;


import org.piax.trans.common.PeerId;
import org.piax.trans.common.PeerLocator;
import org.piax.trans.ts.nfc.nfc.Nfc;
import org.piax.trans.ts.tcp.TcpLocator;
import org.piax.trans.ts.udp.UdpLocator;
import org.piax.trans.tsd.TSD;
import org.piax.trans.tsd.bluetooth.BluetoothTSD;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;

//import android.os.Debug;

public class Monac extends TabActivity implements ReceiveListener, OnPreparedListener {
    static public final String KEY_DRAFT_SOURCE_ID = "draftPeerId";
    static public final String KEY_DRAFT_SCREEN_NAME = "draftScreenName";
    static public final String KEY_DRAFT_TEXT = "draftText";
    static public final String KEY_DRAFT_ACTION = "draftAction";
    static public final String KEY_DRAFT_MESSAGE = "draftMessage";
    static public final String KEY_DRAFT_VON_ID = "draftVONId";
    static public final String KEY_DRAFT_TWEET_STATUS = "draftTweet";

    static public final String KEY_DRAFT_RECIPIENT_ID = "draftRecipientId";
    static public final String KEY_DRAFT_RECIPIENT_SCREEN_NAME = "draftRecipientScreenName";

    static public final String KEY_DRAFT_IN_REPLY_TO = "draftInReplyTo";
    static public final String KEY_DRAFT_IN_REPLY_TO_ID = "draftInReplyToId";
    static public final String KEY_DRAFT_IN_REPLY_TO_SCREEN_NAME = "draftInReplyToScreenName";

    static public final String KEY_SETTING_ACTION = "userName";
    
    private static final int MENU_DRAFT = (Menu.FIRST + 1);
    private static final int MENU_MARK_READ = (Menu.FIRST + 2);
    private static final int MENU_SETTING = (Menu.FIRST + 3);
    //    private static final int MENU_VON_SETTING = (Menu.FIRST + 4);
    private static final int MENU_FINISH = (Menu.FIRST + 4);
    private static final int MENU_SENSOR = (Menu.FIRST + 5);

    protected static final int ACTIVITY_CREATE = 1;

    static public final int PUBLIC_TL_LIMIT = 50;
    static public final int INBOX_TL_LIMIT = 50;
    
    //    private ArrayList list = null;  
    // ListAdapter 
    AlertDialog.Builder builder = null;
    String draftText = "";

    Activity activity;
    Intent draft;
    Intent setting;
    PeerId peerId = null;

    String peerIdString;
    String userName;

    boolean tsdConnect;
    String tsdConnectLocator;
    
    boolean runAsSeed;
    String tsdAcceptLocator;

    boolean enableLocalTSD;
    boolean useBroadcastTSD;

    boolean enableSynchronizer;
    
    
    boolean useBluetooth;
    boolean useWifi;
    boolean useNFC;
    
    String userPublicKey;
    String userPrivateKey;

    // DTN provided by PIAX
    static DTN dtn;
    
    // Nfc
    static Nfc nfc;
    
    NeighborAdapter mNeighborAdapter;

    SimpleDateFormat dateFormatter;

    Cursor mMessageCursor;
    Cursor mInboxCursor;
    MessageCursorAdapter mMessageAdapter;
    MessageCursorAdapter mInboxAdapter;

    View messageDetailView;
    MessageData mSelectedMessage;
    
    int publicCount;
    int inboxCount;

    TabHost tabs;
    MenuItem read_item;
    MenuItem draft_item;
    TabHost.TabSpec publicTab;
    TabHost.TabSpec inboxTab;

    ArrayList<Peer> neighborList;
    
    TSD bluetoothTSD = null;

    ListView nList, pList;
    Handler handler;

    Date lastNotified;
    
    static MessageSynchronizer msync;
    
    // static Twitter twitter;

    String twitterToken;
    String twitterTokenSecret;

    static public final String GATEWAY_URL = "https://payforwarding.android-group.jp";
    //static public final String GATEWAY_URL = "https://ec2-175-41-227-130.ap-northeast-1.compute.amazonaws.com";
    //static public final String GATEWAY_URL = "http://10.0.2.1:10080";
    static public final String PATH_AUTHENTICATE="/monac/MonacGateway/authz";
    static public final String PATH_AUTHORIZE="/monac/MonacGateway/authorize";
    public static final String CALLBACKURL="monac://main";
    
    static public String AUTHORITY_PUBLIC_KEY_STRING = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDTNQWcu0OGhhy/q2kCR/+uI3GT3YRb5dAe54fTyzoHLml26eYoF2OoqaHGCizzJkQsqjgEEOJ7dXteym63cM/lVkUNCPNkU8Y6SM7chNLwQ1IQQWbnUwacH+USD+RqlaBn2994P8e9JBjunb8HljKaKMYxL7rv22gISXDuPtvdlQIDAQAB";
    // not available in real implementation from here
    //String AUTHORITY_PRIVATE_KEY_STRING = null;
    //public static final long DEFAULT_EXPIRE_DURATION = 30 * 24 * 60 * 60 * 1000L; // 30 days;
    // to here
    
    //private AccessToken testToken(String pid, String uid, String publicKey) {
    //    HashMap<String,String> map = new HashMap<String,String>();
//        map.put("von_id", "default");
//        map.put("peer_id", pid);
//        map.put("user_id", uid);
//        map.put("von_key", "japan20110311");
//        List<String> sattrs = new ArrayList<String>();
//        sattrs.add("von_key");
//        Date expires = new Date(new Date().getTime() + DEFAULT_EXPIRE_DURATION);
//        System.out.println("token expires at:" + new Date().getTime() + "+" + DEFAULT_EXPIRE_DURATION + "=" +  expires.getTime());
//        AccessToken at = new AccessToken(map, sattrs, "RSA-SHA1", GATEWAY_URL, this.userPublicKey, expires);
//        at.updateTokenSignature(AUTHORITY_PRIVATE_KEY_STRING);
//        return at;
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {    
        draft_item = menu.add(Menu.NONE, MENU_DRAFT, Menu.NONE,
                              getString(R.string.menu_draft)).setIcon(R.drawable.ic_menu_compose);
        draft_item.setEnabled(dtn != null);     
        
        read_item = menu.add(Menu.NONE, MENU_MARK_READ, Menu.NONE,
                             getString(R.string.menu_mark_read));
        read_item.setIcon(android.R.drawable.ic_menu_agenda);
        read_item.setEnabled(dtn != null);
        if (tabs != null && tabs.getCurrentTabTag().equals("tab3")) {
            read_item.setEnabled(false);
        }
        
        // sensor demo version
        menu.add(Menu.NONE, MENU_SENSOR, Menu.NONE,
                "Sensor").setIcon(android.R.drawable.ic_menu_compass);
        
        draft_item.setEnabled(dtn != null);
        
        menu.add(Menu.NONE, MENU_SETTING, Menu.NONE,
                getString(R.string.menu_setting)).setIcon(android.R.drawable.ic_menu_preferences);

//         menu.add(Menu.NONE, MENU_VON_SETTING, Menu.NONE,
//                  getString(R.string.menu_von_setting)).setIcon(android.R.drawable.ic_menu_share);

        menu.add(Menu.NONE, MENU_FINISH, Menu.NONE,
                 getString(R.string.menu_finish)).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int fid, MenuItem item) {
    	int id = item.getItemId();
        switch (id) {
        // DUMMY_SENSOR
        case MENU_SENSOR:
            Intent si = new Intent(activity, Sensor.class);
            startActivityForResult(si, ACTIVITY_CREATE);
            return true;
        case MENU_DRAFT:
            clearDraftExtras();
            draft.putExtra(KEY_DRAFT_ACTION, "new");
            draft.putExtra(KEY_DRAFT_SOURCE_ID, peerIdString);
            draft.putExtra(KEY_DRAFT_SCREEN_NAME, userName);
            draft.putExtra(KEY_DRAFT_TEXT, draftText);
            startActivityForResult(draft, ACTIVITY_CREATE);
            return true;
        case MENU_MARK_READ:
            // mark as read all for current view;
            if (tabs.getCurrentTabTag().equals("tab1")) {
                ((AndroidMessageDB)dtn.getDB()).markAsReadAll(PUBLIC_TL_LIMIT, ((MessageSecurityManager)dtn.getSecurityManager()).getVONEntries());
                mMessageCursor.requery();
                mMessageAdapter.notifyDataSetChanged(); 
                publicCount = 0;
                updatePublicTab();
                //inboxCount = 0;
                //updateInboxTab();
            }
            if (tabs.getCurrentTabTag().equals("tab2")) {
                ((AndroidMessageDB)dtn.getDB()).markAsReadAll(peerIdString, ((MessageSecurityManager)dtn.getSecurityManager()).getVONEntries());
                mInboxCursor.requery();
                mInboxAdapter.notifyDataSetChanged();
                inboxCount = 0;
                updateInboxTab();
               // publicCount = ((AndroidMessageDB)dtn.getDB()).countUnread(PUBLIC_TL_LIMIT);
               // updatePublicTab();
            }
            return true;
        case MENU_SETTING:
            setting = new Intent(activity, Setting.class);
            startActivityForResult(setting, 0);
            return true;
//         case MENU_VON_SETTING:
//             Intent vonListIntent = new Intent(activity, VONList.class);
//             startActivityForResult(vonListIntent, 0);
//             return true;
        case MENU_FINISH:
            cleanup();
            finish();
            return true;
        }
        return false;
    }
    
    public List<VONEntry> getVONEntries() {
        return ((MessageSecurityManager)dtn.getSecurityManager()).getVONEntries();
    }

    public void cleanup() {
        if (dtn != null) {
            dtn.stop();
        }
        if (nfc != null) {
            // XXX something to cleanup?
        }
        dtn = null;
        nfc = null;
        if (msync != null) {
            msync.stop();
        }
        if (mMessageCursor != null) {
            mMessageCursor.close();
            mMessageCursor = null;
        }
        if (mInboxCursor != null) {
            mInboxCursor.close();
            mInboxCursor = null;
        }
    }

    // NeighborUpdatable
    public void requeryNeighbors() {
        if (neighborList != null) {
        synchronized(neighborList) {
            neighborList.clear();
            if (dtn != null) {
                // can be null at some time.
                neighborList.addAll(dtn.getNodes());
                //System.out.println("Length=" + neighborList.size());
            }
        }
        }
    }

    private void restart() {
        cleanup();
        Intent newIntent = new Intent();
        newIntent.setClass(activity, activity.getClass());
        //intent.setFlags(Intent.FLAG_ACTIVITY_);
        startActivity(newIntent);
        activity.finish();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        // check whether replacing the running peer is required or not.
        String curUserName = sp.getString(Setting.USER_NAME_KEY, "");
        String curPeerIdString = sp.getString(Setting.PEER_ID_KEY, "");

        boolean curEnableSynchronizer = sp.getBoolean(Setting.SYNCHRONIZER_ENABLE_KEY, false);

        boolean curUseBluetooth = sp.getBoolean(Setting.USE_BLUETOOTH_KEY, false);
        boolean curUseWifi = sp.getBoolean(Setting.USE_WIFI_KEY, false);
        boolean curUseNFC = sp.getBoolean(Setting.USE_NFC_KEY, true);
        
        boolean curDiscoverable = sp.getBoolean(Setting.PERIODIC_BLUETOOTH_KEY, false);

        if (BluetoothTSD.ALWAYS_DISCOVERABLE != curDiscoverable) {
            BluetoothTSD.ALWAYS_DISCOVERABLE = curDiscoverable;
        }
        if (enableSynchronizer != curEnableSynchronizer) {
            if (msync != null) {
                msync.stop();
            }
            if (curEnableSynchronizer) {
                enableSynchronizer = true;
                msync = new MessageSynchronizer(dtn.getDB());
                //msync.start();
            }
        }
        
        if ((curUseBluetooth != useBluetooth) ||
            (curUseWifi != useWifi) ||
            (curUseNFC != useNFC)) {
            restart();
        }

        if (curUserName.equals("")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(getString(R.string.prompt_user_name))
                .setTitle(getString(R.string.app_name))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            setting = new Intent(activity, Setting.class);
                            setting.putExtra(KEY_SETTING_ACTION, "userName");
                            startActivityForResult(setting, ACTIVITY_CREATE);
                        }
                    })
                .setCancelable(false);
            builder.show();
            return;
        }

        if (!userName.equals(curUserName)) {
            TextView tv = (TextView) findViewById(R.id.tt);
            if (tabs.getCurrentTabTag().equals("tab1")) {
                tv.setText(getString(R.string.pub) + " @" + userName);
            }
            if (tabs.getCurrentTabTag().equals("tab2")) {
                tv.setText(getString(R.string.inbox) + " @" + userName);
            }
            if (tabs.getCurrentTabTag().equals("tab3")) {
                tv.setText(getString(R.string.neighbors) + " @" + userName);
            }
        }

        // user name is changed or peer id is changed...
        // complete restart.
        if (!peerIdString.equals(curPeerIdString)) {
            // reset all and obtain new certificate.
            SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(this).edit();
            prefEdit.putString(Setting.USER_NAME_KEY, "");
            prefEdit.putString(Setting.USER_PUBLIC_KEY, "");
            prefEdit.putString(Setting.PEER_ID_KEY, "");
            prefEdit.commit();
            restart();           
        }

        if (intent != null) {
            Bundle extras = intent.getExtras();
            switch(requestCode) {
            case ACTIVITY_CREATE:
                String action = extras.getString(KEY_DRAFT_ACTION);
                if (action != null) {
                    draftText = extras.getString(KEY_DRAFT_TEXT);
                    
                    if (action.equals("send")) {
                        MessageData mes = (MessageData)extras.getSerializable(KEY_DRAFT_MESSAGE);
                        String tweet = (String)extras.getString(KEY_DRAFT_TWEET_STATUS);
                        // XXX must search most appropriate token.
                        // id
                        mes.status = "tweet=" + tweet;
                        
                        VONEntry entry = null;
                        if (dtn == null) {
                         //   alert(getString(R.string.internal_error) + "\nNo DTN exists");
                        }
                        else {
                            if (dtn.getSecurityManager() != null) {
                                entry = ((MessageSecurityManager)dtn.getSecurityManager()).getLatestVONEntry();
                            }
                            else {
                                //alert(getString(R.string.internal_error) + "\nDTN is not started");
                            }
                        }
                        if (entry == null) {
                            alert(getString(R.string.authentication_error));
                        }
                        else {
                            dtn.newMessage(mes);
                            draftText = "";
                        }
                    }
                    else {
                    }
                }
            }
        }
        else {
        }
        /// XXX check curVONEntries and vonEntries
    }
    
    @Override
    public void onNewIntent(Intent intent) {
        if (nfc != null) {
            nfc.onNewIntent(intent);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (nfc != null) {
            nfc.onPause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public void onResume() {
        super.onResume();
        if (nfc != null) {
            nfc.setActivity(this);
            nfc.onResume();
        }
        Uri uri=getIntent().getData();
        if (uri!=null && uri.toString().startsWith(CALLBACKURL)) {
            //System.out.println("URI=" + uri.toString());
        }
    }

    private void generateAndSaveKeyPair() {
        List<String> pair = VON.generateKeyPair();
        userPublicKey = pair.get(0);
        userPrivateKey = pair.get(1);
        SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(this).edit();
        prefEdit.putString(Setting.USER_PUBLIC_KEY, userPublicKey);
        prefEdit.putString(Setting.USER_PRIVATE_KEY, userPrivateKey);
        prefEdit.commit();
    }
    
    public void onStart() {
        super.onStart();
        if (userName.equals("")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(getString(R.string.prompt_user_name))
                .setTitle(R.string.app_name)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            setting = new Intent(activity, Setting.class);
                            setting.putExtra(KEY_SETTING_ACTION, "userName");
                            startActivityForResult(setting, ACTIVITY_CREATE);
                        }
                    });
            builder.show();
        }
        else {
            TextView tv = (TextView) findViewById(R.id.tt);
            tv.setText(getString(R.string.pub) + " @" + userName);
            tv.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            //startDTN();
            //setupAdapters();
            confirmAndStartDTN();
            // XXX IS IT OK?
            requeryNeighbors();
            if (mNeighborAdapter != null) {
                mNeighborAdapter.notifyDataSetChanged();
            }
        }
    }

    private void getVONKeyAndToken(String uid, String pid, String vid, String publicKey, String tokenKey) {

        HttpClient client = new DefaultHttpClient();
        if (uid == null || pid == null || vid == null || publicKey == null || tokenKey == null) {
            alert(getString(R.string.authentication_error));
        }
        // Should it be HTTP/1.1?
        //  client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpPost post = new HttpPost(Monac.GATEWAY_URL + PATH_AUTHORIZE);
        //HttpPost post = new HttpPost("https://ec2-175-41-227-130.ap-northeast-1.compute.amazonaws.com" + PATH_AUTHORIZE);
        MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        try {
            try {
                entity.addPart("public_key", new StringBody(publicKey, "text/plain",
                        Charset.forName("UTF-8")));
                entity.addPart("vid", new StringBody(vid, "text/plain",
                        Charset.forName("UTF-8")));
                entity.addPart("pid", new StringBody(pid, "text/plain",
                        Charset.forName("UTF-8")));
                entity.addPart("uid", new StringBody(uid, "text/plain",
                        Charset.forName("UTF-8")));
                entity.addPart("token_key", new StringBody(tokenKey, "text/plain",
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
            JSONObject obj;
            if (response != null) {
                obj = new JSONObject(response);
                String von_key = obj.getString("von_key");
                //System.out.println("VON_KEY=" + von_key);
                //System.out.println("TOKEN=" + obj.get("token"));
                AccessToken token = AccessToken.parse(obj.getString("token"));
                String ovid = token.tokenAttributes.get("von_id");
                String user_id = token.getAttribute("user_id");
                Date expiresAt = token.tokenExpires;
                HashMap<String,String> smap = new HashMap<String,String>();
                smap.put("von_key", von_key);
                if (token.isValidToken(Monac.AUTHORITY_PUBLIC_KEY_STRING, smap)) {
                    
                  ///AccessToken cert = this.testToken(peerIdString, "teranisi", userPublicKey);
                    VON von = new ManagedVON(AUTHORITY_PUBLIC_KEY_STRING, token, von_key);
                    // XXX NULL!!!
                    // dtn.getVONManager().addVON(von);
                    alert(user_id + " " + getString(R.string.authentication_success) + "\nExpires: " + expiresAt);
                    String jsonStr = null;
                    if (dtn == null) {
                        // This means bootstrap.
                        VONEntry entry = new VONEntry(von.vonId(), von);
                        JSONObject o = entry.toJSONObject();
                        JSONArray array = new JSONArray();
                        array.put(o);
                        jsonStr = array.toString();
                        //System.out.println("NEW VONENTRIES=" + jsonStr);
                    }
                    else {
                        // This means update while running.
                        MessageSecurityManager mgr = (MessageSecurityManager)dtn.getSecurityManager();
                        mgr.addVON(von);
                        jsonStr = mgr.toString();
                        //System.out.println("SAVE VONENTRIES=" + jsonStr);

                    }
                    userName = user_id;
                    SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(this).edit();
                    prefEdit.putString(Setting.USER_NAME_KEY, user_id);
                    prefEdit.putString(Setting.VON_ENTRIES_KEY, jsonStr);
                    prefEdit.commit();
                }
                else {
                    alert(getString(R.string.authentication_error));
                }
            }
            if (response == null) {
                alert(getString(R.string.authentication_error));
            }
            //updateManagedVON(ovid, von_key, token.toString(), token.tokenExpires);
        } catch (JSONException e) {
            alert(getString(R.string.authentication_error));
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // client.getConnectionManager().shutdown();        
    }
    
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.tabbed_main);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        useNFC =  PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Setting.USE_NFC_KEY, true);
        if (useNFC) {
            if (nfc != null) {
                nfc.setActivity(this);
            }
            else {
                nfc = Nfc.getInstance("Android");
                if (nfc != null) {
                    nfc.setActivity(this);
                    nfc.setupNotificationResource(R.raw.touched);
                }
            }
        }
        else {
            nfc = null;
        }
        handler = new Handler();
        lastNotified = null;

        View v = findViewById(R.id.tt);
        TextView tt = (TextView) v; 
        tt.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    
                    if (tabs.getCurrentTabTag().equals("tab1")) {
                        ListView pList = (ListView) activity.findViewById(R.id.list1);
                        pList.setSelection(0);
                    }
                    if (tabs.getCurrentTabTag().equals("tab2")) {
                        ListView iList = (ListView) activity.findViewById(R.id.list2);
                        iList.setSelection(0);
                    }
                }
            });

        //        mDbHelper = new AndroidMessageDB(this);
        //        mDbHelper.open();

        dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // format is too slow at first time.
        //        dateFormatter.format(new Date());
        publicCount = 0;

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        enableSynchronizer = sp.getBoolean(Setting.SYNCHRONIZER_ENABLE_KEY, false);
        userName = sp.getString(Setting.USER_NAME_KEY, "");
        activity = this;
        
        tabs = getTabHost();
        tabs.setup();
        TabHost.OnTabChangeListener tabListener = new TabHost.OnTabChangeListener() {
                public void onTabChanged(String tabId) {
                    TextView tv = (TextView) findViewById(R.id.tt);
                    if (tabId.equals("tab1")) {
                        if (read_item != null) {
                            read_item.setEnabled(true);
                        }
                        tv.setText(getString(R.string.pub) + " @" + userName);
                    }
                    else if (tabId.equals("tab2")) {
                        if (read_item != null) {
                            read_item.setEnabled(true);
                        }
                        tv.setText(getString(R.string.inbox) + " @" + userName);
                    }
                    else {
                        if (read_item != null) {
                            read_item.setEnabled(false);
                        }
                        tv.setText(getString(R.string.neighbors) + " @" + userName);
                    }
                }
            };

        publicTab= tabs.newTabSpec("tab1");
        tabs.setOnTabChangedListener(tabListener);
        publicTab.setContent(R.id.content1);
        publicTab.setIndicator(getString(R.string.pub),
                               getResources().getDrawable(R.drawable.tb_text));
        tabs.addTab(publicTab);

        inboxTab= tabs.newTabSpec("tab2");
        inboxTab.setContent(R.id.content2);
        inboxTab.setIndicator(getString(R.string.inbox)
                              ,getResources().getDrawable(R.drawable.tb_inbox));
        tabs.addTab(inboxTab);

        TabHost.TabSpec tab3= tabs.newTabSpec("tab3");
        tab3.setContent(R.id.content3);
        tab3.setIndicator(getString(R.string.neighbors),
                          getResources().getDrawable(R.drawable.tb_neighbor));
        tabs.addTab(tab3);
        
        tabs.setCurrentTab(0);

        //        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar);
        //        TextView tv = (TextView) findViewById(R.id.title_text);
        //        tv.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        draft = new Intent(this, Draft.class);

        
        // Callback
        //
        // "?token_key=" + token_key + "&vid=" + latest.vonId + "&uid=" + vu.uid + (pid != null ? "&pid=" + pid : ""),
        Uri uri=getIntent().getData();
        if (uri!=null && uri.toString().startsWith(CALLBACKURL)) {
            //System.out.println("onCreate URI=" + uri.toString());
            String tokenKey=uri.getQueryParameter("token_key");
            if (tokenKey != null) {
                String uid = uri.getQueryParameter("uid");
                String pid = uri.getQueryParameter("pid");
                String vid = uri.getQueryParameter("vid");
                //(String uid, String pid, String vid, String publicKey, String tokenKey)
                getVONKeyAndToken(uid, pid, vid, userPublicKey, tokenKey);
            }
        }
    }

    private void updateButtons() {
        if (draft_item != null) {
            draft_item.setEnabled(true);
        }
        if (read_item != null) {
            read_item.setEnabled(true);
        }
    }

    private void confirmAndStartDTN () {
        if (dtn != null) {
            // DTN already running.
            setupAdapters();
            dtn.clearReceiveListeners();
            //dtn.clearNodeStateListeners();
            dtn.addReceiveListener(this);
            // Is this needed??
            //dtn.addTask(new NodeStateUpdator());
            //dtn.addNodeStateListener(this);
        }
        else {
            boolean ask_data_ex = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Setting.ASK_DATA_EX_KEY, true);
            if (ask_data_ex == false) {
                startDTN();
                setupAdapters();
                updateButtons();
            }
            else {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setMessage(getString(R.string.ask))
                    .setTitle(R.string.app_name)
                    .setPositiveButton(getString(R.string.allow), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                startDTN();
                                setupAdapters();
                                updateButtons();
                            }
                        })
                    .setNegativeButton(getString(R.string.dont_allow), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                    .setCancelable(true);
                builder.show();
            }
        }
    }
    
    public void updateInboxTab() {
        String str = getString(R.string.inbox) + (inboxCount == 0? "" : " (" + inboxCount + ")");
        ViewGroup vg = (ViewGroup)getTabWidget().getChildAt(1);
        if (vg != null) {
            TextView v = (TextView) vg.getChildAt(1);
            if (v != null) {
                v.setText(str);
                v.invalidate();
            }
        }         
    }

    public void updatePublicTab() {
        String str = getString(R.string.pub) + (publicCount == 0? "" : " (" + publicCount + ")");
        ViewGroup vg = (ViewGroup)getTabWidget().getChildAt(0);
        if (vg != null) {
            TextView v = (TextView) vg.getChildAt(1);
            if (v != null) {
                v.setText(str);
                v.invalidate();
            }
        } 
    }

    private void setupTimelineAdapters() {
        //System.out.println("###setup timeline adapters on " + this);
        mMessageCursor = ((AndroidMessageDB)dtn.getDB()).searchWithLimit(PUBLIC_TL_LIMIT, getVONEntries());
        startManagingCursor(mMessageCursor);
        mMessageAdapter
            = new MessageCursorAdapter(this,
                                       R.layout.message_row,
                                       mMessageCursor,
                                       new String[] {
                                           MessageData.KEY_SCREEN_NAME,
                                           MessageData.KEY_STATUS,
                                           MessageData.KEY_TEXT,
                                           MessageData.KEY_NEW_FLAG,
                                           MessageData.KEY_CREATED_AT
                                           //AndroidMessageDB.KEY_SOURCE_ID
                                       },
                                       new int[] { R.id.toptext,
                                                   R.id.twitter_icon,
                                                   R.id.bottomtext,
                                                   R.id.new_icon,
                                                   R.id.footertext
                                                   //R.id.mes_back
                                       },
                                       peerIdString,
                                       (MessageSecurityManager)dtn.getSecurityManager()
                                       );
        //mMessageAdapter.setVONManager(dtn.getVONManager());

        mInboxCursor =  ((AndroidMessageDB)dtn.getDB()).searchWithRecipient(peerIdString, getVONEntries());
        startManagingCursor(mInboxCursor);
        mInboxAdapter
            = new MessageCursorAdapter(this,
                                       R.layout.message_row,
                                       mInboxCursor,
                                       new String[] {
                                           MessageData.KEY_SCREEN_NAME,
                                           MessageData.KEY_STATUS,
                                           MessageData.KEY_TEXT,
                                           MessageData.KEY_NEW_FLAG,
                                           MessageData.KEY_CREATED_AT
                                       },
                                       new int[] { R.id.toptext,
                                                   R.id.twitter_icon,
                                                   R.id.bottomtext,
                                                   R.id.new_icon,
                                                   R.id.footertext
                                       },
                                       peerIdString,
                                       (MessageSecurityManager)dtn.getSecurityManager());
        
        pList = (ListView) this.findViewById(R.id.list1);
        pList.setAdapter(mMessageAdapter);
        pList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    //Debug.startMethodTracing("/data/data/com.idovatter/show-content");
                    Cursor c = mMessageCursor;
                    c.moveToPosition(position);
                    mSelectedMessage = AndroidMessageDB.messageByCursor(c);
                    if (mSelectedMessage.isNew) {
                        dtn.getDB().markAsRead(mSelectedMessage.row_id);
                        mMessageCursor.requery();
                        mMessageAdapter.notifyDataSetChanged();
                        if (publicCount > 0) {
                            publicCount--;
                        }
                        updatePublicTab();
//                         if (isInboxMessage(mSelectedMessage)) {
//                             if (inboxCount > 0) {
//                                 inboxCount--;
//                             }
//                             mInboxCursor.requery();
//                             mInboxAdapter.notifyDataSetChanged();
//                             updateInboxTab();
//                         }
                    }
                    showMessageDialog();
                }
            });

        publicCount = ((AndroidMessageDB)dtn.getDB()).countUnread(PUBLIC_TL_LIMIT, getVONEntries());
        updatePublicTab();

        ListView iList = (ListView) this.findViewById(R.id.list2);
        iList.setAdapter(mInboxAdapter);
        iList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Cursor c = mInboxCursor;
                    c.moveToPosition(position);
                    mSelectedMessage = AndroidMessageDB.messageByCursor(c);
                    //System.out.println("MES=" + mSelectedMessage.getJson());
                    if (mSelectedMessage.isNew) {
                        dtn.getDB().markAsRead(mSelectedMessage.row_id);
                        c.requery();
                        mInboxAdapter.notifyDataSetChanged(); 
                        if (inboxCount > 0) {
                            inboxCount--;
                        }
                        updateInboxTab();
//                         if (isPublicMessage(mSelectedMessage)) {
//                             mMessageCursor.requery();
//                             mMessageAdapter.notifyDataSetChanged();
//                             if (publicCount > 0) {
//                                 publicCount--;
//                             }
//                             updatePublicTab();
//                         }
                    }
                    showMessageDialog();
                }
            });
        inboxCount =  ((AndroidMessageDB)dtn.getDB()).countUnread(peerIdString, getVONEntries());
        updateInboxTab();        
    }

    public void setupAdapters() {
        setupTimelineAdapters();

        neighborList = new ArrayList<Peer>();
        nList = (ListView) this.findViewById(R.id.list3);
        mNeighborAdapter
            = new NeighborAdapter(this,
                                  R.layout.node_row,
                                  neighborList);
        nList.setAdapter(mNeighborAdapter);
        requeryNeighbors();
        //onNodeStateChange();
    }

    private void showMessageDialog() {
        MessageDialog dialog = new MessageDialog(this, mSelectedMessage, this);
        dialog.show();
    }

    public void draftForReply(MessageData mes) {
        clearDraftExtras();
        draft.putExtra(KEY_DRAFT_SOURCE_ID, peerIdString);
        draft.putExtra(KEY_DRAFT_SCREEN_NAME, userName);
        draft.putExtra(KEY_DRAFT_TEXT, "@" + mes.screen_name + " ");
        draft.putExtra(KEY_DRAFT_IN_REPLY_TO, mes.id);
        draft.putExtra(KEY_DRAFT_IN_REPLY_TO_ID, mes.source_id);
        draft.putExtra(KEY_DRAFT_IN_REPLY_TO_SCREEN_NAME, mes.screen_name);
        draft.putExtra(KEY_DRAFT_ACTION, "reply");
        startActivityForResult(draft, ACTIVITY_CREATE);        
    }

    public void draftForCiteReply(MessageData mes) {
        clearDraftExtras();
        draft.putExtra(KEY_DRAFT_SOURCE_ID, peerIdString);
        draft.putExtra(KEY_DRAFT_SCREEN_NAME, userName);
        draft.putExtra(KEY_DRAFT_TEXT, " \"@" + mes.screen_name + " " + ((MessageSecurityManager)dtn.getSecurityManager()).getText(mes) + "\"");
        draft.putExtra(KEY_DRAFT_IN_REPLY_TO, mes.id);
        draft.putExtra(KEY_DRAFT_IN_REPLY_TO_ID, mes.source_id);
        draft.putExtra(KEY_DRAFT_IN_REPLY_TO_SCREEN_NAME, mes.screen_name);
        draft.putExtra(KEY_DRAFT_ACTION, "citeReply");
        startActivityForResult(draft, ACTIVITY_CREATE);        
    }
    
    private void alert(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(message)
        .setTitle(getString(R.string.app_name))
        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        })
        .setCancelable(true);
        builder.show();
    }

    public void draftForDirect(MessageData mes) {
        clearDraftExtras();
        Message m = ((MessageSecurityManager)dtn.getSecurityManager()).decapsulate(mes);
        if (m.senderPublicKey != null) {
            if (m.senderPublicKeyExpiresAt.after(new Date())) {
                dtn.setPublicKey(new PeerId(mes.source_id), m.senderPublicKey, m.senderPublicKeyExpiresAt);
            }
            else {
                alert(getString(R.string.error_no_pubkey));
            }
        }
        
        draft.putExtra(KEY_DRAFT_SOURCE_ID, peerIdString);
        draft.putExtra(KEY_DRAFT_SCREEN_NAME, userName);
        draft.putExtra(KEY_DRAFT_TEXT, "D " + mes.screen_name + " ");
        draft.putExtra(KEY_DRAFT_RECIPIENT_ID, mes.source_id);
        draft.putExtra(KEY_DRAFT_RECIPIENT_SCREEN_NAME, mes.screen_name);
        draft.putExtra(KEY_DRAFT_ACTION, "direct");
        
        
        startActivityForResult(draft, ACTIVITY_CREATE);        
    }

    private void clearDraftExtras() {
        draft.putExtra(KEY_DRAFT_SOURCE_ID, (String)null);
        draft.putExtra(KEY_DRAFT_SCREEN_NAME, (String)null);
        draft.putExtra(KEY_DRAFT_TEXT, (String)null);
        draft.putExtra(KEY_DRAFT_IN_REPLY_TO, (String)null);
        draft.putExtra(KEY_DRAFT_IN_REPLY_TO_ID, (String)null);
        draft.putExtra(KEY_DRAFT_IN_REPLY_TO_SCREEN_NAME, (String)null);
        draft.putExtra(KEY_DRAFT_ACTION, (String)null);
        draft.putExtra(KEY_DRAFT_RECIPIENT_ID, (String)null);
        draft.putExtra(KEY_DRAFT_RECIPIENT_SCREEN_NAME, (String)null);
    }

    public void startDTN() {
        // Android DTN
        Map<Integer, Object> params = new HashMap<Integer, Object>();
        params.put(AndroidDTN.ANDROID_CONTEXT, this);
        params.put(AndroidDTN.TSD_ENABLE_MULTICAST, Boolean.FALSE);
        params.put(AndroidDTN.TSD_ENABLE_BROADCAST, Boolean.FALSE);
        useBluetooth = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Setting.USE_BLUETOOTH_KEY, true);
        params.put(AndroidDTN.TSD_ENABLE_BLUETOOTH, useBluetooth); 
        params.put(AndroidDTN.USE_BLUETOOTH, useBluetooth); 
        
        useWifi = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Setting.USE_WIFI_KEY, true);
        params.put(AndroidDTN.USE_WIFI, useWifi);
        params.put(AndroidDTN.TSD_ENABLE_MULTICAST, useWifi);

        // XXX NFC is not implemented yet.
               
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        
        peerIdString = sp.getString(Setting.PEER_ID_KEY, "");
        userName = sp.getString(Setting.USER_NAME_KEY, "");

        userPublicKey = sp.getString(Setting.USER_PUBLIC_KEY, "");
        userPrivateKey = sp.getString(Setting.USER_PRIVATE_KEY, "");
        
        if (userPublicKey.equals("")) {
            // this is the first time.
            generateAndSaveKeyPair();
            Log.d("monac","****GENERATED KEY PAIR!!****");
        }
        else {
            Log.d("monac","****USING EXISTING KEY PAIR!!****");
        }
        
        if (peerIdString.equals("")) {
            peerId = PeerId.newId();
            peerIdString = peerId.toString();
            SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(this).edit();
            prefEdit.putString(Setting.PEER_ID_KEY, peerIdString);
            prefEdit.commit();
        }
        else {
            peerId = new PeerId(peerIdString);
        }
        String vonEntries = sp.getString(Setting.VON_ENTRIES_KEY, null);
        
        MessageSecurityManager smgr = new MessageSecurityManager(this.peerIdString, AUTHORITY_PUBLIC_KEY_STRING, vonEntries);
        smgr.setupKeyPair(userPublicKey, userPrivateKey);

        params.put(AndroidDTN.PEER_ID, peerId);
        params.put(AndroidDTN.PEER_NAME, userName);
        params.put(Overlay.SERVICE_ID, smgr.getLatestVONEntry().vonId);
        params.put(Overlay.SECURITY_MANAGER, smgr);
        params.put(AndroidDTN.ALGORITHM, new EpidemicBroadcast());
        
        dtn = DTN.getInstance("AndroidDTN", params);
        dtn.clearReceiveListeners();
        //dtn.clearNodeStateListeners();
        dtn.addReceiveListener(this);
        //dtn.addNodeStateListener(this);
        
        dtn.start();
        dtn.addTask(new NodeStateUpdator());        
        if (enableSynchronizer) {
            msync = new MessageSynchronizer(dtn.getDB());
        }
    }
    
    public void updateTimelines() {
        if (mMessageCursor != null) {
            mMessageCursor.close();
            mMessageCursor = null;
        }
        if (mInboxCursor != null) {
            mInboxCursor.close();
            mInboxCursor = null;
        }
        if (dtn != null) {
            setupTimelineAdapters();
        }
    }

    
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        //        if (mMessageCursor != null)
        //            mMessageCursor.close();
        //        if (mInboxCursor != null)
        //            mInboxCursor.close();
    }

    class NodeStateUpdator implements Runnable {
        
        private void updateDiscoverable() {
            ImageView dicon = (ImageView)findViewById(R.id.discoverable_icon);
            if (useBluetooth) {
                dicon.setVisibility(View.VISIBLE); 
                if (BluetoothTSD.isDiscoverable()) {
                    dicon.setImageResource(R.drawable.bt_c);
                }
                else {
                    dicon.setImageResource(R.drawable.bt_u);
                }
            }
            else {
                dicon.setVisibility(View.GONE); 
            }
        }
        
        public void run() {
            if (mNeighborAdapter != null) {
                handler.post(new Runnable() {
                    public void run() {
                        updateDiscoverable();
                        requeryNeighbors();
                        mNeighborAdapter.notifyDataSetChanged();
                    }
                });
            }
        }
    }

    boolean isPublicMessage(MessageData mes) {
        return ((AndroidMessageDB)dtn.getDB()).memberOfAll(PUBLIC_TL_LIMIT, mes.row_id);
    }

    boolean isInboxMessage(MessageData mes) {
        // direct to myself;
        if (mes.recipient_id != null) {
            if (mes.recipient_id.equals(peerIdString)) {
                return true;
            }
        }
        // direct from myself;
        if (mes.recipient_id != null) {
            if (mes.source_id.equals(peerIdString)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void onReceive(Target target, Serializable payload) {
        Message m = (Message) payload;
        Date now = new Date();
        if (lastNotified == null ||
            (now.getTime() - lastNotified.getTime() > 5000)) {
            MediaPlayer mp = MediaPlayer.create(this, R.raw.ping);
            mp.setOnPreparedListener(this);
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(300);
            
            lastNotified = now;
        }
        if (isInboxMessage(m.data)) {
            inboxCount++;
        }
        else {
            publicCount++;
        }
        handler.post(new Runnable() {
                public void run() {
                    //System.out.println("### requery and update on " + activity);
                    mMessageCursor.requery();
                    mInboxCursor.requery();
                    mMessageAdapter.notifyDataSetChanged();
                    mInboxAdapter.notifyDataSetChanged();
                    updatePublicTab();
                    updateInboxTab();
                }
            });
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }

    
}