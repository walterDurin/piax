package org.piax.ov.jmes;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.piax.ov.Overlay;

import org.piax.trans.Peer;
import org.piax.trans.ReceiveListener;
import org.piax.trans.RequestListener;
import org.piax.trans.SecurityManager;
import org.piax.trans.Target;
import org.piax.trans.Transport;
import org.piax.trans.ProtocolUnsupportedException;
import org.piax.trans.common.ReturnSet;
import org.piax.trans.target.RecipientId;

public abstract class MessageOverlay extends Overlay implements ReceiveListener, RequestListener {
    List<CommandReceiveListener> cListeners;
    
    public MessageOverlay(Map<Integer,Object> params) {
        super(params);
        cListeners = new ArrayList<CommandReceiveListener>();
    }

    @Override
    public abstract void send(Target target, Serializable payload) throws ProtocolUnsupportedException, IOException;
    
    // The messaging api that should be implemented in the subclasses.
    //public abstract void sendMessage(Message mes);

    public void addCommandReceiveListener(CommandReceiveListener listener) {
        cListeners.add(listener);
    }
    // start
    // stop
    
    // Utilitiies
    public void sendCommand(Peer dst, Command com) throws ProtocolUnsupportedException, IOException {
        byte[] payload = smgr.wrap(com);
        System.out.println("dst=" + dst + "sending:" + new String(payload));
        trans.send(new RecipientId(dst.peerId), payload);
    }
    
    protected abstract void onReceiveCommand(Target target, Command com);
    
    @Override
    public ReturnSet<?> request(Target target, Serializable payload)
            throws ProtocolUnsupportedException, IOException {
        // XXX
        throw new ProtocolUnsupportedException("Not implemented yet.");
    }

    @Override
    public List<Class<? extends Target>> getTargetClass() {
        throw new ProtocolUnsupportedException("Not implemented in this class.");
    }

    @Override
    public Object onRequest(Target target, Serializable payload) {
        // XXX
        throw new ProtocolUnsupportedException("Not implemented yet.");
    }

    @Override
    public void onReceive(Target via, Serializable payload) {
        System.out.println("onReceive:" + new String((byte[])payload));
        onReceiveCommand(via, (Command) smgr.unwrap((byte[])payload));
    }
    
}
