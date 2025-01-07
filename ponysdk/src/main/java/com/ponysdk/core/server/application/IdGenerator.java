package com.ponysdk.core.server.application;

import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;

class IdGenerator {

    private static final int P = Integer.MAX_VALUE; //Mersenne prime number M31
    private static final int[] FACTORS = computeOrderFactors(); //prime divisors of P-1
    private static final int ROOT = findPrimitiveRoot();

    private int baseValue = ThreadLocalRandom.current().nextInt(1, P);
    private final long[] multipliers = new long[65];
    private long used = 0L;

    public IdGenerator() {
        int exponent;
        do {
            exponent = ThreadLocalRandom.current().nextInt(1, P - 1);
        } while (!validateExponent(exponent));
        int m = expmod(ROOT, exponent);
        multipliers[0] = 1;
        for (int i = 1; i < multipliers.length; i++) {
            multipliers[i] = (multipliers[i - 1] * m) % P;
        }
    }

    public int nextID() {
        if (used == -1L) {
            //jump to next 64-block if this block has been consumed
            baseValue = (int) ((baseValue * multipliers[64]) % P);
            used = 0L;
        }
        int bit = nextBit(used);
        used |= 1L << bit;
        return 1 + (int) ((baseValue * multipliers[bit]) % P);
    }

    static int[] computeOrderFactors() {
        TreeSet<Integer> factors = new TreeSet<>();
        int i = 2;
        int remaining = P - 1;
        while (i * i <= remaining) {
            if (remaining % i != 0) {
                i++;
            } else {
                factors.add(i);
                remaining /= i;
            }
        }
        if (remaining > 1) factors.add(remaining);
        return factors.stream().mapToInt(Integer::intValue).toArray();
    }

    static int findPrimitiveRoot() {
        int r = 2;
        while (true) {
            if (isPrimitiveRoot(r)) {
                return r;
            } else {
                r++;
            }
        }
    }

    private static boolean isPrimitiveRoot(int r) {
        // the order of r is a divisor of P-1, so do the test for every (P-1)/f where f is a prime factor of P-1
        for (int f : FACTORS) {
            if (expmod(r, (P - 1) / f) == 1) return false;
        }
        return true;
    }


    /**
     * @return a random bit index that is not already set in used (used should have at least one bit cleared)
     */
    static int nextBit(long used) { //non-private because of tests
        int bit = ThreadLocalRandom.current().nextInt(0, 64);
        if (((1L << bit) & used) != 0L) {
            //jump to next bit
            int shift = Long.numberOfTrailingZeros(~(used >>> bit));
            bit += shift;
            //if "next" cleared bit is the first one inserted because of shifting, return the lowest 0 of used
            if (bit == 64) bit = Long.numberOfTrailingZeros(~used);
        }
        return bit;
    }

    /**
     * @return true if the order of expmod(ROOT, e) is another primitive root
     */
    private static boolean validateExponent(int e) {
        for (int factor : FACTORS) {
            if (e % factor == 0) return false;
        }
        return true;
    }

    /**
     * @return base raised to the power of exponent, modulo P
     */
    private static int expmod(int base, int exponent) {
        // return (base ** exponent)  is 7 power exponent modulo MAX
        long b = base;
        long result = 1L;
        while (exponent > 0) {
            if ((exponent & 1) == 1) {
                result = (result * b) % P;
            }
            exponent >>= 1;
            b = (b * b) % P;
        }
        return (int) result;
    }

}
