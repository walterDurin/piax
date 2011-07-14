package org.piax.trans;

import java.util.Map;


public interface ResponseChecker {
    public boolean isWaitingFor(Map<Object,Object> message);
}
