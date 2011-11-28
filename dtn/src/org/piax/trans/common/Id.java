package org.piax.trans.common;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.piax.trans.util.MersenneTwister;

/*
 * The reason of not using BigInteger is below.
 * - shorten memory randSize.
 * - treat unsigned
 * 
 * In jdk 5.0 case, BigInteger has 24 bytes overhead.
 * As the proper data length of Id is 16 or 20 bytes, it is not small.
 */

/** 
 * Basic class of Id.
 * 
 * @author     Mikio Yoshida
 * @version    1.0.0
 */
public class Id implements Serializable, Comparable<Id> {
    private static final long serialVersionUID = 4353081717413249552L;
    
    private static MersenneTwister rand;
    static {
        long seed = System.nanoTime();
        try {
            seed += Arrays.hashCode(InetAddress.getLocalHost().getAddress());
        } catch (UnknownHostException e) {
            seed += new Object().hashCode();
        }
        rand = new MersenneTwister(seed);
    }

    public static Id newId(int len) {
        Id id = new Id(len);
        id.rand();
        return id;
    }
    
    protected byte[] val;
    
    protected Id(int byteLen) {
        val = new byte[byteLen];
    }
    
    public Id(String id) throws NumberFormatException {
        if (id.length() % 2 != 0) {
            throw new NumberFormatException();
        }
        val = new byte[id.length() / 2];
        for (int i = 0; i < val.length; i++) {
            String digit = id.substring(i * 2, i * 2 + 2);
            // I want to use unsigned operation, so...
            val[i] = (byte) Integer.parseInt(digit, 16);
        }
    }
    
    public Id(byte[] val) {
        if (val.length == 0) {
            throw new IllegalArgumentException();
        }
        this.val = val;
    }
    
    protected void rand() {
        rand.nextBytes(val);
    }

    public byte[] getBytes() {
        return val;
    }
    
    public boolean testBit(int ix) {
        if (ix < 0 || ix >= val.length * 8) {
            throw new IllegalArgumentException();
        }
        int n = ix / 8;
        int m = ix % 8;
        byte mask = (byte) ( 0x01 << (7 - m));
        if ((val[n] & mask) == 0) {
            return false;
        } else {
            return true;
        }
    }
    
    public int bitLen() {
        return val.length * 8;
    }
    
    public int commonPrefixLen(Id id) {
        int max = Math.min(bitLen(), id.bitLen());
        for (int i = 0; i < max; i++) {
            if (testBit(i) ^ id.testBit(i)) {
                return i;
            }
        }
        return max;
    }
    
    public boolean samePrefix(Id id, int nBits) {
        for (int i = 0; i < nBits; i++) {
            if (testBit(i) ^ id.testBit(i)) {
                return false;
            }
        }
        return true;
    }
    
    /*
     * TODO Think!
     * This compareTo compare two objects if these two have different
     * byte lengths. 
     * throwing IllegalArgumentException is one idea.
     */
    public int compareTo(Id id) {
        if (!(id instanceof Id)) {
            throw new ClassCastException();
        }

        Id _id = (Id) id;
        int max = Math.min(val.length, _id.val.length);
        for (int i = 0; i < max; i++) {
            int _val = ((int) val[i]) & 0xff;
            int _idVal = ((int) _id.val[i]) & 0xff;
            if (_val > _idVal) {
                return 1;
            } else if (_val < _idVal) {
                return -1;
            }
        }
        if (val.length > _id.val.length) {
            for (int j = max; j < val.length; j++) {
                if (val[j] != 0) {
                    return 1;
                }
            }
            return 0;
        } else if (val.length == _id.val.length) {
            return 0;
        } else {
            for (int j = max; j < _id.val.length; j++) {
                if (_id.val[j] != 0) {
                    return -1;
                }
            }
            return 0;
        }
    }
    
    /*
     * Returns false if id has different byte length.
     * This is the different point from compareTo.
     */
    @Override
    public boolean equals(Object id) {
        if (id == null) {
            return false;
        }
        if (!(id instanceof Id)) {
            return false;
        }
        return Arrays.equals(val, ((Id) id).val);
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(val);
    }
    
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        
        for (int i = 0; i < val.length; i++) {
            // I want to use unsigned operation, so...
            int _val = ((int) val[i]) & 0xff;
            char upper = Character.forDigit(_val / 16, 16);
            char lower = Character.forDigit(_val % 16, 16);
            str.append(upper);
            str.append(lower);
        }
        return str.toString();
    }
}
