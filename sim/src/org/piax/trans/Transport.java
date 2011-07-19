package org.piax.trans;

import java.io.IOException;
import java.util.Map;

import org.piax.trans.common.Id;

public interface Transport {
    public Id getId();
    public void setParameter(Object key, Object value);
    public void addReceiveListener(ReceiveListener listener);
    public void send(TransPack mes) throws IOException;
    public TransPack sendAndWait(TransPack mes, ResponseChecker checker) throws IOException;
    
    public Node getSelfNode();

    public Object getAttr(Object key);
	public void putAttr(Object key, Object value);
	public Map<Object,Object> getAttrs();
    
    public void fin();
}
