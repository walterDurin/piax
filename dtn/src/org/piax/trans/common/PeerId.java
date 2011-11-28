/*
 * PeerId.java
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
 * 2007/02/16 designed and implemented by M. Yoshida.
 * 
 * $Id: PeerId.java 225 2010-06-20 12:34:07Z teranisi $
 */
package org.piax.trans.common;

import java.math.BigInteger;

import org.piax.trans.msgframe.MessageReachable;

/**
 * The class represents Peer Id.
 * 
 * @author     Mikio Yoshida
 * @version    1.0.0
 */
public class PeerId extends Id implements MessageReachable {    
    private static final long serialVersionUID = 6078594063699255913L;
    
    public static final String ATTRIB_NAME = "$peer";

    public static int BYTE_LENGTH = 16;

    public static PeerId newId() {
        PeerId id = new PeerId(BYTE_LENGTH);
        id.rand();
        return id;
    }

    public static PeerId zeroId() {
        PeerId id = new PeerId(BYTE_LENGTH);
        for (int i = 0; i < id.val.length; i++) {
            id.val[i] = 0x00;
        }
        return id;
    }

    public boolean isZero() {
        for (int i = 0; i < val.length; i++) {
            if (val[i] != 0x00) return false;
        }
        return true;
    }

    private PeerId(int byteLen) {
        super(byteLen);
    }
    
    public PeerId(byte[] val) throws IllegalArgumentException {
        super(val);
        if (val.length != BYTE_LENGTH) {
            throw new IllegalArgumentException();
        }
    }
    
    public PeerId(String id) throws NumberFormatException {
        super(id);
    }

    /*
     * TODO use BigInteger for simple imple
     */
    public boolean isNearThan(Id a, Id b) {
        BigInteger b_x = new BigInteger(1, this.val);
        BigInteger b_a = new BigInteger(1, a.val);
        BigInteger b_b = new BigInteger(1, b.val);
        BigInteger dista = b_x.subtract(b_a).abs();
        BigInteger distb = b_x.subtract(b_b).abs();
        return dista.compareTo(distb) == -1;
    }
    
/*
    public static void main(String[] args) {
        //PeerId id = PeerId.newId();
        //System.out.println(id);
        PeerId id1 = new PeerId("01234567");
        PeerId id2 = new PeerId("81234566");
        PeerId id3 = new PeerId("fd0978ab");
        System.out.println(id1);
        for (int i = 0; i < id1.bitLen(); i++) {
            System.out.print("" + (id1.testBit(i) ? 1:0) );
        }
        System.out.println();
        System.out.println(id1.compareTo(id2));
        System.out.println(id1.commonPrefixLen(id2));
        System.out.println(id3.isNearThan(id1, id2));
    }
*/    
}
