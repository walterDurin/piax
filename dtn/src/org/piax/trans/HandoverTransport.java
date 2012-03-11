package org.piax.trans;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.piax.trans.common.*;
import org.piax.trans.msgframe.*;
import org.piax.trans.target.RecipientId;
//import org.piax.trans.ts.LocatorTransportSpi;
//import org.piax.trans.ts.udp.UdpLocator;
import org.piax.trans.target.RecipientIdWithLocator;

//import android.util.Log;

public class HandoverTransport implements Transport {
    PeerId id;
    String name;
    PeerLocator locator;
    byte[] magic;
    
    IdTransport trans;
    AvailableIdResolver resolver;
    DataAsyncMessagingLeaf daml;
    
    protected List<ReceiveListener> receiveListeners;
    protected List<RequestListener> requestListeners;

    static final byte[] DEFAULT_DATA_MAGIC = {(byte) 0x71};

    class DataAsyncMessagingLeaf extends MessagingLeaf {
    	IdTransport trans;
    	public DataAsyncMessagingLeaf(byte[] magic, IdTransport trans) throws MagicNumberConflictException {
            super(magic == null? DEFAULT_DATA_MAGIC : magic, trans);
        }

        @Override
        public void receive(byte[] data, CallerHandle callerHandle) {
            System.out.println("*** receive:" + new String(data) + "listeners=" + receiveListeners);
            for (ReceiveListener listener : receiveListeners) {
                // XXX no need to check whether the message is for me?
                // XXX XXX second argument should be this node's locator.
                listener.onReceive(new RecipientIdWithLocator(id, (PeerLocator)callerHandle.getSrcPeer()), data);
            }
        }
    }

    public HandoverTransport() {
        this (PeerId.newId(), null);
    }

    public HandoverTransport(PeerId id, String name) {
        this (id, name, null, null);
    }

    public HandoverTransport(String name, PeerLocator locator) {
        this (PeerId.newId(), name, locator, null);
    }

    public HandoverTransport(byte[] magic) {
        this(PeerId.newId(), null, null, magic);
    }

    // create new transport sharing underlaying IdTransport
    // Should it be Singleton model?
    public HandoverTransport(HandoverTransport trans, byte[] magic) {
        this.id = trans.id;
        this.receiveListeners = new ArrayList<ReceiveListener>();
        this.requestListeners = new ArrayList<RequestListener>();
        try {
            this.trans = trans.trans;
            resolver = trans.resolver;
            daml = new DataAsyncMessagingLeaf(magic, trans.trans);
        } catch (MagicNumberConflictException e) {
            e.printStackTrace();
        }
    }

    public HandoverTransport(PeerId id, String name, PeerLocator locator, byte[] magic) {
        this.id = id;
        this.name = name;
        this.locator = locator;
        this.magic = magic;
        this.receiveListeners = new ArrayList<ReceiveListener>();
        this.requestListeners = new ArrayList<RequestListener>();
    }
    
    public void setDelegate(AcceptanceDelegate delegate) {
        resolver.delegate = delegate;
    }

    public PeerId getPeerId() {
        return id;
    }

    public void setPeerId(PeerId id) {
        this.id = id;
        trans.setPeerId(id);
        for (PeerLocator loc : getLocators()) {
            resolver.acceptChange(id, loc);
        }
    }

    public void clearLocators()  {
        trans.clearLocators();
    }

    public List<PeerLocator> getLocators()  {
        return trans.getLocators();
    }

    public void clearLocator(PeerLocator locator)  {
        trans.clearLocator(locator);
    }

    public void addLocator(PeerLocator locator) throws NoSuchPeerException {
        addLocator(locator, null);
    }    

