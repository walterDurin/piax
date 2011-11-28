package jp.android_group.payforward.monac;

import java.util.Date;
import java.util.List;

import jp.android_group.payforward.monac.R;

import org.piax.trans.Peer;
import org.piax.trans.stat.LocatorStat;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class NeighborAdapter extends ArrayAdapter {  
  
    private List items;  
    private LayoutInflater inflater;
    private Context ctxt;
  
    public NeighborAdapter(Context context, int textViewResourceId,  
                           List items) {  
        super(context, textViewResourceId, items);  
        this.items = items;  
        this.ctxt = context;
        this.inflater = (LayoutInflater) context  
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);  
    }  
    
    private String agoExpression(Date date) {
        long sec = (new Date().getTime() - date.getTime()) / 1000;
        long days = sec / 86400;
        if (days > 0) {
            return days + " day(s)";
        }
        else {
            long hours = sec / 3600;
            if (hours > 0) {
                return hours + " hour(s)";
            }
            else {
                long mins = sec / 60;
                if (mins > 0) {
                    return mins + " minute(s)"; 
                }
            }
        }
        return sec + " second(s)";
    }
    
  
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;  
        if (view == null) {  
            view = inflater.inflate(R.layout.node_row, null);  
            view.setBackgroundResource(R.drawable.back);  
        }  
        Peer node = (Peer)items.get(position);  
        if (node != null) {  
            TextView screenName = (TextView)view.findViewById(R.id.ntoptext);  
            screenName.setTypeface(Typeface.DEFAULT_BOLD);  
            TextView text = (TextView)view.findViewById(R.id.nbottomtext);  
            ImageView iv = (ImageView)view.findViewById(R.id.stat_icon);
            ImageView wv = (ImageView)view.findViewById(R.id.wifi_icon);
            ImageView bt = (ImageView)view.findViewById(R.id.bt_icon);
            ImageView nfc = (ImageView)view.findViewById(R.id.nfc_icon);
            if (screenName != null) {
                if (node.name == null || node.name.length() == 0) {
                    screenName.setText(ctxt.getString(R.string.no_name));
                }
                else {
                    screenName.setText(node.name);
                }
            }  
            if (text != null) {
                String ago = agoExpression(node.lastSeen) + " ago";
//                SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                String ago = dateFormatter.format(node.lastSeen) + "";
                bt.setVisibility(View.GONE);
                wv.setVisibility(View.GONE);
                nfc.setVisibility(View.GONE);
                if (node.lstats != null) {
                    for (LocatorStat lstat : node.lstats) {
                        if (lstat.locator instanceof org.piax.trans.ts.nfc.NfcLocator) {
                            nfc.setVisibility(View.VISIBLE);
                        }
                        if (lstat.locator instanceof org.piax.trans.ts.bluetooth.BluetoothLocator) {
                            bt.setVisibility(View.VISIBLE);
                            if (lstat.status >= Peer.TransportStateCommunicatable) {
                                bt.setImageResource(R.drawable.bt_c);
                            }
                            else if (lstat.status >= Peer.TransportStateAlive) {
                                bt.setImageResource(R.drawable.bt_a);
                            }
                            else {
                                bt.setImageResource(R.drawable.bt_u);
                            }
                        }
                        if (lstat.locator instanceof org.piax.trans.ts.InetLocator) {
                            wv.setVisibility(View.VISIBLE);
                            if (lstat.status >= Peer.TransportStateCommunicatable) {
                                wv.setImageResource(R.drawable.wifi_c);
                            }
                            else if (lstat.status >= Peer.TransportStateAlive) {
                                wv.setImageResource(R.drawable.wifi_a);
                            }
                            else {
                                wv.setImageResource(R.drawable.wifi_u);
                            }
                        }


                    }
                }
                //String version = node.version;
                /* for debugging
                if (node.status == PeerInfo.TransportStateLinked) {
                    ago = "C " + ago;
                }
                else if (node.status == PeerInfo.TransportStateAccepted) {
                    ago = "S " + ago;
                }
                */
                //System.out.println("node.status=" + node.getStatus());
                text.setText(ago);
            }
            if (node.getStatus() >= Peer.TransportStateCommunicatable) {
                iv.setImageResource(R.drawable.available);
            }
            else if (node.getStatus() == Peer.TransportStateUnavailable) {
                iv.setImageResource(R.drawable.unavailable);
            }
            else {
                iv.setImageResource(R.drawable.disconnected);
            }
        }  
        return view;  
    }  
}