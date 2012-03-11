package org.piax.trans;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.piax.trans.common.PeerId;
import org.piax.trans.common.PeerLocator;
import org.piax.trans.msgframe.CallerHandle;
import org.piax.trans.msgframe.MagicNumberConflictException;
import org.piax.trans.msgframe.MessagingLeaf;
import org.piax.trans.msgframe.NoSuchPeerException;
import org.piax.trans.msgframe.RPCInvoker;

//  IdResolver that only manages available neighbor Id/Locator mappings.

public class AvailableIdResolver extends RPCInvoker implements IdResolverIf {

    static final byte[] ID_RES_MAGIC = {(byte) 0x56};
    static final byte[] REVERSE_MAGIC = {(byte) 0x57};

    LocatorTransport locTrans;
    AcceptanceDelegate delegate;
    ResolveMessagingLeaf rml;
    BlockingQueue<ResolverAcceptorPack> queue;
    
    protected final TypedRegister<PeerId, PeerLocator> resolveMap;
    PeerId myId;
    String name;
    LocatorTransport trans;

    protected Map<PeerId, PeerLocator> rejectedMap;

    class ResolverAcceptorPack {
        public AcceptanceDelegate delegate;
        public CallerHandle handle;
        public PeerId id;
        public ResolverAcceptorPack(AcceptanceDelegate delegate,
                                    CallerHandle handle,
                                    PeerId id) {
            this.delegate = delegate;
            this.handle = handle;
            this.id = id;
        }
    }
    
    class ResolverAcceptorRunner implements Runnable {
        public void run() {
            ResolverAcceptorPack pack;
            PeerLocator senderLocator;
            while (true) {
                try {
                    pack = queue.take();
                } catch (InterruptedException e) {
                    pack = null;
                }
                if (pack == null) {
                    continue;
                }
                senderLocator = (PeerLocator) pack.handle.getSrcPeer();

                try {
                    if (pack.delegate == null ||
                        pack.delegate.checkAcceptFrom(pack.id, senderLocator)) {
                        
                        put(pack.id, senderLocator);
                        //                        System.out.println("Accepting! :" + pack.handle.getSrcPeer());
                        reply(pack.handle, myId.getBytes());
                        //System.out.println("Accepted! :" + pack.handle.getSrcPeer());
                        if (delegate != null) {
                            delegate.acceptedFrom(pack.id, senderLocator);
                        }
                    }
                    // rejected
                    else {
                        rejectedMap.put(pack.id, senderLocator);
                        //System.out.println("Rejecting! :" + pack.handle.getSrcPeer());
                        reply(pack.handle, PeerId.zeroId().getBytes());
                        //System.out.println("Rejected! :" + pack.handle.getSrcPeer());
                        if (pack.delegate != null) {
                            pack.delegate.rejectedFrom(pack.id, senderLocator);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class ResolveMessagingLeaf extends MessagingLeaf {
        public ResolveMessagingLeaf(LocatorTransport lTrans) throws MagicNumberConflictException {
            super(REVERSE_MAGIC, lTrans);
        }

        @Override
            public void receive(byte[] data, CallerHandle callerHandle) {
            // This means, 'Hey, I wanna be your friend' message is received.
            PeerId senderId = new PeerId(data);
            try {
                // already rejected
                if (rejectedMap.get(senderId) != null) {
                    reply(callerHandle, PeerId.zeroId().getBytes());
                    return;
                }
                queue.add(new ResolverAcceptorPack(delegate, callerHandle, senderId));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public void put(PeerId peerId, PeerLocator loc) {
        resolveMap.setValueForType(peerId, loc, loc.getClass());
    }
    
    @Override
    public boolean remove(PeerId peerId, PeerLocator loc) {
        List<PeerLocator> locs = resolveMap.getValues(peerId);
        if (locs != null) {
            PeerLocator found = null;
            for (PeerLocator l: locs) {
                if (l.equals(loc)) {
                    found = l;
                }
            }
            if (found != null) {
                resolveMap.remove(peerId, found);
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean add(PeerId peerId, PeerLocator loc) {
        resolveMap.add(peerId, loc);
        return true;
    }
    

    public AvailableIdResolver(LocatorTransport locTrans, PeerId id, String name)
        throws MagicNumberConflictException {
        this(ID_RES_MAGIC, id, locTrans, name);
        queue = new LinkedBlockingQueue<ResolverAcceptorPack>();
        new Thread(new ResolverAcceptorRunner()).start();
    }

    public AvailableIdResolver(byte[] magic, PeerId id, LocatorTransport locTrans, String name)
        throws MagicNumberConflictException {
        
        super(magic, locTrans);
        this.myId = id; 
        this.name = name;
        trans = locTrans;
        resolveMap = new TypedRegister<PeerId, PeerLocator>();
        rml = new ResolveMessagingLeaf(locTrans);
        this.locTrans = locTrans;
        rejectedMap = new ConcurrentHashMap<PeerId, PeerLocator>();
        this.delegate = null;
    }

    public void setDelegate(AcceptanceDelegate delegate) {
        this.delegate = delegate;
    }

    public void acceptChange(PeerId id, PeerLocator locator) {
        this.myId = id;
        acceptChange(locator);
    }

    public String toString() {
        return resolveMap.toString();
    }

    // Reverse resolve locator->peerId
    // plus, let the node know this node.
    public PeerId reverseResolve(PeerLocator locator) throws InterruptedIOException, NoSuchPeerException, IOException {
    	byte[] iddata;
        iddata = rml.sendSync(locator, myId.getBytes(), 20000);
        return new PeerId(iddata);
    }

    // Access to the peer specified by locator and get id/locator mapping
    public void learnPeerLocator (PeerLocator locator) throws InterruptedIOException, NoSuchPeerException, IOException {
        PeerId id = reverseResolve(locator);
        put(id, locator);
    }

    public void relearnPeerLocators (Class<? extends PeerLocator> locatorType) {
        for (PeerId id : getPeerIds()) {
            if (!myId.equals(id)) {
                try {
                    PeerLocator loc = getLocator(id);
                    if (locatorType.equals(loc.getClass()) && loc.immediateLink()) {
                        learnPeerLocator(loc);
                    }
                } catch (Exception e) {
                    synchronized(resolveMap) {
                        resolveMap.remove(id);
                    }
                    //System.out.println("forgot " + id);
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void acceptChange(PeerLocator newLoc) {
        put(myId, newLoc);
        // XXX relearn only related ids.
        relearnPeerLocators(newLoc.getClass());
    }

    @Override
    public void fin() {
    	super.fin();
    	rml.fin();
    }

    @Override
    public PeerLocator getLocator(PeerId peerId) {
        if (myId.equals(peerId)) {
            return trans.getLocator();
        }
        synchronized (resolveMap) {
            return trans.bestRemoteLocator(resolveMap.getValues(peerId));
        }
    }

    @Override
    public List<PeerId> getPeerIds() {
        synchronized (resolveMap) {
            return new ArrayList<PeerId>(resolveMap.keySet());
        }
    }

}