    public void addLocator(PeerLocator locator, Collection <PeerLocator> relays) throws NoSuchPeerException {
        try {
            trans.addLocator(locator, relays);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public IdTransport getIdTransport() {
        return trans;
    }

    public PeerLocator resolve(PeerId id) {
        PeerLocator loc = null;
        synchronized (resolver) {
            loc = resolver.getLocator(id);
        }
        return loc; 
    }

    public void learnIdLocatorMapping(PeerId id, PeerLocator locator) {
    	resolver.put(id, locator);
    }
    
    public void forgetIdLocatorMapping(PeerId id, PeerLocator locator) {
        resolver.remove(id, locator);
    }

    public PeerId learnLocator(PeerLocator loc) throws NoSuchPeerException, ContactRefusedException {
        PeerId id = null;
        try {
            if (trans.getLocator(loc.getClass()) != null) {
                synchronized (resolver) {
                    id = resolver.reverseResolve(loc);
                    if (id != null) {
                        if (id.isZero()) {
                            throw new ContactRefusedException("contact for " + loc + " is refused");
                        }
                        else {
                            resolver.put(id, loc);
                        }
                    }
                    else {
                        throw new NoSuchPeerException();
                    }
                }
            }
        } catch (InterruptedIOException e) {
            //System.out.println("*** Interrupted request to " + loc.getPeerNameCandidate());
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return id;
    }

    public void learnServiceInfo(ServiceInfo info) throws NoSuchPeerException, ContactRefusedException, InterruptedIOException, IOException {
        PeerId id = null;
        PeerLocator loc = PeerLocator.create(info);
        if (info == null) {
            return;
        }
        // Check whether the id/locator mapping is not changed.
        if (trans.getLocator(loc.getClass()) != null) {
            id = resolver.reverseResolve(loc);
            if (id != null) {
                if (id.isZero()) {
                    throw new ContactRefusedException("contact for " + loc + " is refused");
                }
                else {
                    resolver.put(id, loc);
                    info.setId(id);
                }
            }
            else {
                throw new NoSuchPeerException();
            }
        }
        else {
            // locator is currently null, learn service info for now.
            resolver.put(info.getId(), loc);
        }
    }

    public Collection<PeerId> knownPeerIds() {
        return resolver.getPeerIds();
    }

    public PeerId peerId() {
    	return id;
    }

    public ServiceInfo getServiceInfo () {
        return getServiceInfo(id);
    }

    public ServiceInfo getServiceInfo (PeerId id) {
    	ServiceInfo info = null;
        PeerLocator l = resolver.getLocator(id);
        if (l != null) {
        	info = l.getServiceInfo();
        	info.setId(id);
        }
        return info;
    }
    
    public void fin() {
    	daml.fin();
    	resolver.fin();
    	trans.fin();
    }

    @Override
    public void start() throws IOException {
        try {
            trans = new IdTransport(this.id, name, null, null);
            resolver = new AvailableIdResolver(trans.getLocatorTransport(), this.id, name);
            trans.setIdResolver(resolver);
            daml = new DataAsyncMessagingLeaf(magic, trans);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MagicNumberConflictException e) {
            e.printStackTrace();
        }
        if (locator != null) {
            try {
                addLocator(locator);
            } catch (NoSuchPeerException e) {
            }
        }
    }

    @Override
    public void stop() {
       fin();
    }

    @Override
    public SecurityManager getSecurityManager() {
        // XXX SSLEngine?
        return null;
    }

    @Override
    public void send(Target target, Serializable payload) throws ProtocolUnsupportedException, IOException {
        if (!(target instanceof RecipientId)) {
            throw new ProtocolUnsupportedException("Only recipient Id is supported."); 
        }
        if (! (payload instanceof byte[])) {
            throw new ProtocolUnsupportedException("Only an array of bytes is supported as a payload."); 
        }
        PeerId recipientId = ((RecipientId)target).getId();
        if (knownPeerIds().contains(recipientId)) {
            try {
                daml.send(recipientId, (byte[])payload);
            } catch (NoSuchPeerException e) {
                throw new IOException(e);
            }
        }
    }

    @Override
    public void addReceiveListener(ReceiveListener listener) {
        receiveListeners.add(listener);
    }

    @Override
    public void removeReceiveListener(ReceiveListener listener) {
        receiveListeners.remove(listener);
    }

    @Override
    public ReturnSet<?> request(Target target, Serializable payload)
            throws ProtocolUnsupportedException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addRequestListener(RequestListener listener) {
        requestListeners.add(listener);        
    }

    @Override
    public void removeRequestListener(RequestListener listener) {
        requestListeners.remove(listener);
    }

    @Override
    public List<Class<? extends Target>> getTargetClass() {
        ArrayList<Class<? extends Target>> target = new ArrayList<Class<? extends Target>>();
        target.add(RecipientId.class);
        return target;
    }

    @Override
    public void clearReceiveListeners() {
        receiveListeners.clear();
    }

    @Override
    public void clearRequestListeners() {
        requestListeners.clear();
    }

}
