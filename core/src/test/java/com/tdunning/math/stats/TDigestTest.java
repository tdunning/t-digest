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

import com.google.common.collect.Lists;
import org.apache.mahout.common.RandomUtils;
import org.apache.mahout.math.jet.random.AbstractContinousDistribution;
import org.apache.mahout.math.jet.random.Gamma;
import org.apache.mahout.math.jet.random.Normal;
import org.apache.mahout.math.jet.random.Uniform;
import org.junit.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Base test case for TDigests, just extend this class and implement the abstract methods.
 */
@Ignore
public abstract class TDigestTest extends AbstractTest {
    private static final Integer lock = 3;
    private static PrintWriter sizeDump = null;
    private static PrintWriter errorDump = null;
    private static PrintWriter deviationDump = null;

    private static String digestName;

    @BeforeClass
    public static void freezeSeed() {
        RandomUtils.useTestSeed();
    }

    public static void setup(String digestName) throws IOException {
        TDigestTest.digestName = digestName;
        synchronized (lock) {
            sizeDump = new PrintWriter(new FileWriter("sizes-" + digestName + ".csv"));
            sizeDump.printf("tag\ti\tq\tk\tactual\n");

            errorDump = new PrintWriter((new FileWriter("errors-" + digestName + ".csv")));
            errorDump.printf("dist\ttag\tx\tQ\terror\n");

            deviationDump = new PrintWriter((new FileWriter("deviation-" + digestName + ".csv")));
            deviationDump.printf("tag\tQ\tk\tx\tmean\tleft\tright\tdeviation\n");
        }
    }

    @After
    public void flush() {
        synchronized (lock) {
            if (sizeDump != null) {
                sizeDump.flush();
            }
            if (errorDump != null) {
                errorDump.flush();
            }
            if (deviationDump != null) {
                deviationDump.flush();
            }
        }
    }

    @AfterClass
    public static void teardown() {
        if (sizeDump != null) {
            sizeDump.close();
        }
        if (errorDump != null) {
            errorDump.close();
        }
        if (deviationDump != null) {
            deviationDump.close();
        }
    }

    public interface DigestFactory {
        TDigest create();
    }

    protected abstract DigestFactory factory(double compression);

    private DigestFactory factory() {
        return factory(100);
    }

    @Test
    public void offsetUniform() {
        System.out.printf("delta, q, x1, x2, q1, q2, error_x, error_q\n");
        for (double compression : new double[]{20, 50, 100, 200}) {
            TDigest digest = factory(compression).create();
            digest.setScaleFunction(ScaleFunction.K_0);
            Random rand = new Random();
            AbstractContinousDistribution gen = new Uniform(50, 51, rand);
            double[] data = new double[1_000_000];
            for (int i = 0; i < 1_000_000; i++) {
                data[i] = gen.nextDouble();
                digest.add(data[i]);
            }
            Arrays.sort(data);
            for (double q : new double[]{0.5, 0.9, 0.99, 0.999, 0.9999, 0.99999, 0.999999}) {
                double x1 = Dist.quantile(q, data);
                double x2 = digest.quantile(q);
                double q1 = Dist.cdf(x1, data);
                double q2 = digest.cdf(x1);
                System.out.printf("%.0f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f\n",
                        compression, q, x1, x2, q1, q2, Math.abs(x1 - x2) / (1 - q), Math.abs(q1 - q2) / (1 - q)
                );
            }
        }
    }

