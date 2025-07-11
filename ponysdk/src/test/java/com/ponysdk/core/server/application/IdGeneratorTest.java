package com.ponysdk.core.server.application;

import org.junit.Assert;
import org.junit.Test;

public class IdGeneratorTest {

    private final IdGenerator tested = new IdGenerator();

    @Test
    public void cycleLengthTest() {
        long count = 0L;
        int first = tested.nextID();
        while (count++ < Integer.MAX_VALUE + 64L) {
            if (tested.nextID() == first) {
                Assert.assertEquals("order: " + count, (1L + Integer.MAX_VALUE) / 64, count / 64);
                return;
            }
        }
        Assert.fail("no cycle");
    }

    @Test
    public void nextBitTest() {
        for (int i = 0; i < 100; i++) {
            long used = 0L;
            while (used != -1L) {
                int bit = IdGenerator.nextBit(used);
                Assert.assertEquals(Long.toHexString(used) + ',' + bit, 0, ((1L << bit) & used));
                used |= 1L << bit;
            }
        }
    }


}
