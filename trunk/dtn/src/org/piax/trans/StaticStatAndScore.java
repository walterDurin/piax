package org.piax.trans;

import org.piax.trans.common.PeerLocator;
import org.piax.trans.stat.LinkStatAndScoreIf;

public class StaticStatAndScore implements LinkStatAndScoreIf {

    @Override
    public Integer[] eval(PeerLocator src, PeerLocator dst) {
        if (!src.getClass().equals(dst.getClass())) {
            return new Integer []{0};
        }
        if (src.getClass().toString().contains("Bluetooth")) {
            // 40kbps
            return new Integer []{40};
        }
        if (src instanceof org.piax.trans.ts.InetLocator) {
            // 3Mbps
            return new Integer []{3000};
        }
        return new Integer [] {0}; 
    }

    @Override
    public String evalFormat() {
        return "%d";
    }

}
