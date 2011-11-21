/*
 * KeyComparator.java
 * 
 * Copyright (c) 2006- Osaka University
 * Copyright (c) 2004-2005 BBR Inc, Osaka University
 * 
 * Permission is hereby granted, free of charge, to any person obtaining 
 * a copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including 
 * without limitation the rights to use, copy, modify, merge, publish, 
 * distribute, sublicense, and/or sell copies of the Software, and to 
 * permit persons to whom the Software is furnished to do so, subject to 
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be 
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
/*
 * Revision History:
 * ---
 * 2007/01/02 designed and implemented by M. Yoshida.
 * 
 * $Id: KeyComparator.java 37 2009-10-19 20:52:01Z yos $
 */

package org.piax.trans.target;

import java.util.Comparator;

/**
 * @author     Mikio Yoshida
 * @version    1.0.0
 */
public class KeyComparator implements Comparator<Comparable<?>> {

    private static enum Special {
        BOTTOM, UNDEF, TOP
    }
    
    public static final Special BOTTOM_VALUE = Special.BOTTOM;
    public static final Special UNDEF_VALUE = Special.UNDEF;
    public static final Special TOP_VALUE = Special.TOP;
    
    private static KeyComparator instance = new KeyComparator();
    
    public static KeyComparator getInstance() {
        return instance;
    }
    
    private KeyComparator() {}
    
    /* (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public int compare(Comparable<?> key1, Comparable<?> key2) {
        // if key1 is special
        if (key1.equals(UNDEF_VALUE)) {
            if (key2.equals(UNDEF_VALUE)) return 0;
            if (key2.equals(BOTTOM_VALUE)) return 1;
            return -1;
        }
        if (key1.equals(BOTTOM_VALUE)) {
            if (key2.equals(UNDEF_VALUE)) return -1;
            if (key2.equals(BOTTOM_VALUE)) return 0;
            return -1;
        }
        if (key1.equals(TOP_VALUE)) {
            if (key2.equals(UNDEF_VALUE)) return 1;
            if (key2.equals(TOP_VALUE)) return 0;
            return 1;
        }
        
        // if key1 is normal
        // and if key2 is special
        if (key2.equals(UNDEF_VALUE) || key2.equals(BOTTOM_VALUE)) {
            return 1;
        }
        if (key2.equals(TOP_VALUE)) {
            return -1;
        }
        
        // and if key2 is normal
        if (key1.getClass() == key2.getClass()) {
            return ((Comparable<Object>) key1).compareTo(key2);
        } else {
            return key1.getClass().getName().
                compareTo(key2.getClass().getName());
        }
    }
    
    public boolean isTOP(Comparable<?> key) {
        return TOP_VALUE.equals(key);
    }
    
    public boolean isBOTTOM(Comparable<?> key) {
        return BOTTOM_VALUE.equals(key);
    }
    
    public boolean isUNDEF(Comparable<?> key) {
        return UNDEF_VALUE.equals(key);
    }

    public boolean isProper(Comparable<?> key) {
        return (!isTOP(key) && !isBOTTOM(key));
    }

    public boolean isOrdered(Comparable<?> key1, Comparable<?> key2) {
        return compare(key1, key2) <= 0;
    }

    public boolean isOrdered(Comparable<?> key1, Comparable<?> key2, Comparable<?> key3) {
        return compare(key1, key2) <= 0 && compare(key2, key3) <= 0;
    }

    public boolean isBetween(Comparable<?> x, 
            Comparable<?> a, Comparable<?> b) {
        if (compare(a, b) > 0) {
            return compare(b, x) <= 0 && compare(x, a) <= 0;
        }
        return compare(a, x) <= 0 && compare(x, b) <= 0;
    }
}
