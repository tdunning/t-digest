/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tdunning.math.stats;

import com.google.common.collect.Lists;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ArrayDigestTest extends TDigestTest {
    @BeforeClass
    public static void setup() throws IOException {
        TDigestTest.setup("array");
    }

    protected DigestFactory factory(double compression) {
        return new DigestFactory() {
            @Override
            public ArrayDigest create() {
                int pageSize = randomIntBetween(4, 50);
                return TDigest.createArrayDigest(pageSize, 100);
            }
        };
    }

    @Override
    protected TDigest fromBytes(ByteBuffer bytes) {
        return ArrayDigest.fromBytes(bytes);
    }

    @Test
    public void testBadPage() {
        try {
            TDigest.createArrayDigest(3, 100);
            fail("Should have caught bad page size");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("Must have page size"));
        }
    }

    public static class XW implements Comparable<XW> {
        private static AtomicInteger idCount = new AtomicInteger();

        int id = idCount.incrementAndGet();
        double x;
        int w;

        public XW(double x, int w) {
            this.x = x;
            this.w = w;
        }

        @Override
        public int compareTo(XW o) {
            int r = Double.compare(x, o.x);
            if (r == 0) {
                return id - o.id;
            } else {
                return r;
            }
        }

        @Override
        public String toString() {
            return "XW{" +
                    "x=" + x +
                    ", w=" + w +
                    '}';
        }
    }

    // verifies that the data that we add is preserved
    @Test
    public void testAddIterate() {
        final ArrayDigest ad = (ArrayDigest) factory(100).create();

        assertEquals("[]", Lists.newArrayList(ad.centroids()).toString());

        List<XW> ref = Lists.newArrayList(new XW(0.5, 1));
        ad.addRaw(0.5, 1);
        assertEquals("[Centroid{centroid=0.5, count=1}]", Lists.newArrayList(ad.centroids()).toString());

        Random random = new Random();
        int totalWeight = 1;
        for (int i = 0; i < 1000; i++) {
            double x = random.nextDouble();
            ad.addRaw(x, 1);
            totalWeight++;
            ref.add(new XW(x, 1));
        }

        assertEquals(totalWeight, ad.size());
        assertEquals(1001, ad.centroids().size());

        for (int i = 0; i < 1000; i++) {
            int w = random.nextInt(5) + 2;
            double x = random.nextDouble();
            ad.addRaw(x, w);
            totalWeight += w;
            ref.add(new XW(x, w));
        }

        assertEquals(totalWeight, ad.size());
        assertEquals(2001, ad.centroids().size());


        Collections.sort(ref);
        Iterator<XW> ix = ref.iterator();
        int i = 0;
        for (Centroid c : ad.centroids()) {
            XW expected = ix.next();
            assertEquals("mean " + i, expected.x, c.mean(), 1e-15);
            assertEquals("weight " + i, expected.w, c.count());
            i++;
        }

        assertEquals(0, Lists.newArrayList(ad.allBefore(0)).size());
        assertEquals(ad.centroids().size(), Lists.newArrayList(ad.allBefore(1)).size());

        assertEquals(0, Lists.newArrayList(ad.allAfter(1)).size());
        assertEquals(ad.centroids().size(), Lists.newArrayList(ad.allAfter(0)).size());

        for (int k = 0; k < 1000; k++) {
            final double split = random.nextDouble();
            List<ArrayDigest.Index> z1 = Lists.newArrayList(ad.allBefore(split));
            i = 0;
            for (ArrayDigest.Index index : z1) {
                assertTrue("Check value before split " + i + " " + ad.mean(index), ad.mean(index) < split);
                i++;
            }

            List<ArrayDigest.Index> z2 = Lists.newArrayList(ad.allAfter(split));
            i = 0;
            for (ArrayDigest.Index index : z2) {
                assertTrue("Check value after split " + i + " " + ad.mean(index), ad.mean(index) > split);
                i++;
            }

            assertEquals("Bad counts for split " + split, ad.centroids().size(), z1.size() + z2.size());
        }
    }

    @Test
    public void testInternalSums() {
        Random random = new Random();
        ArrayDigest ad = (ArrayDigest) factory(100).create();
        for (int i = 0; i < 1000; i++) {
            ad.add(random.nextDouble(), 7);
        }

        for (int i = 0; i < 11; i++) {
            ArrayDigest.Index floor = ad.floor(i / 10.0);
            System.out.printf("%3.1f\t%.3f\n", i / 10.0, (double) ad.headSum(floor) / ad.size());
            assertEquals(i / 10.0, (double) ad.headSum(floor) / ad.size(), 0.15);
        }
    }
}
