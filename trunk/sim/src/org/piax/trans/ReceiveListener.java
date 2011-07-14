package org.piax.trans;

import java.util.Map;

public interface ReceiveListener {
    public void onReceive(Node sender, Map<Object,Object> mes);
}
