package org.piax.trans.ts.nfc.nfc.android;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.tech.NfcF;
import android.os.Parcelable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.piax.trans.ts.nfc.nfc.Nfc;
import org.piax.trans.ts.nfc.nfc.NfcReceiver;

/**
 * An example of how to use the NFC foreground NDEF push APIs.
 */
public class AndroidNfc extends Nfc implements OnPreparedListener {
    //private NfcAdapter mAdapter;
    private NdefMessage mMessage;
    
    private boolean USE_GZIP = false;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mFilters;
    private String[][] mTechLists;
    private Activity activity;
    private NfcReceiver nr;
    private int resid = -1;

    public AndroidNfc() {
        activity = null;
        nr = null;
        mMessage = null;
    }
    
    @Override
    public void setActivity(Activity activity) {
        this.activity = activity;
    }
    
    @Override
    public void setupReceiver(NfcReceiver nr) {
        this.nr = nr; 
    }
    

    @Override
    public void setupNotificationResource(int resid) {
        this.resid = resid;
    }
    
    @Override
    public void send(byte[] data) {
        try {
            byte[] packed;
            if (USE_GZIP) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GZIPOutputStream gos = new GZIPOutputStream(baos);
                gos.write(data);
                gos.close();
                packed = baos.toByteArray();
            }
            else {
                packed = data;
            }
            NdefRecord nr = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
                    "application/monac".getBytes(),
                    new byte[0], packed);
            mMessage = new NdefMessage(new NdefRecord[] { nr });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (resid != -1) {
            MediaPlayer mp = MediaPlayer.create(activity, resid);
            mp.setOnPreparedListener(this);
        }
        
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMsgs == null || rawMsgs.length == 0) {
            return;
        }
        NdefMessage[] ndefMessages = new NdefMessage[rawMsgs.length];
        for (int i = 0; i < rawMsgs.length; i++) {
            ndefMessages[i] = (NdefMessage)rawMsgs[i];
        }
        doHandleNdef(ndefMessages);
    }

    private synchronized void doHandleNdef(NdefMessage[] ndefMessages) {
        byte[] data = null;
        for (NdefMessage mes : ndefMessages) {
            NdefRecord[] records = mes.getRecords();
            for (NdefRecord record : records) {
                data = record.getPayload();
            }
        }
        if (data != null && nr != null) {
            //System.out.println("RECEIVED length=" + data.length);
            byte[] extracted;
            if (USE_GZIP) {
                try {
                    ByteArrayInputStream bais = new ByteArrayInputStream(data);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    GZIPInputStream gis = new GZIPInputStream(bais);
                    for (;;) {
                        int b = gis.read();
                        if (b < 0) break;
                        bos.write(b);
                    }
                    extracted = bos.toByteArray();
                }
                catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
            else {
                extracted = data;
            }
            //System.out.println("EXTRACTED length=" + extracted.length);
            ByteBuffer bb = ByteBuffer.wrap(new byte[extracted.length]);
            bb.position(0);
            bb.mark();

            bb.put(extracted);
            
            // flip
            bb.limit(bb.position());
            bb.reset();
            
            //ByteBufferUtil.flip(bb);
            nr.receive(bb);
        }
    }
    
    @Override
    public void onResume() {
        NfcAdapter mAdapter = NfcAdapter.getDefaultAdapter(activity);
        mPendingIntent = PendingIntent.getActivity(activity, 0,
                new Intent(activity, activity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        // Setup an intent filter for all MIME type.
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            //ndef.addDataType("*/*");
            ndef.addDataType("application/monac");
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        mFilters = new IntentFilter[] {
                ndef,
        };
        mTechLists = new String[][] { new String[] { NfcF.class.getName() } };
        if (mAdapter != null) {
            if (mMessage != null) {
                mAdapter.enableForegroundNdefPush(activity, mMessage);
            }
            mAdapter.enableForegroundDispatch(activity, mPendingIntent, mFilters, mTechLists);
        }
    }

    @Override
    public void onPause() {
        NfcAdapter mAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (mAdapter != null) {
            mAdapter.disableForegroundNdefPush(activity);
            mAdapter.disableForegroundDispatch(activity);
        }
    }
    
    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }
}
