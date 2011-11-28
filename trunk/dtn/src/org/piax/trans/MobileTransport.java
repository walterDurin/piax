package org.piax.trans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.piax.trans.common.PeerId;
import org.piax.trans.common.PeerLocator;
import org.piax.trans.common.ServiceInfo;
import org.piax.trans.msgframe.NoSuchPeerException;
import org.piax.trans.stat.LocatorStat;
import org.piax.trans.stat.TrafficInfo;
import org.piax.trans.tsd.TSD;
import org.piax.trans.tsd.TSDListener;

public class MobileTransport extends MultiLocatorTransport implements TSDListener, AcceptanceDelegate, PeerManager {
    PeerId pid;
    String name;
    List<TSD> tsds;
    PeerStateDelegate stateDelegate;
    //ServiceInfo info;
    boolean available;

    List<Peer> peerStats;
    
    // PIAXTransport provides
    // send

    // New methods
    // addTransportStateListener(TransportStateListener tsl)
    // link(ServiceInfo info)
    // unlink(ServiceInfo info)
    // unlink(Locator locator)
    // unlink(PeerId peerId)
    // ... and methods as PeerStatManagerIf.
    
    
    // PeerStatManagerIf
    @Override
    public List<Peer> listPeers() {
        synchronized(peerStats) {
            return peerStats;
        }
    }

    @Override
    public List<Peer> listSortedPeers() {
        ArrayList<Peer> ret = new ArrayList<Peer>(peerStats);
        Collections.sort(ret, new Comparator<Peer>() {
            public int compare(Peer o1, Peer o2) {
                Date date1 = o1.lastSeen;
                Date date2 = o2.lastSeen;
                return date2.compareTo(date1);
            }
        });
        return ret;
    }
    
    @Override
    public void addPeer(Peer ps) {
        peerStats.add(ps);
    }
    
    @Override
    public Peer getPeerByLocator(PeerLocator loc) {
        for (Peer ps: peerStats) {
            if (ps.lstats != null) {
                for (LocatorStat lstat : ps.lstats) {
                    if (lstat.locator.equals(loc)) {
                        return ps;
                    }
                }
            }
        }
        return null;    
    }
    
    @Override
    public Peer getPeer(PeerId peerId) {
        for (Peer ps: peerStats) {
            if (peerId.equals(ps.peerId)) {
                return ps;
            }
        }
        return null;
    }
    
    @Override
    public Peer getPeerCreate(PeerId id) {
        if (id.equals(pid)) { // myself ... return null;
            return null;
        }
        synchronized(peerStats) {
            Peer ps = getPeer(id);
            if (ps != null) {
                return ps;
            }
            Peer nps = new Peer();
            nps.peerId = id;
            nps.peerIdString = id.toString();
            addPeer(nps);
            return nps;
        }
    }

    @Override
    public Peer getPeerCreate(ServiceInfo info) {
        Peer node = null;
        if (info.getId() == null) {
            node = getPeerCreateByLocator(PeerLocator.create(info));
        }
        else {
            synchronized(peerStats) {
                node = getPeerByLocator(PeerLocator.create(info));
                if (node != null) {
                    if (node.peerId == null) { // unknown info already exists.
                        peerStats.remove(node);
                    }
                }
                node = getPeerCreate(info.getId());
                if (node == null) return null;
                if (info.getName() != null) {
                    node.name = info.getName();
                }
                node.addLocator(PeerLocator.create(info));
            }
        }
        return node;
    }

    @Override
    public Peer getPeerCreateByLocator(PeerLocator locator) {
        Peer ps = getPeerByLocator(locator);
        if (ps != null) {
            return ps;
        }
        synchronized(peerStats) {
            Peer nps = new Peer();
            nps.addLocator(locator);
            addPeer(nps);
            return nps;
        }
    }
    
    @Override
    public Peer setIdAndLocatorMapping(PeerId id, PeerLocator locator) {
        Peer ps = getPeerByLocator(locator);
        if (ps != null) {
            String name = ps.name;
            System.out.println("*** found peer's peerid=" + ps.peerId);
            if (ps.peerId == null) { // unknown info already exists.
                peerStats.remove(ps);
                System.out.println("*** unknown id peer removed.");
                ps = getPeerCreate(id);
                ps.name = name;
                ps.addLocator(locator);
            }
            else {
                ps.setPeerId(id);
            }
            return ps;
        }
        else {
            System.out.println("*** not found");
        }
        return null;
    }
    