    @Test
    public void writeUniformResultsWithCompression() {

        List<ScaleFunction> scaleFcns = Arrays.asList(ScaleFunction.K_0, ScaleFunction.K_1,
                ScaleFunction.K_2, ScaleFunction.K_3, ScaleFunction.K_1_GLUED,
                ScaleFunction.K_2_GLUED, ScaleFunction.K_3_GLUED, ScaleFunction.K_QUADRATIC);

        int trialSize = 1_000_000;

        Map<ScaleFunction, List<Integer>> centroidCounts= new HashMap<>();

        for (ScaleFunction scaleFcn : scaleFcns) {
            centroidCounts.put(scaleFcn, new ArrayList<Integer>());
            try {
                Map<Double, List<String>> records = new HashMap<>();
                double[] quants = new double[]{0.00001, 0.0001, 0.001, 0.01, 0.1,
                        0.5, 0.9, 0.99, 0.999, 0.9999, 0.99999};
                for (double q : quants) {
                    records.put(q, new ArrayList<String>());
                }
                for (int j = 0; j < 100; j++) {
                    for (double compression : new double[]{100}) {
                        TDigest digest = factory(compression).create();
                        digest.setScaleFunction(scaleFcn);
                        Random rand = new Random();
                        AbstractContinousDistribution gen = new Uniform(50, 51, rand);
                        double[] data = new double[trialSize];
                        for (int i = 0; i < trialSize; i++) {
                            data[i] = gen.nextDouble();
                            digest.add(data[i]);
                        }
                        Arrays.sort(data);
                        digest.compress();
                        for (double q : quants) {
                            double x1 = Dist.quantile(q, data);
                            double q1 = Dist.cdf(x1, data);
                            double q2 = digest.cdf(x1);
                            records.get(q).add(String.valueOf(Math.abs(q1 - q2)) + "," +
                                    String.valueOf(Math.abs(q1 - q2) / Math.min(q, 1 - q)) + "\n");
                        }
                        centroidCounts.get(scaleFcn).add(digest.centroids().size());
                    }
                }

                String fcnName;

                if (scaleFcn.toString().endsWith("GLUED") || scaleFcn.toString().endsWith("QUADRATIC")) {
                    fcnName = scaleFcn.toString() ;
                } else {
                    fcnName = scaleFcn.toString() +  "_USUAL";
                }

                for (double q : quants) {
                    FileWriter csvWriter = new FileWriter("../docs/asymmetric/data/tree/" + fcnName + "_" + String.valueOf(q) + ".csv");
                    csvWriter.append("error_q,norm_error_q\n");
                    for (String obs : records.get(q)) {
                        csvWriter.append(obs);
                    }
                    csvWriter.flush();
                    csvWriter.close();
                }

                FileWriter csvWriter = new FileWriter("../docs/asymmetric/data/tree/" + fcnName + "_centroid_counts.csv");
                csvWriter.append("centroid_count\n");
                for (Integer ct : centroidCounts.get(scaleFcn)) {
                    csvWriter.append(ct.toString()).append("\n");
                }
                csvWriter.flush();
                csvWriter.close();

                } catch (IOException e) {
                    return;
                }
            }
        }

    @Test
    public void bigJump() {
        TDigest digest = factory(100).create();
        for (int i = 1; i < 20; i++) {
            digest.add(i);
        }
        digest.add(1_000_000);

        assertEquals(18, digest.quantile(0.89999999), 0);
        assertEquals(19, digest.quantile(0.9), 0);
        assertEquals(19, digest.quantile(0.949999999), 0);
        assertEquals(1_000_000, digest.quantile(0.95), 0);

        assertEquals(0.925, digest.cdf(19), 1e-11);
        assertEquals(0.95, digest.cdf(19.0000001), 1e-11);
        assertEquals(0.9, digest.cdf(19 - 0.0000001), 1e-11);

        digest = factory(80).create();
        digest.setScaleFunction(ScaleFunction.K_0);

        for (int j = 0; j < 100; j++) {
            for (int i = 1; i < 20; i++) {
                digest.add(i);
            }
            digest.add(1_000_000);
        }
        assertEquals(18.0, digest.quantile(0.885), 0.0);
        assertEquals(19.0, digest.quantile(0.915), 0.0);
        assertEquals(19.0, digest.quantile(0.935), 0.0);
        assertEquals(1_000_000.0, digest.quantile(0.965), 0.0);
    }

