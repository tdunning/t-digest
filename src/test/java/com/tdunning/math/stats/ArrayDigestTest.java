package com.tdunning.math.stats;

import com.google.common.collect.Lists;
import junit.framework.TestCase;
import org.apache.mahout.common.RandomUtils;
import org.apache.mahout.math.jet.random.AbstractContinousDistribution;
import org.apache.mahout.math.jet.random.Gamma;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class ArrayDigestTest extends TestCase {
    public void testBadPage() {
        try {
            new ArrayDigest(3, 100);
            fail("Should have caught bad page size");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().startsWith("Must have page size"));
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
    public void testAddIterate() {
        final ArrayDigest ad = new ArrayDigest(32, 100);

        Assert.assertEquals("[]", Lists.newArrayList(ad.centroids()).toString());

        List<XW> ref = Lists.newArrayList(new XW(0.5, 1));
        ad.addRaw(0.5, 1);
        Assert.assertEquals("[Centroid{centroid=0.5, count=1}]", Lists.newArrayList(ad.centroids()).toString());

        Random random = new Random();
        int totalWeight = 1;
        for (int i = 0; i < 1000; i++) {
            double x = random.nextDouble();
            ad.addRaw(x, 1);
            totalWeight++;
            ref.add(new XW(x, 1));
        }

        Assert.assertEquals(totalWeight, ad.size());
        Assert.assertEquals(1001, ad.centroidCount());

        for (int i = 0; i < 1000; i++) {
            int w = random.nextInt(5) + 2;
            double x = random.nextDouble();
            ad.addRaw(x, w);
            totalWeight += w;
            ref.add(new XW(x, w));
        }

        Assert.assertEquals(totalWeight, ad.size());
        Assert.assertEquals(2001, ad.centroidCount());


        Collections.sort(ref);
        Iterator<XW> ix = ref.iterator();
        int i = 0;
        for (Centroid c : ad.centroids()) {
            XW expected = ix.next();
            Assert.assertEquals("mean " + i, expected.x, c.mean(), 1e-15);
            Assert.assertEquals("weight " + i, expected.w, c.count());
            i++;
        }

        assertEquals(0, Lists.newArrayList(ad.before(0)).size());
        assertEquals(ad.centroidCount(), Lists.newArrayList(ad.before(1)).size());

        assertEquals(0, Lists.newArrayList(ad.after(1)).size());
        assertEquals(ad.centroidCount(), Lists.newArrayList(ad.after(0)).size());

        for (int k = 0; k < 1000; k++) {
            final double split = random.nextDouble();
            List<ArrayDigest.Index> z1 = Lists.newArrayList(ad.before(split));
            i = 0;
            for (ArrayDigest.Index index : z1) {
                Assert.assertTrue("Check value before split " + i + " " + ad.mean(index), ad.mean(index) < split);
                i++;
            }

            List<ArrayDigest.Index> z2 = Lists.newArrayList(ad.after(split));
            i = 0;
            for (ArrayDigest.Index index : z2) {
                Assert.assertTrue("Check value after split " + i + " " + ad.mean(index), ad.mean(index) > split);
                i++;
            }

            Assert.assertEquals("Bad counts for split " + split, ad.centroidCount(), z1.size() + z2.size());
        }
    }

    public void testInternalSums() {
        Random random = new Random();
        ArrayDigest ad = new ArrayDigest(32, 100);
        for (int i = 0; i < 1000; i++) {
            ad.add(random.nextDouble(), 7);
        }

        for (int i = 0; i < 11; i++) {
            ArrayDigest.Index floor = ad.floor(i / 10.0);
            System.out.printf("%3.1f\t%.3f\n", i / 10.0, (double) ad.headSum(floor) / ad.size());
        }
    }

    @Test
    public void testGamma() {
        // this Gamma distribution is very heavily skewed.  The 0.1%-ile is 6.07e-30 while
        // the median is 0.006 and the 99.9th %-ile is 33.6 while the mean is 1.
        // this severe skew means that we have to have positional accuracy that
        // varies by over 11 orders of magnitude.
        Random gen = RandomUtils.getRandom();
        for (int i = 0; i < 10; i++) {
            runTest(new Gamma(0.1, 0.1, gen), 100,
//                    new double[]{6.0730483624079e-30, 6.0730483624079e-20, 6.0730483627432e-10, 5.9339110446023e-03,
//                            2.6615455373884e+00, 1.5884778179295e+01, 3.3636770117188e+01},
                    new double[]{0.001, 0.01, 0.1, 0.5, 0.9, 0.99, 0.999},
                    "gamma", false);
        }
    }

    /**
     * Builds estimates of the CDF of a bunch of data points and checks that the centroids are accurately
     * positioned.  Accuracy is assessed in terms of the estimated CDF which is much more stringent than
     * checking position of quantiles with a single value for desired accuracy.
     *
     * @param gen           Random number generator that generates desired values.
     * @param sizeGuide     Control for size of the histogram.
     * @param tag           Label for the output lines
     * @param recordAllData True if the internal histogrammer should be set up to record all data it sees for
     */
    private void runTest(AbstractContinousDistribution gen, double sizeGuide, double[] qValues, String tag, boolean recordAllData) {
        TDigest dist = new ArrayDigest(32, sizeGuide);
        if (recordAllData) {
            dist.recordAllData();
        }

        long t0 = System.nanoTime();
        List<Double> data = Lists.newArrayList();
        for (int i = 0; i < 100000; i++) {
            double x = gen.nextDouble();
            data.add(x);
            dist.add(x);
        }
        dist.compress();
        Collections.sort(data);

        double[] xValues = qValues.clone();
        for (int i = 0; i < qValues.length; i++) {
            double ix = data.size() * qValues[i] - 0.5;
            int index = (int) Math.floor(ix);
            double p = ix - index;
            xValues[i] = data.get(index) * (1 - p) + data.get(index + 1) * p;
        }

        double qz = 0;
        int iz = 0;
        for (Centroid centroid : dist.centroids()) {
            double q = (qz + centroid.count() / 2.0) / dist.size();
//            sizeDump.printf("%s\t%d\t%.6f\t%.3f\t%d\n", tag, iz, q, 4 * q * (1 - q) * dist.size() / dist.compression(), centroid.count());
            qz += centroid.count();
            iz++;
        }

        System.out.printf("# %fus per point\n", (System.nanoTime() - t0) * 1e-3 / 100000);
        System.out.printf("# %d centroids\n", dist.centroidCount());

        assertTrue("Summary is too large", dist.centroidCount() < 10 * sizeGuide);
        int softErrors = 0;
        for (int i = 0; i < xValues.length; i++) {
            double x = xValues[i];
            double q = qValues[i];
            double estimate = dist.cdf(x);
//            errorDump.printf("%s\t%s\t%.8g\t%.8f\t%.8f\n", tag, "cdf", x, q, estimate - q);
            assertEquals(q, estimate, 0.005);

//            estimate = cdf(dist.quantile(q), data);
//            errorDump.printf("%s\t%s\t%.8g\t%.8f\t%.8f\n", tag, "quantile", x, q, estimate - q);
//            if (Math.abs(q - estimate) > 0.005) {
//                softErrors++;
//            }
//            assertEquals(q, estimate, 0.012);
        }
        assertTrue(softErrors < 3);

        if (recordAllData) {
            Iterator<? extends Centroid> ix = dist.centroids().iterator();
            Centroid b = ix.next();
            Centroid c = ix.next();
            qz = b.count();
            while (ix.hasNext()) {
                Centroid a = b;
                b = c;
                c = ix.next();
                double left = (b.mean() - a.mean()) / 2;
                double right = (c.mean() - b.mean()) / 2;

                double q = (qz + b.count() / 2.0) / dist.size();
//                for (Double x : b.data()) {
//                    deviationDump.printf("%s\t%.5f\t%d\t%.5g\t%.5g\t%.5g\t%.5g\t%.5f\n", tag, q, b.count(), x, b.mean(), left, right, (x - b.mean()) / (right + left));
//                }
                qz += a.count();
            }
        }
    }

}