    @Override
    public void setLocatorStatus(PeerLocator locator, int status) {
        // If sudden receive occurred before discovery, it should not be updated. 
        Peer ps = getPeerByLocator(locator);
        if (ps != null) {
            ps.setLocatorStatus(locator, status);
        }
    }

    @Override
    public void putSend(int msgSize, PeerLocator to) {
        Peer ps = getPeerByLocator(to);
        if (ps != null) {
            ps.putSend(msgSize, to);
        }
    }
    
    @Override
    public void putReceive(int msgSize, PeerLocator from) {
        Peer ps = getPeerByLocator(from);
        if (ps != null) {
            ps.putReceive(msgSize, from);
        }
    }
    
    @Override
    public TrafficInfo getTraffic() {
        // XXX not implemented yet.
        // count each active peer's traffic info?
        return null;
    }
    
    
    // TSDListener
    @Override
    public void serviceAvailable (ServiceInfo info, boolean first) {
        int state;
        if (first) {
            state = Peer.TransportStateAvailable;
        }
        else {
            state = Peer.TransportStateAlive;
        }
        Peer peer = getPeerCreate(info);
        PeerLocator loc = PeerLocator.create(info);
        if (peer != null) {
            peer.setLocatorStatus(loc, state);
            stateDelegate.onPeerStateChange(peer, loc, state);
        }
    }

    @Override
    public void serviceUnavailable (ServiceInfo info) {
        PeerLocator loc = PeerLocator.create(info);
        //        System.out.println("*** UNAVAILABLE:" + loc.getPeerNameCandidate());
        if (info.getId() != null) {
            this.forgetIdLocatorMapping(info.getId(), loc);
            Peer peer = this.getPeer(info.getId());
            if (peer != null) {
                peer.setLocatorStatus(loc, Peer.TransportStateUnavailable);
            }
            if (stateDelegate != null) {
                stateDelegate.onPeerStateChange(peer, loc, Peer.TransportStateUnavailable);
            }
        }
    }
    
//    public ExtIdTransport(PeerId pid, String name, TSD tsd) {
//        super(pid, name);
//        this.pid = pid;
//        this.name = name;
//        this.tsds = new ArrayList<TSD>();
//        this.tsds.add(tsd);
//        setDelegate(this);
//        stateListeners = new ArrayList<TransportStateListener>();
//        peerStats = new ArrayList<PeerStat>();
//        tsd.addServiceListener(this);
//    }

    public MobileTransport(PeerId pid, String name) {
        super(pid, name);
        this.pid = pid;
        this.name = name;
        this.tsds = new ArrayList<TSD>();
        stateDelegate = null;
        peerStats = new ArrayList<Peer>();
    }
    
    @Override
    public void start() throws IOException {
        super.start();
        setDelegate(this);
        this.trans.getLocatorTransport().setPeerStatManager(this);
    }

    public void addTSD(TSD tsd) {
        this.tsds.add(tsd);
        tsd.addServiceListener(this);
        if (available) {
            for (PeerLocator locator : this.getLocators()) {
                ServiceInfo info = locator.getServiceInfo();
                if (info != null) {
                    info.setId(pid);
                    info.setName(name);
                    tsd.registerService(info);
                }
            }
        }
        // XXX At this time, tsd is already running?
    }

    public void addLocator(PeerLocator locator) throws NoSuchPeerException {
        super.addLocator(locator);
        ServiceInfo info = locator.getServiceInfo();
        if (info != null) {
            info.setId(pid);
            info.setName(name);
            for (TSD tsd : tsds) {
                // if already stated, nothing happens.
                tsd.registerService(info);
                tsd.start();
            }
        }
        available = true;
    }

//    public void setAvailable(boolean avail) {
//        available = avail;
//        if (avail) {
//            // multiple register is avoided.
//            for (TSD tsd : tsds) {
//                tsd.registerService(info);
//            }
//        }
//        else {
//            for (TSD tsd : tsds) {
//                tsd.unregisterService(info);
//            }
//        }
//    }

    public void clearLocator(PeerLocator locator) {
        ServiceInfo info = locator.getServiceInfo();
        info.setId(pid);
        info.setName(name);
        for (TSD tsd : tsds) {
            tsd.unregisterService(info);
        }
    }

