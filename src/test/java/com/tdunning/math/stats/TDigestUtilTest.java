package com.tdunning.math.stats;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.google.common.collect.Lists;

public class TDigestUtilTest extends AbstractTest {

    @Test
    public void testIntEncoding() {
        Random gen = getRandom();
        ByteBuffer buf = ByteBuffer.allocate(10000);
        List<Integer> ref = Lists.newArrayList();
        for (int i = 0; i < 3000; i++) {
            int n = gen.nextInt();
            n = n >>> (i / 100);
            ref.add(n);
            AbstractTDigest.encode(buf, n);
        }

        buf.flip();

        for (int i = 0; i < 3000; i++) {
            int n = AbstractTDigest.decode(buf);
            assertEquals(String.format("%d:", i), ref.get(i).intValue(), n);
        }
    }
}