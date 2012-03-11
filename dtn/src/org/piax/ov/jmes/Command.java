package org.piax.ov.jmes;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.piax.gnt.SecurityManager;

public class Command {

    public static final String VERSION = "2.0";
    public static final String MESSAGE_TYPE = "m";
    public static final String PLAIN_TYPE = "p";

    public String version;
    public String commandString;
    public String type;
    public String senderId;
    public Object argument; // whatever JSON can include

    public Command(String commandString, String type, String senderId, Object argument){
        this(VERSION, commandString, type, senderId, argument);
    }
    
    public Command(String version, String commandString, String type, String senderId, Object argument){
        this.version = version;
        this.commandString = commandString;
        this.type = type;
        this.senderId = senderId;
        this.argument = argument;
    }
    
    private boolean versionLessThan(String ver1, String ver2) {
        return Float.parseFloat(ver1) < Float.parseFloat(ver2);
    }

    protected String[] commandVersion(JSONArray obj) {
        try {
            return ((String)obj.get(0)).split("/");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Command(SecurityManager smgr, byte[] src) {
        try {
            JSONArray obj = new JSONArray(new String(src));
            String[] cv = commandVersion(obj);
            version = cv.length == 2 ? cv[1] : "1.0";
            
            if (versionLessThan(version, VERSION)) {
                throw new ProtocolCompatibilityException("Version is not compatible.");
            }
            commandString = cv[0];
            type = (String) obj.get(1);
            senderId = (String) obj.get(2);
            if (MESSAGE_TYPE.equals(type)) {
                argument = unwrapMessageArgument(smgr, obj.get(3));
            }
            else {
                argument = obj.get(3);
            }
        } catch (JSONException e) {
            throw new ProtocolCompatibilityException(e);
        }
    }
    
    private Object unwrapMessageArgument(SecurityManager smgr, Object argument) {
        try {
            ArrayList<MessageData> mList = new ArrayList<MessageData>();
            JSONArray arr = (JSONArray) argument;
            for (int i = 0; i < arr.length(); i++) {
                MessageData md = MessageData.fromJson((String)arr.get(i));
                if (md == null) {
                    continue;
                }
                mList.add(md);
            }
            return mList;
        } catch (JSONException e) {
        }
        return null;
    }

    public byte[] wrap(SecurityManager smgr) {
        JSONArray arr = new JSONArray();
        arr.put(commandString + "/" + version);
        arr.put(type);
        arr.put(senderId);
        arr.put(argument);
        
        
        
        return arr.toString().getBytes();
    }

}
