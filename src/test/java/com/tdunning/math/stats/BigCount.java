package com.tdunning.math.stats;

import org.junit.Test;

public abstract class BigCount extends AbstractTest {

    @Test
    public void testBigMerge() {
        TDigest digest = createDigest();
        for (int i = 0; i < 5; i++) {
            digest.add(getDigest());
            double actual = digest.quantile(0.5);
            assertEquals("Count = " + digest.size(), 3000,
                    actual, 0.001);
        }
    }

    private TDigest getDigest() {
        TDigest digest = createDigest();
        addData(digest);
        return digest;
    }

    public TDigest createDigest() {
        throw new IllegalStateException("Should have over-ridden createDigest");
    }

    private static void addData(TDigest digest) {
        digest.add(10, 300000000);
        digest.add(200, 300000000);
        digest.add(3000, 300000000);
        digest.add(4000, 300000000);
        digest.add(5000, 300000000);
        digest.add(47883554, 200);
    }
}