package org.piax.trans.sim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	public int out;
	public int in;
	
	static public SimTransportOracle o = null;
	
	static public SimTransportOracle getOracle() {
        return o;
    }
	public void clearCount() {
	    in = 0;
	    out = 0;
	}
	public SimTransport() {
	    in = 0;
	    out = 0;
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
	
	// XXX BUG in this method!!!
	private void convertNodeInstance_OLD(Map<Object,Object> mes) {
	    for (Object key : mes.keySet()) {
	        Object value = mes.get(key);
	        if (value instanceof Node) {
	            ((Node)value).trans = this;
	        }
	        else if (value instanceof List) {
	            for (Object obj : (List)value) {
	                if (obj instanceof Node) {
	                    ((Node)obj).trans = this;
	                }
	            }
	        }
	    }
	}
	
	private void convertNodeInstance(Map<Object,Object> mes) {
        for (Object key : mes.keySet()) {
            Object value = mes.get(key);
            if (value instanceof Node) {
                Node node = (Node)value;
                Node repl = new Node(this, node.getId(), node.self, node.attrs);
                mes.put(key, repl);
            }
            else if (value instanceof List) {
                List repl = new ArrayList<Object>();
                for (Object obj : (List)value) {
                    if (obj instanceof Node) {
                        Node node = (Node)obj;
                        Node replN = new Node(this, node.getId(), node.self, node.attrs);
                        repl.add(replN);
                    }
                    else {
                        repl.add(obj);
                    }
                }
                mes.put(key, repl);
            }
        }
    }
	
	public void onReceive(Node sender, Map<Object,Object>mes) {
	    in++;
	    // XXX This is very tricky part.
	    convertNodeInstance(mes);
	    sender = new Node(this, sender.getId(), sender.self, sender.attrs);
	    
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
        ret += "(" + in + "/" + out + ")";
        return ret;
    }

	@Override
	public void send(TransPack mes) throws IOException {
	    out++;
		o.send(mes);
	}

	@Override
	public TransPack sendAndWait(TransPack mes, ResponseChecker checker)
			throws IOException {
	    out++;
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
