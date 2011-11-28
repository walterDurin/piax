package org.piax.trans;

import java.util.ArrayList;
import java.util.List;

import org.piax.trans.util.Register;

public class TypedRegister<K, V> extends Register<K, V> {
    private static final long serialVersionUID = -5076294892595269993L;

    public List<V> getValuesForType(K key, Class c) {
        List<V> ret = new ArrayList<V>();
        List<V> l = getValues(key);
        if (l == null) {
            return null;
        }
        for (V v : l) {
            if (l.getClass().equals(c)) {
                ret.add(v);
            }
        }
        return ret;
    }
    
    public void setValueForType(K key, V value, Class c) {
        List<V> l = getValuesForType(key, c);
        if (l != null) {
            for (V v : l) {
                remove(key, v);
            }
        }
        add(key, value);
    }
}
