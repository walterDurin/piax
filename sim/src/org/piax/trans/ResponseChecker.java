package org.piax.trans;


public interface ResponseChecker {
    public boolean isWaitingFor(Message message);
}
