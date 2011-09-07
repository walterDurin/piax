package org.piax.ov.ovs.skipgraph;

import java.util.ArrayList;

import org.piax.trans.util.MersenneTwister;

public class MembershipVector {
    public static int ALPHABET = 2;
    private static MersenneTwister rand;
    static {
        long seed = System.nanoTime();
        seed += new Object().hashCode();
        rand = new MersenneTwister(seed);
    }    

    ArrayList<Integer> vector;
    public MembershipVector() {
        vector = new ArrayList<Integer>();
    }
    
    public boolean existsElementAt(int pos) {
        if (vector.size() < pos) {
            return false;   
        }
        else {
            return true;
        }
    }

    public void randomElement(int pos) {
        if (vector.size() < pos) {
            vector.add(rand.nextInt(ALPHABET));
        }
        else { // this never happens???
            vector.set(pos - 1, rand.nextInt(ALPHABET));
        }
    }
    
    public boolean equals(int l, MembershipVector val) {
        if (vector.size() < l || val.vector.size() < l) {
            return false;
        }
        for (int i = 0; i < l; i++) {
            if (vector.get(i) != val.vector.get(i)) {
                return false;
            }
        }
        return true;
    }
    
    public boolean equals(MembershipVector val) {
        for (int i = 0; i < vector.size(); i++) {
            if (val.vector.size() < i + 1) {
                return false;
            }
            if (vector.get(i) != val.vector.get(i)) {
                return false;
            }
        }
        return true;
    }
    
    public String toString() {
        String ret = "";
        for (int i = 0; i < vector.size(); i++) {
            ret += vector.get(i);
        }
        return ret;
    }
    
    static public void main(String[] args) {
        MembershipVector mv = new MembershipVector();
        MembershipVector mv2 = new MembershipVector();

        mv.randomElement(1);
        mv.randomElement(2);
        mv2.randomElement(1);
        mv2.randomElement(2);
        
        System.out.println(mv);
        System.out.println(mv2);
        
        System.out.println(mv2.existsElementAt(2));
        System.out.println(mv2.existsElementAt(3));
        System.out.println(mv2.existsElementAt(4));

        if (mv.equals(2, mv2)) {
            System.out.println("EQUALS");
        }
        else {
            System.out.println("NOT EQUALS");
        }
        
    }

}