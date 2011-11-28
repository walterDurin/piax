package org.piax.trans.util;

/**
 * Mersenne Twister（高速・高精度の乱数ジェネレータ）の実装クラス。
 * 
 * @author     Mikio Yoshida
 * @version    1.0.0
 */
public class MersenneTwister extends java.util.Random {
    private static final long serialVersionUID = -3517094166157065652L;

    static final int N = 624;
    static final int M = 397;
    static final int UPPER_MASK = 0x80000000;
    static final int LOWER_MASK = 0x7FFFFFFF;
    static final int MATRIX_A   = 0x9908B0DF;
    int x[] = new int[N];
    int p, q, r;

    /**
     * Mersenne Twisterを生成する。
     */
    public MersenneTwister() {  setSeed(System.currentTimeMillis());  }
    
    /**
     * long型のシードを使って、Mersenne Twisterを生成する。
     * 
     * @param seed 初期シード
     */
    public MersenneTwister(long seed) {  setSeed(seed);  }

    /**
     * intの配列型のシードを使って、Mersenne Twisterを生成する。
     * 
     * @param seeds 初期シード
     */
    public MersenneTwister(int[] seeds) {  setSeed(seeds);  }

    /* (non-Javadoc)
     * @see java.util.Random#setSeed(long)
     */
    @Override
    synchronized public void setSeed(long seed) {
        if (x == null) return;
        x[0] = (int)seed;
        for (int i = 1; i < N; i++) {
            x[i] = 1812433253 * (x[i - 1] ^ (x[i - 1] >>> 30)) + i;
            // for >32 bit machines
            x[i] &= 0xffffffff;
        }
        p = 0;  q = 1;  r = M;
    }

    /**
     * 単一のint配列型のシードを使って、乱数ジェネレータのシードを設定する。
     * 
     * @param seeds 初期シード
     */
    synchronized public void setSeed(int[] seeds) {
        setSeed(19650218);
        int i = 1,  j = 0;
        for (int k = 0; k < Math.max(N, seeds.length); k++) {
            x[i] ^= (x[i - 1] ^ (x[i - 1] >>> 30)) * 1664525;
            x[i] += seeds[j] + j;

            // for >32 bit machines
            x[i] &= 0xffffffff;
            if (++i >= N) {
                x[0] = x[N - 1];
                i = 1;
            }
            if (++j >= seeds.length) {
                j = 0;
            }
        }
        for (int k = 0; k < N - 1; k++) {
            x[i] ^= (x[i - 1] ^ (x[i - 1] >>> 30)) * 1566083941;
            x[i] -= i;

            // for >32 bit machines
            x[i] &= 0xffffffff;
            if (++i >= N) {
                x[0] = x[N - 1];  
                i = 1;  
            }
        }
        x[0] = 0x80000000;
    }

    /* (non-Javadoc)
     * @see java.util.Random#next(int)
     */
    @Override
    synchronized protected int next(int bits) {
        int y = (x[p] & UPPER_MASK) | (x[q] & LOWER_MASK);
        y = x[p] = x[r] ^ (y >>> 1) ^ ((y & 1) * MATRIX_A);
        if (++p == N) p = 0;
        if (++q == N) q = 0;
        if (++r == N) r = 0;

        y ^= (y >>> 11);
        y ^= (y  <<  7) & 0x9D2C5680;
        y ^= (y  << 15) & 0xEFC60000;
        y ^= (y >>> 18);
        return (y >>> (32 - bits));
    }
}
