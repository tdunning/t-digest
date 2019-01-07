/*
 * Licensed to Ted Dunning under one or more
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

import com.carrotsearch.randomizedtesting.annotations.Seed;
import com.google.common.collect.Lists;
import org.apache.mahout.common.RandomUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

//to freeze the tests with a particular seed, put the seed on the next line
//@Seed("84527677CF03B566:A6FF596BDDB2D59D")
@Seed("1CD6F48E8CA53BD1:379C5BDEB3A02ACB")
public class MergingDigestTest extends TDigestTest {
    @BeforeClass
    public static void setup() throws IOException {
        TDigestTest.setup("merge");
    }

    protected DigestFactory factory(final double compression) {
        return new DigestFactory() {
            @Override
            public TDigest create() {
                return new MergingDigest(4 * compression);
            }
        };
    }

    @Before
    public void testSetUp() {
        RandomUtils.useTestSeed();
    }

    /**
     * Validates the fast asin approximation
     */
    @Test
    public void testApproximation() {
        double worst = 0;
        double old = Double.NEGATIVE_INFINITY;
        for (double x = -1; x < 1; x += 0.00001) {
            double ex = Math.asin(x);
            double actual = ScaleFunction.fastAsin(x);
            double error = ex - actual;
//            System.out.printf("%.8f, %.8f, %.8f, %.12f\n", x, ex, actual, error * 1e6);
            assertEquals("Bad approximation", 0, error, 1e-6);
            assertTrue("Not monotonic", actual >= old);
            worst = Math.max(worst, Math.abs(error));
            old = actual;
        }
        assertEquals(Math.asin(1), ScaleFunction.fastAsin(1), 0);
        System.out.printf("worst = %.5g\n", worst);
    }

    //    @Test
    public void testFill() {
        int delta = 300;
        MergingDigest x = new MergingDigest(delta);
        Random gen = new Random();
        ScaleFunction scale = x.getScaleFunction();
        double compression = x.compression();
        for (int i = 0; i < 1000000; i++) {
            x.add(gen.nextGaussian());
        }
        double q0 = 0;
        int i = 0;
        System.out.printf("i, q, mean, count, dk\n");
        for (Centroid centroid : x.centroids()) {
            double q = q0 + centroid.count() / 2.0 / x.size();
            double q1 = q0 + (double) centroid.count() / x.size();
            double dk = scale.k(q1, compression, x.size()) - scale.k(q0, compression, x.size());
            if (centroid.count() > 1) {
                assertTrue(String.format("K-size for centroid %d at %.3f is %.3f", i, centroid.mean(), dk), dk <= 1);
            } else {
                dk = 1;
            }
            System.out.printf("%d,%.7f,%.7f,%d,%.7f\n", i, q, centroid.mean(), centroid.count(), dk);
            if (Double.isNaN(dk)) {
                System.out.printf(">>>> %.8f, %.8f\n", q0, q1);
            }
            q0 = q1;
            i++;
        }
    }

    @Test
    public void testSmallCountQuantile() {
        List<Double> data = Lists.newArrayList(15.0, 20.0, 32.0, 60.0);
        TDigest td = new MergingDigest(200);
        for (Double datum : data) {
            td.add(datum);
        }
        assertEquals(20, td.quantile(0.4), 1e-10);
        assertEquals(20, td.quantile(0.25), 1e-10);
        assertEquals(15, td.quantile(0.25 - 1e-10), 1e-10);
        assertEquals(20, td.quantile(0.5 - 1e-10), 1e-10);
        assertEquals(32, td.quantile(0.5), 1e-10);
    }

    /**
     * Tests cases where min or max is not the same as the extreme centroid
     * which has weight>1. In these cases min and max give us a little information
     * we wouldn't otherwise have.
     */
    @Test
    public void singletonAtEnd() {
        MergingDigest digest = new MergingDigest(100);
        digest.add(1);
        digest.add(2);
        digest.add(3);

        assertEquals(1, digest.getMin(), 0);
        assertEquals(3, digest.getMax(), 0);
        assertEquals(3, digest.centroidCount());
        assertEquals(0, digest.cdf(0), 0);
        assertEquals(0, digest.cdf(1 - 1e-9), 0);
        assertEquals(0.5 / 3, digest.cdf(1), 1e-10);
        assertEquals(1.0 / 3, digest.cdf(1 + 1e-10), 1e-10);
        assertEquals(2.0 / 3, digest.cdf(3 - 1e-9), 0);
        assertEquals(2.5 / 3, digest.cdf(3), 0);
        assertEquals(1.0, digest.cdf(3 + 1e-9), 0);

        digest.add(1);
        assertEquals(1.0 / 4, digest.cdf(1), 0);

        // normally min == mean[0] because weight[0] == 1
        // we can force this not to be true for testing
        digest = new MergingDigest(1);
        digest.setScaleFunction(ScaleFunction.K_0);
        for (int i = 0; i < 100; i++) {
            digest.add(1);
            digest.add(2);
            digest.add(3);
        }
        // This sample will be added to the first cluster that already exists
        // the effect will be to (slightly) nudge the mean of that cluster
        // but also decrease the min. As such, near q=0, cdf and quantiles
        // should reflect this single sample as a singleton
        digest.add(0);
        assertTrue(digest.centroidCount() > 0);
        Centroid first = digest.centroids().iterator().next();
        assertTrue(first.count() > 1);
        assertTrue(first.mean() > digest.getMin());
        assertEquals(0.0, digest.getMin(), 0);
        assertEquals(0, digest.cdf(0 - 1e-9), 0);
        assertEquals(0.5 / digest.size(), digest.cdf(0), 1e-10);
        assertEquals(1.0 / digest.size(), digest.cdf(1e-9), 1e-10);

        assertEquals(0, digest.quantile(0), 0);
        assertEquals(0, digest.quantile(0.5 / digest.size()), 0);
        assertEquals(0, digest.quantile(1.0 / digest.size() - 1e-10), 0);
        assertEquals(0, digest.quantile(1.0 / digest.size()), 0);
        assertEquals(2.0 / first.count() / 100, digest.quantile(1.01 / digest.size()), 2e-5);
        assertEquals(first.mean(), digest.quantile(first.count() / 2.0 / digest.size()), 1e-5);

        digest.add(4);
        Centroid last = Lists.reverse(Lists.newArrayList(digest.centroids())).iterator().next();
        assertTrue(last.count() > 1);
        assertTrue(last.mean() < digest.getMax());
        assertEquals(1.0, digest.cdf(digest.getMax() + 1e-9), 0);
        assertEquals(1 - 0.5 / digest.size(), digest.cdf(digest.getMax()), 0);
        assertEquals(1 - 1.0 / digest.size(), digest.cdf((digest.getMax() - 1e-9)), 1e-10);

        assertEquals(4, digest.quantile(1), 0);
        assertEquals(4, digest.quantile(1 - 0.5 / digest.size()), 0);
        assertEquals(4, digest.quantile(1 - 1.0 / digest.size() + 1e-10), 0);
        assertEquals(4, digest.quantile(1 - 1.0 / digest.size()), 0);
        double slope = 1.0 / (last.count() / 2.0 - 1) * (digest.getMax() - last.mean());
        double x = 4 - digest.quantile(1 - 1.01 / digest.size());
        assertEquals(slope * 0.01, x, 1e-10);
        assertEquals(last.mean(), digest.quantile(1 - last.count() / 2.0 / digest.size()), 1e-10);
    }

    /**
     * Brute force test that cdf and quantile give reference behavior in digest made up of all singletons.
     */
    @Test
    public void singletonQuantiles() {
        double[] data = new double[20];
        MergingDigest digest = new MergingDigest(100);
        for (int i = 0; i < 20; i++) {
            digest.add(i);
            data[i] = i;
        }

        for (double x = digest.getMin() - 0.1; x <= digest.getMax() + 0.1; x += 1e-3) {
            assertEquals(Dist.cdf(x, data), digest.cdf(x), 0);
        }

        for (double q = 0; q <= 1; q += 1e-3) {
            assertEquals(Dist.quantile(q, data), digest.quantile(q), 0);
        }
    }

    /**
     * Verifies behavior involving interpolation (or lack of same, really) between singleton centroids.
     */
    @Test
    public void singleSingleRange() {
        MergingDigest digest = new MergingDigest(100);
        digest.add(1);
        digest.add(2);
        digest.add(3);

        // verify the cdf is a step between singletons
        assertEquals(0.5 / 3.0, digest.cdf(1), 0);
        assertEquals(1 / 3.0, digest.cdf(1 + 1e-10), 0);
        assertEquals(1 / 3.0, digest.cdf(2 - 1e-10), 0);
        assertEquals(1.5 / 3.0, digest.cdf(2), 0);
        assertEquals(2 / 3.0, digest.cdf(2 + 1e-10), 0);
    }

    /**
     * Verifies interpolation between a singleton and a larger centroid.
     */
    @Test
    public void singleMultiRange() {
        MergingDigest digest = new MergingDigest(10);
        digest.setScaleFunction(ScaleFunction.K_0);
        for (int i = 0; i < 100; i++) {
            digest.add(1);
            digest.add(2);
            digest.add(3);
        }
        // this check is, of course true, but it also forces merging before we change scale
        assertTrue(digest.centroidCount() < 300);
        digest.setScaleFunction(ScaleFunction.K_2);
        digest.add(0);
        // we now have a digest with a singleton first, then a heavier centroid next
        Iterator<Centroid> ix = digest.centroids().iterator();
        Centroid first = ix.next();
        Centroid second = ix.next();
        assertEquals(1, first.count());
        assertEquals(0, first.mean(), 0);
        assertTrue(second.count() > 1);
        assertEquals(1.0, second.mean(), 0);

        assertEquals(0.5 / digest.size(), digest.cdf(0), 0);
        assertEquals(1.0 / digest.size(), digest.cdf(1e-10), 1e-10);
        assertEquals((1 + second.count() / 8.0) / digest.size(), digest.cdf(0.25), 1e-10);
    }

    @Override
    protected TDigest fromBytes(ByteBuffer bytes) {
        return MergingDigest.fromBytes(bytes);
    }
}