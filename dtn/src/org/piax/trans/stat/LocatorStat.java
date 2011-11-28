package org.piax.trans.stat;

import java.util.Date;

import org.piax.trans.common.PeerLocator;

public class LocatorStat {
    public PeerLocator locator;
    public int status;
    public Date lastSeen;
    public Date lastConnected;
    public LinkStatistics statistics;
}