    @Test
    public void testSmallCountQuantile() {
        List<Double> data = Lists.newArrayList(15.0, 20.0, 32.0, 60.0);
        TDigest td = factory(200).create();
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
     * Brute force test that cdf and quantile give reference behavior in digest made up of all singletons.
     */
    @Test
    public void singletonQuantiles() {
        double[] data = new double[20];
        TDigest digest = factory(100).create();
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
        TDigest digest = factory(100).create();
        digest.add(1);
        digest.add(2);
        digest.add(3);

        // verify the cdf is a step between singletons
        assertEquals(0.5 / 3.0, digest.cdf(1), 0);
        assertEquals(1 / 3.0, digest.cdf(1 + 1e-10), 0);
        assertEquals(1 / 3.0, digest.cdf(2 - 1e-10), 0);
        assertEquals(1.5 / 3.0, digest.cdf(2), 0);
        assertEquals(2 / 3.0, digest.cdf(2 + 1e-10), 0);
        assertEquals(2 / 3.0, digest.cdf(3 - 1e-10), 0);
        assertEquals(2.5 / 3.0, digest.cdf(3), 0);
        assertEquals(1.0, digest.cdf(3 + 1e-10), 0);
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

    /**
     * Tests cases where min or max is not the same as the extreme centroid which has weight>1. In these cases min and
     * max give us a little information we wouldn't otherwise have.
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
        assertEquals(2.0 / first.count() / 100, digest.quantile(1.01 / digest.size()), 5e-5);
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

    protected abstract TDigest fromBytes(ByteBuffer bytes);

    @Test
    public void testSingleValue() {
        final TDigest digest = factory().create();
        final double value = getRandom().nextDouble() * 1000;
        digest.add(value);
        final double q = getRandom().nextDouble();
        for (double qValue : new double[]{0, q, 1}) {
            assertEquals(value, digest.quantile(qValue), 0.001f);
        }
    }

    @Test
    public void testFewValues() {
        // When there are few values in the tree, quantiles should be exact
        final TDigest digest = factory().create();
        final Random r = getRandom();
        final int length = r.nextInt(10);
        final List<Double> values = new ArrayList<>();
        for (int i = 0; i < length; ++i) {
            final double value;
            if (i == 0 || r.nextBoolean()) {
                value = r.nextDouble() * 100;
            } else {
                // introduce duplicates
                value = values.get(i - 1);
            }
            digest.add(value);
            values.add(value);
        }
        Collections.sort(values);

        // for this value of the compression, the tree shouldn't have merged any node
        assertEquals(digest.centroids().size(), values.size());
        for (double q : new double[]{0, 1e-10, r.nextDouble(), 0.5, 1 - 1e-10, 1}) {
            double q1 = Dist.quantile(q, values);
            double q2 = digest.quantile(q);
            assertEquals(String.format("At q=%g, expected %.2f vs %.2f", q, q1, q2), q1, q2, 0.03);
        }
    }

    private int repeats() {
        return Boolean.parseBoolean(System.getProperty("runSlowTests")) ? 10 : 1;
    }

    public void testEmptyDigest() {
        TDigest digest = factory().create();
        assertEquals(0, digest.centroids().size());
        assertEquals(0, digest.centroids().size());
    }

    /**
     * Builds estimates of the CDF of a bunch of data points and checks that the centroids are accurately positioned.
     * Accuracy is assessed in terms of the estimated CDF which is much more stringent than checking position of
     * quantiles with a single value for desired accuracy.
     *
     * @param gen           Random number generator that generates desired values.
     * @param tag           Label for the output lines
     * @param recordAllData True if the internal histogrammer should be set up to record all data it sees for
     */
    private void runTest(DigestFactory factory, AbstractContinousDistribution gen, double[] qValues, String tag, boolean recordAllData) {
        TDigest dist = factory.create();
        if (recordAllData) {
            dist.recordAllData();
        }

        double[] data = new double[100000];
        for (int i = 0; i < 100000; i++) {
            double x = gen.nextDouble();
            data[i] = x;
        }
        long t0 = System.nanoTime();
        for (double x : data) {
            dist.add(x);
        }
        System.out.printf("# %fus per point\n", (System.nanoTime() - t0) * 1e-3 / 100000);
        System.out.printf("# %d centroids\n", dist.centroids().size());
        Arrays.sort(data);

        double[] xValues = qValues.clone();
        for (int i = 0; i < qValues.length; i++) {
            xValues[i] = Dist.quantile(qValues[i], data);
        }

        double qz = 0;
        int iz = 0;
        for (Centroid centroid : dist.centroids()) {
            double q = (qz + centroid.count() / 2.0) / dist.size();
            sizeDump.printf("%s\t%d\t%.6f\t%.3f\t%d\n", tag, iz, q, 4 * q * (1 - q) * dist.size() / dist.compression(), centroid.count());
            qz += centroid.count();
            iz++;
        }
        assertEquals(iz, dist.centroids().size());
        dist.compress();
        assertEquals(qz, dist.size(), 1e-10);

        assertTrue(String.format("Summary is too large (got %d, wanted <= %.1f)", dist.centroids().size(), dist.compression()), dist.centroids().size() <= dist.compression());
        int softErrors = 0;
        for (int i = 0; i < xValues.length; i++) {
            double x = xValues[i];
            double q = qValues[i];
            double estimate = dist.cdf(x);
            errorDump.printf("%s\t%s\t%.8g\t%.8f\t%.8f\n", tag, "cdf", x, q, estimate - q);
            assertEquals(q, estimate, 0.08);

            estimate = Dist.cdf(dist.quantile(q), data);
            errorDump.printf("%s\t%s\t%.8g\t%.8f\t%.8f\n", tag, "quantile", x, q, estimate - q);
            if (Math.abs(q - estimate) > 0.005) {
                softErrors++;
            }
            assertEquals(String.format("discrepancy %.5f vs %.5f @ %.5f", q, estimate, x), q, estimate, 0.012);
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
                for (Double x : b.data()) {
                    deviationDump.printf("%s\t%.5f\t%d\t%.5g\t%.5g\t%.5g\t%.5g\t%.5f\n", tag, q, b.count(), x, b.mean(), left, right, (x - b.mean()) / (right + left));
                }
                qz += a.count();
            }
        }
    }

    @Test
    public void testEmpty() {
        final TDigest digest = factory().create();
        final double q = getRandom().nextDouble();
        assertTrue(Double.isNaN(digest.quantile(q)));
    }

    @Test
    public void testMoreThan2BValues() {
        final TDigest digest = factory().create();
        Random gen = getRandom();
        for (int i = 0; i < 1000; ++i) {
            final double next = gen.nextDouble();
            digest.add(next);
        }
        for (int i = 0; i < 10; ++i) {
            final double next = gen.nextDouble();
            final int count = 1 << 28;
            digest.add(next, count);
        }
        assertEquals(1000 + 10L * (1 << 28), digest.size());
        assertTrue(digest.size() > Integer.MAX_VALUE);
        final double[] quantiles = new double[]{0, 0.1, 0.5, 0.9, 1, gen.nextDouble()};
        Arrays.sort(quantiles);
        double prev = Double.NEGATIVE_INFINITY;
        for (double q : quantiles) {
            final double v = digest.quantile(q);
            assertTrue(String.format("q=%.1f, v=%.4f, pref=%.4f", q, v, prev), v >= prev);
            prev = v;
        }
    }


    public void testSorted() {
        final TDigest digest = factory().create();
        Random gen = getRandom();
        for (int i = 0; i < 10000; ++i) {
            digest.add(gen.nextDouble(), 1 + gen.nextInt(10));
        }
        Centroid previous = null;
        for (Centroid centroid : digest.centroids()) {
            if (previous != null) {
                assertTrue(previous.mean() <= centroid.mean());
            }
            previous = centroid;
        }
    }

    @Test
    public void testNaN() {
        final TDigest digest = factory().create();
        Random gen = getRandom();
        final int iters = gen.nextInt(100);
        for (int i = 0; i < iters; ++i) {
            digest.add(gen.nextDouble(), 1 + gen.nextInt(10));
        }
        try {
            // both versions should fail
            if (gen.nextBoolean()) {
                digest.add(Double.NaN);
            } else {
                digest.add(Double.NaN, 1);
            }
            fail("NaN should be an illegal argument");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }


    @Test
    public void testUniform() {
        Random gen = getRandom();
        for (int i = 0; i < repeats(); i++) {
            runTest(factory(), new Uniform(0, 1, gen),
                    new double[]{0.001, 0.01, 0.1, 0.5, 0.9, 0.99, 0.999},
                    "uniform", true);
        }
    }

    @Test
    public void testGamma() {
        // this Gamma distribution is very heavily skewed.  The 0.1%-ile is 6.07e-30 while
        // the median is 0.006 and the 99.9th %-ile is 33.6 while the mean is 1.
        // this severe skew means that we have to have positional accuracy that
        // varies by over 11 orders of magnitude.
        Random gen = getRandom();
        for (int i = 0; i < repeats(); i++) {
            runTest(factory(200), new Gamma(0.1, 0.1, gen),
//                    new double[]{6.0730483624079e-30, 6.0730483624079e-20, 6.0730483627432e-10, 5.9339110446023e-03,
//                            2.6615455373884e+00, 1.5884778179295e+01, 3.3636770117188e+01},
                    new double[]{0.001, 0.01, 0.1, 0.5, 0.9, 0.99, 0.999},
                    "gamma", true);
        }
    }

    @Test
    public void testNarrowNormal() {
        // this mixture of a uniform and normal distribution has a very narrow peak which is centered
        // near the median.  Our system should be scale invariant and work well regardless.
        final Random gen = getRandom();
        AbstractContinousDistribution mix = new AbstractContinousDistribution() {
            final AbstractContinousDistribution normal = new Normal(0, 1e-5, gen);
            final AbstractContinousDistribution uniform = new Uniform(-1, 1, gen);

            @Override
            public double nextDouble() {
                double x;
                if (gen.nextDouble() < 0.5) {
                    x = uniform.nextDouble();
                } else {
                    x = normal.nextDouble();
                }
                return x;
            }
        };

        for (int i = 0; i < repeats(); i++) {
            runTest(factory(400), mix, new double[]{0.001, 0.01, 0.1, 0.3, 0.5, 0.7, 0.9, 0.99, 0.999}, "mixture", false);
        }
    }

    @Test
    public void testRepeatedValues() {
        final Random gen = getRandom();

        // 5% of samples will be 0 or 1.0.  10% for each of the values 0.1 through 0.9
        AbstractContinousDistribution mix = new AbstractContinousDistribution() {
            @Override
            public double nextDouble() {
                return Math.rint(gen.nextDouble() * 10) / 10.0;
            }
        };

        TDigest dist = factory(400).create();
        List<Double> data = Lists.newArrayList();
        for (int i1 = 0; i1 < 1000000; i1++) {
            double x = mix.nextDouble();
            data.add(x);
        }

        long t0 = System.nanoTime();
        for (double x : data) {
            dist.add(x);
        }

        System.out.printf("# %fus per point\n", (System.nanoTime() - t0) * 1e-3 / 1000000);
        System.out.printf("# %d centroids\n", dist.centroids().size());

        assertTrue("Summary is too large: " + dist.centroids().size(), dist.centroids().size() < dist.compression());

        // all quantiles should round to nearest actual value
        for (int i = 0; i < 10; i++) {
            double z = i / 10.0;
            // we skip over troublesome points that are nearly halfway between
            for (double delta : new double[]{0.01, 0.02, 0.03, 0.07, 0.08, 0.09}) {
                double q = z + delta;
                double cdf = dist.cdf(q);
                // we also relax the tolerances for repeated values
                assertEquals(String.format("z=%.1f, q = %.3f, cdf = %.3f", z, q, cdf), z + 0.05, cdf, 0.03);

                double estimate = dist.quantile(q);
                assertEquals(String.format("z=%.1f, q = %.3f, cdf = %.3f, estimate = %.3f", z, q, cdf, estimate),
                        Math.rint(q * 10) / 10.0, estimate, 0.02);
            }
        }
    }

    @Test
    public void testSequentialPoints() {
        for (int i = 0; i < repeats(); i++) {
            runTest(factory(), new AbstractContinousDistribution() {
                        double base = 0;

                        @Override
                        public double nextDouble() {
                            base += Math.PI * 1e-5;
                            return base;
                        }
                    }, new double[]{0.001, 0.01, 0.1, 0.5, 0.9, 0.99, 0.999},
                    "sequential", true);
        }
    }

    @Test
    public void testSerialization() {
        Random gen = getRandom();
        final double compression = 20 + randomDouble() * 100;
        TDigest dist = factory(compression).create();
        for (int i = 0; i < 100000; i++) {
            double x = gen.nextDouble();
            dist.add(x);
        }
        dist.compress();

        ByteBuffer buf = ByteBuffer.allocate(20000);
        dist.asBytes(buf);
        assertTrue(String.format("size is %d\n", buf.position()), buf.position() < 12000);
        assertEquals(dist.byteSize(), buf.position());

        System.out.printf("# big %d bytes\n", buf.position());

        buf.flip();
        TDigest dist2 = fromBytes(buf);
        assertEquals(dist.centroids().size(), dist2.centroids().size());
        assertEquals(dist.compression(), dist2.compression(), 1e-4);
        assertEquals(dist.size(), dist2.size());

        for (double q = 0; q < 1; q += 0.01) {
            assertEquals(dist.quantile(q), dist2.quantile(q), 1e-5);
        }

        Iterator<? extends Centroid> ix = dist2.centroids().iterator();
        for (Centroid centroid : dist.centroids()) {
            assertTrue(ix.hasNext());
            assertEquals(centroid.count(), ix.next().count());
        }
        assertFalse(ix.hasNext());

        buf.flip();
        dist.asSmallBytes(buf);
        assertTrue(buf.position() < 6000);
        System.out.printf("# small %d bytes\n", buf.position());

        buf.flip();
        dist2 = fromBytes(buf);
        assertEquals(dist.centroids().size(), dist2.centroids().size());
        assertEquals(dist.compression(), dist2.compression(), 1e-4);
        assertEquals(dist.size(), dist2.size());

        for (double q = 0; q < 1; q += 0.01) {
            assertEquals(dist.quantile(q), dist2.quantile(q), 1e-6);
        }

        ix = dist2.centroids().iterator();
        for (Centroid centroid : dist.centroids()) {
            assertTrue(ix.hasNext());
            assertEquals(centroid.count(), ix.next().count());
        }
        assertFalse(ix.hasNext());
    }

    /**
     * Does basic sanity testing for a particular small example that used to fail. See
     * https://github.com/addthis/stream-lib/issues/138
     */
    @Test
    public void testThreePointExample() {
        TDigest tdigest = factory(100).create();
        double x0 = 0.18615591526031494;
        double x1 = 0.4241943657398224;
        double x2 = 0.8813006281852722;

        tdigest.add(x0);
        tdigest.add(x1);
        tdigest.add(x2);

        double p10 = tdigest.quantile(0.1);
        double p50 = tdigest.quantile(0.5);
        double p90 = tdigest.quantile(0.9);
        double p95 = tdigest.quantile(0.95);
        double p99 = tdigest.quantile(0.99);

        assertTrue("ordering of quantiles", p10 <= p50);
        assertTrue("ordering of quantiles", p50 <= p90);
        assertTrue("ordering of quantiles", p90 <= p95);
        assertTrue("ordering of quantiles", p95 <= p99);

        assertEquals("Extreme quantiles", x0, p10, 0);
        assertEquals("Extreme quantiles", x2, p99, 0);

//        System.out.println("digest: " + tdigest.getClass());
//        System.out.println("p10: " + tdigest.quantile(0.1));
//        System.out.println("p50: " + tdigest.quantile(0.5));
//        System.out.println("p90: " + tdigest.quantile(0.9));
//        System.out.println("p95: " + tdigest.quantile(0.95));
//        System.out.println("p99: " + tdigest.quantile(0.99));
//        System.out.println();
    }

    @Test
    public void testSingletonInACrowd() {
        final double compression = 100;
        TDigest dist = factory(compression).create();
        for (int i = 0; i < 10000; i++) {
            dist.add(10);
        }
        dist.add(20);
        dist.compress();
        assertEquals(10.0, dist.quantile(0), 0);
        assertEquals(10.0, dist.quantile(0.5), 0);
        assertEquals(10.0, dist.quantile(0.8), 0);
        assertEquals(10.0, dist.quantile(0.9), 0);
        assertEquals(10.0, dist.quantile(0.99), 0);
        assertEquals(10.0, dist.quantile(0.999), 0);
        assertEquals(20.0, dist.quantile(1), 0);
    }

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

    @Test()
    public void testSizeControl() throws IOException, InterruptedException {
        // very slow running data generator.  Don't want to run this normally.  To run slow tests use
        // mvn test -DrunSlowTests=true
//        assumeTrue(Boolean.parseBoolean(System.getProperty("runSlowTests")));
        final AtomicInteger live = new AtomicInteger(0);
        final AtomicInteger pending = new AtomicInteger(0);

        final Random gen0 = getRandom();
        try (final PrintWriter out = new PrintWriter(new FileOutputStream(String.format("scaling-%s.tsv", digestName)))) {
            out.printf("k\tsamples\tcompression\tcentroids\tsize1\tsize2\n");

            List<Callable<String>> tasks = Lists.newArrayList();
            for (int k = 0; k < 5; k++) {
                for (final int size : new int[]{10, 100, 1000, 10000}) {
                    final int currentK = k;
                    tasks.add(new Callable<String>() {
                        final Random gen = new Random(gen0.nextLong());

                        @Override
                        public String call() {
                            System.out.printf("Starting %d,%d\n", currentK, size);
                            live.incrementAndGet();
                            try {
                                StringWriter s = new StringWriter();
                                PrintWriter out = new PrintWriter(s);
                                for (double compression : new double[]{50, 100, 200, 500}) {
                                    try {
                                        TDigest dist = factory(compression).create();
                                        for (int i = 0; i < size * 1000; i++) {
                                            dist.add(gen.nextDouble());
                                        }
                                        dist.compress();
                                        out.printf("%d\t%d\t%.0f\t%d\t%d\t%d\n", currentK, size, compression, dist.centroidCount(), dist.smallByteSize(), dist.byteSize());
                                        out.flush();
                                    } catch (Throwable e) {
                                        System.out.printf("                         Exception %s, %d, %.0f, %d\n", e.toString(), currentK, compression, size);
                                        throw e;
                                    }
                                }
                                out.close();
                                return s.toString();
                            } finally {
                                live.decrementAndGet();
                                pending.decrementAndGet();
                                System.out.printf("                   %d,%d (%d live threads, %d pending tasks)\n", currentK, size, live.get(), pending.get());
                            }
                        }
                    });
                }
            }
            pending.set(tasks.size());

            ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 2);
            for (Future<String> result : executor.invokeAll(tasks)) {
                try {
                    out.write(result.get());
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            executor.shutdownNow();
            assertTrue("Dangling executor thread", executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void testScaling() throws FileNotFoundException {
        final Random gen = getRandom();

        try (PrintWriter out = new PrintWriter(new FileOutputStream(String.format("error-scaling-%s.tsv", digestName)))) {
            out.printf("pass\tcompression\tq\terror\tsize\n");

            Collection<Callable<String>> tasks = Lists.newArrayList();
            for (int k = 0; k < 10; k++) {
                List<Double> data = Lists.newArrayList();
                for (int i = 0; i < 100000; i++) {
                    data.add(gen.nextDouble());
                }
                Collections.sort(data);

                for (double compression : new double[]{10, 20, 50, 100, 200, 500, 1000}) {
                    TDigest dist = factory(compression).create();
                    for (Double x : data) {
                        dist.add(x);
                    }
                    dist.compress();

                    for (double q : new double[]{0.001, 0.01, 0.1, 0.5}) {
                        double estimate = dist.quantile(q);
                        double actual = data.get((int) (q * data.size()));
                        out.printf("%d\t%.0f\t%.3f\t%.9f\t%d\n", k, compression, q, estimate - actual, dist.byteSize());
                        out.flush();
                    }
                }
            }
        }
    }

    @Test
    public void testExtremeQuantiles() {
        // t-digest shouldn't merge extreme nodes, but let's still test how it would
        // answer to extreme quantiles in that case ('extreme' in the sense that the
        // quantile is either before the first node or after the last one)
        TDigest digest = factory().create();
        digest.add(10, 3);
        digest.add(20, 1);
        digest.add(40, 5);
        // this group tree is roughly equivalent to the following sorted array:
        // [ ?, 10, ?, 20, ?, ?, 50, ?, ? ]
        // and we expect it to compute approximate missing values:
        // [ 5, 10, 15, 20, 30, 40, 50, 60, 70]
        List<Double> values = Arrays.asList(5., 10., 15., 20., 30., 35., 40., 45., 50.);
        for (double q : new double[]{1.5 / 9, 3.5 / 9, 6.5 / 9}) {
            assertEquals(String.format("q=%.2f ", q), Dist.quantile(q, values), digest.quantile(q), 0.01);
        }
    }

    @Test
    public void testMonotonicity() {
        TDigest digest = factory().create();
        final Random gen = getRandom();
        for (int i = 0; i < 100000; i++) {
            digest.add(gen.nextDouble());
        }

        double lastQuantile = -1;
        double lastX = -1;
        for (double z = 0; z <= 1; z += 1e-5) {
            double x = digest.quantile(z);
            assertTrue(x >= lastX);
            lastX = x;

            double q = digest.cdf(z);
            assertTrue(q >= lastQuantile);
            lastQuantile = q;
        }
    }

//    @Test
//    public void testKSDrift() {
//        final Random gen = getRandom();
//        int N1 = 50;
//        int N2 = 10000;
//        double[] data = new double[N1 * N2];
//        System.out.printf("rep,i,ks,class\n");
//        for (int rep = 0; rep < 5; rep++) {
//            TDigest digest = factory(200).create();
//            for (int i = 0; i < N1; i++) {
//                for (int j = 0; j < N2; j++) {
//                    double x = gen.nextDouble();
//                    data[i * N2 + j] = x;
//                    digest.add(x);
//                }
//                System.out.printf("%d,%d,%.7f,%s,%d\n", rep, i, ks(data, (i + 1) * N2, digest), digest.getClass().getSimpleName(), digest.centroidCount());
//            }
//        }
//    }

//    private double ks(double[] data, int length, TDigest digest) {
//        double d1 = 0;
//        double d2 = 0;
//        Arrays.sort(data, 0, length);
//        int i = 0;
//        for (Centroid centroid : digest.centroids()) {
//            double x = centroid.mean();
//            while (i < length && data[i] <= x) {
//                i++;
//            }
//            double q0a = (double) i / (length - 1);
//            double q0b = (double) (i + 1) / (length - 1);
//            double q0;
//            if (i > 0) {
//                if (i < length) {
//                    q0 = (q0a * (data[i] - x) + q0b * (x - data[i - 1])) / (data[i] - data[i - 1]);
//                } else {
//                    q0 = 1;
//                }
//            } else {
//                q0 = 0;
//            }
//            double q1 = digest.cdf(x);
//            d1 = Math.max(q1 - q0, d1);
//            d2 = Math.max(q0 - q1, d2);
//        }
//        return Math.max(d1, d2);
//    }
}
