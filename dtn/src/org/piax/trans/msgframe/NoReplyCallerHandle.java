package org.piax.trans.msgframe;

public class NoReplyCallerHandle implements CallerHandle {
    CallerHandle caller;
    
    public NoReplyCallerHandle(CallerHandle caller) {
        this.caller = caller;
    }

    public CallerHandle getSrcHandle() {
        return caller;
    }

    @Override
    public MessageReachable getSrcPeer() {
        return caller.getSrcPeer();
    }

}
