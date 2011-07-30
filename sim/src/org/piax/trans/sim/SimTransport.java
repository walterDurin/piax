package org.piax.trans.sim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.piax.trans.Node;
import org.piax.trans.ReceiveListener;
import org.piax.trans.ResponseChecker;
import org.piax.trans.TransPack;
import org.piax.trans.Transport;
import org.piax.trans.common.Id;

public class SimTransport implements Transport, ReceiveListener {
	Id id;
	ReceiveListener listener;
	Map<Object,Object> attrs;
	
	static public SimTransportOracle o = null;
	
	static public SimTransportOracle getOracle() {
        return o;
    }
	
	public SimTransport() {
		if (o == null) {
			o = new SimTransportOracle();
		}
		id = o.getId();
		attrs = new HashMap<Object,Object>();
	}
	
	public Object getAttr(Object key) {
		return attrs.get(key);
	}
	
	public void putAttr(Object key, Object value) {
		attrs.put(key, value);
	}
	
	public Map<Object,Object> getAttrs() {
		return attrs;
	}
	
	public Node getSelfNode() {
		Node n = new Node(this, id, null, getAttrs());
		return n;
	}
	
	@Override
	public Id getId() {
		return id;
	}

	@Override
	public void setParameter(Object key, Object value) {
		o.setParameter(key, value);
	}

	@Override
	public void addReceiveListener(ReceiveListener listener) {
		this.listener = listener;
		o.addReceiveListener(this);
	}
	
	public void onReceive(Node sender, Map<Object,Object>mes) {
		listener.onReceive(sender, mes);
	}
	
	public Node getRemoteNode(Transport target) {
		return o.getRemoteNode(getSelfNode(), target.getSelfNode());
	}
	
	public long getElapsedTime() {
		return SimTransportOracle.getElapsedTime();
	}
	
	public String toString() {
        String ret = "";
        for (Object key: attrs.keySet()) {
            ret += "|" + attrs.get(key);
        }
        ret += "|";
        return ret;
    }

	@Override
	public void send(TransPack mes) throws IOException {
		// TODO Auto-generated method stub
		o.send(mes);
	}

	@Override
	public TransPack sendAndWait(TransPack mes, ResponseChecker checker)
			throws IOException {
		return o.sendAndWait(mes, checker);
	}

	@Override
	public void fin() {
		if (o != null) {
			o.fin();
			o = null;
		}
	}

}