    public void clearLocators() {
        for (PeerLocator locator : super.getLocators()) {
            clearLocator(locator);
        }
        super.clearLocators();
    }
    
    class LinkerThread implements Runnable {
        Peer peer;
        PeerLocator locator;
        public LinkerThread(Peer peer, PeerLocator locator) {
            this.peer = peer;            
            this.locator = locator;
        }
        public void run() {
            int tStat = Peer.TransportStateLinking;
            if (stateDelegate != null) {
                peer.setLocatorStatus(locator, tStat);
                stateDelegate.onPeerStateChange(peer, locator, tStat);
            }
            try {
                PeerId id = learnLocator(locator);
                //peer = setIdAndLocatorMapping(id, locator);
                peer.peerId = id;
                if (id != null) {
                    System.out.println("LINK OK:" + id + " for " + peer.peerId);
                    if (resolve(peer.peerId) != null) {
                        tStat = Peer.TransportStateLinked;
                    }
                    
                }
                else {
                    System.out.println("LINK FAILED");
                    tStat = Peer.TransportStateLinkFailure;
                }
            }
            catch (ContactRefusedException e) {
                e.printStackTrace();
                tStat = Peer.TransportStateLinkRefused;    
            }
            catch (NoSuchPeerException e) {
                e.printStackTrace();
                tStat = Peer.TransportStateLinkFailure;
            }
            if (stateDelegate != null) {
                stateDelegate.onPeerStateChange(peer, locator, tStat);
            }
            setLocatorStatus(locator, tStat);
        }
    }

    public void newLink(Peer peer, PeerLocator locator) {
    	Thread linker = new Thread(new LinkerThread(peer, locator));
    	linker.start();
    }

    public void acceptedFrom(PeerId pid, PeerLocator locator) {
        Peer peer = getPeerCreate(pid);
        if (stateDelegate != null) {
            stateDelegate.onPeerStateChange(peer, locator, Peer.TransportStateAccepted);
        }
    }

    public void rejectedFrom(PeerId pid, PeerLocator locator) {
        Peer peer = getPeerCreate(pid);
        if (stateDelegate != null) {
            stateDelegate.onPeerStateChange(peer, locator, Peer.TransportStateRejected);
        }
    }

    public void unlink(ServiceInfo info) {
        // forgetServiceInfo(info);
        // notifyState(info, Peer.TransportStateUnlinked);
    }

    public void restart(PeerId pid, String name) {
        //        System.out.println("OLD pid=" + this.pid);
        this.pid = pid;
        //        System.out.println("NEW pid=" + this.pid);
        this.name = name;
        for (TSD tsd : tsds) {
            tsd.unregisterAllServices();
        }
        trans.setPeerId(pid);
        
        // advertise all locators for myself.
        for (PeerLocator locator : trans.getLocators()) {
            ServiceInfo info = locator.getServiceInfo();
            info.setId(pid);
            info.setName(name);
            for (TSD tsd : tsds) {
                tsd.registerService(info);
            }
        }
    }

    public void fin() {
        super.fin();
        for (TSD tsd : tsds) {
            tsd.close();
        }
    }

    public boolean checkAcceptFrom(PeerId id, PeerLocator locator) {
        Peer peer = getPeerCreate(id);
        if (stateDelegate != null) {
            return stateDelegate.onAccepting(peer, locator);
        }
        else {
            return false;
        }
    }

    public void setPeerStateDelegate(PeerStateDelegate delegate) {
        stateDelegate = delegate;
    }
    
    /*

 // Correct node status.
    if ((new Date().getTime() - node.lastSeen.getTime()) > tsdTimeoutPeriod * 1000) {
        node.setStatus(PeerStat.TransportStateUnavailable);
    }
    if (state != PeerStat.TransportStateAlive) {
        // If already linked, no need to change its state.
        if ((state == PeerStat.TransportStateAvailable) &&
            (node.getStatus() == PeerStat.TransportStateAccepted ||
             node.getStatus() == PeerStat.TransportStateLinked)) {
            //System.out.println("@@@@ node.status for " + node.id + "is already linked but " + state);
        }
        else {
            node.setStatus(state);
        }
    }
    if (state != PeerStat.TransportStateUnavailable) {
        node.alive();
    }
    */
    
}
