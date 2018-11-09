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

import com.clearspring.analytics.stream.quantile.QDigest;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.tdunning.math.stats.serde.AVLTreeDigestCompactSerde;
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


/**
 * Base test case for TDigests, just extend this class and implement the abstract methods.
 */
@Ignore
public abstract class TDigestTest extends AbstractTest {
    private static final Integer lock = 3;
    private static PrintWriter sizeDump = null;
    private static PrintWriter errorDump = null;
    private static PrintWriter deviationDump = null;

    @BeforeClass
    public static void freezeSeed() {
        RandomUtils.useTestSeed();
    }

    public static void setup(String digestName) throws IOException {
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

    protected abstract TDigest fromBytes(ByteBuffer bytes);

    private static TDigest merge(Iterable<TDigest> subData, Random gen, TDigest r) {
        List<Centroid> centroids = new ArrayList<>();
        boolean recordAll = false;
        for (TDigest digest : subData) {
            for (Centroid centroid : digest.centroids()) {
                centroids.add(centroid);
            }
            recordAll |= digest.isRecording();
        }
        Collections.shuffle(centroids, gen);
        if (recordAll) {
            r.recordAllData();
        }

        for (Centroid c : centroids) {
            //noinspection StatementWithEmptyBody
            if (r.isRecording()) {
                // TODO should do something better here.
            }
            ((AbstractTDigest) r).add(c.mean(), c.count(), c);
        }
        return r;
    }

    private void merge(final DigestFactory factory) throws FileNotFoundException, InterruptedException, ExecutionException {
        final Random gen0 = getRandom();
        PrintWriter out = new PrintWriter(new File("merge.tsv"));
        out.printf("type\tparts\tq\te0\te1\te2\te2.rel\n");

        List<Callable<String>> tasks = Lists.newArrayList();
        for (int k = 0; k < repeats(); k++) {
            final int currentK = k;
            tasks.add(new Callable<String>() {
                final Random gen = new Random(gen0.nextLong());

                @Override
                public String call() throws Exception {
                    StringWriter s = new StringWriter();
                    PrintWriter out = new PrintWriter(s);

                    for (int parts : new int[]{2, 5, 10, 20, 50, 100}) {
                        List<Double> data = Lists.newArrayList();

                        TDigest dist = factory.create();
                        dist.recordAllData();

                        // we accumulate the data into multiple sub-digests
                        List<TDigest> subs = Lists.newArrayList();
                        for (int i = 0; i < parts; i++) {
                            subs.add(factory.create().recordAllData());
                        }

                        int[] cnt = new int[parts];
                        for (int i = 0; i < 100000; i++) {
                            double x = gen.nextDouble();
                            data.add(x);
                            dist.add(x);
                            subs.get(i % parts).add(x);
                            cnt[i % parts]++;
                        }
                        dist.compress();
                        Collections.sort(data);

                        // collect the raw data from the sub-digests
                        List<Double> data2 = Lists.newArrayList();
                        int i = 0;
                        int k = 0;
                        for (TDigest digest : subs) {
                            assertEquals("Sub-digest size check", cnt[i], digest.size());
                            int k2 = 0;
                            for (Centroid centroid : digest.centroids()) {
                                Iterables.addAll(data2, centroid.data());
                                assertEquals("Centroid consistency", centroid.count(), centroid.data().size());
                                k2 += centroid.data().size();
                            }
                            k += k2;
                            assertEquals("Sub-digest centroid sum check", cnt[i], k2);
                            i++;
                        }
                        assertEquals("Sub-digests don't add up to the right size", data.size(), k);

                        // verify that the raw data all got recorded
                        Collections.sort(data2);
                        assertEquals(data.size(), data2.size());
                        Iterator<Double> ix = data.iterator();
                        for (Double x : data2) {
                            assertEquals(ix.next(), x);
                        }

                        // now merge the sub-digests
                        TDigest dist2 = factory.create().recordAllData();
                        dist2.add(subs);

                        // verify the merged result has the right data
                        List<Double> data3 = Lists.newArrayList();
                        for (Centroid centroid : dist2.centroids()) {
                            Iterables.addAll(data3, centroid.data());
                        }
                        Collections.sort(data3);
                        assertEquals(data.size(), data3.size());
                        ix = data.iterator();
                        for (Double x : data3) {
                            assertEquals(ix.next(), x);
                        }

                        if (dist instanceof MergingDigest) {
                            ((MergingDigest) dist).checkWeights();
                            ((MergingDigest) dist2).checkWeights();
                            for (TDigest sub : subs) {
                                ((MergingDigest) sub).checkWeights();
                            }
                        }

                        for (double q : new double[]{0.001, 0.01, 0.1, 0.2, 0.3, 0.5}) {
                            double z = quantile(q, data);
                            double e1 = dist.quantile(q) - z;
                            double e2 = dist2.quantile(q) - z;
                            out.printf("quantile\t%d\t%.6f\t%.6f\t%.6f\t%.6f\t%.6f\n", parts, q, z - q, e1, e2, Math.abs(e2) / q);
                            assertTrue(String.format("Relative error: parts=%d, q=%.4f, e1=%.5f, e2=%.5f, rel=%.4f", parts, q, e1, e2, Math.abs(e2) / q), Math.abs(e2) / q < 0.3);
                            assertTrue(String.format("Absolute error: parts=%d, q=%.4f, e1=%.5f, e2=%.5f, rel=%.4f", parts, q, e1, e2, Math.abs(e2) / q), Math.abs(e2) < 0.015);
                        }

                        for (double x : new double[]{0.001, 0.01, 0.1, 0.2, 0.3, 0.5}) {
                            double z = cdf(x, data);
                            double e1 = dist.cdf(x) - z;
                            double e2 = dist2.cdf(x) - z;

                            out.printf("cdf\t%d\t%.6f\t%.6f\t%.6f\t%.6f\t%.6f\n", parts, x, z - x, e1, e2, Math.abs(e2) / x);
                            assertTrue(String.format("Absolute cdf: parts=%d, x=%.4f, e1=%.5f, e2=%.5f", parts, x, e1, e2), Math.abs(e2) < 0.015);
                            assertTrue(String.format("Relative cdf: parts=%d, x=%.4f, e1=%.5f, e2=%.5f, rel=%.3f", parts, x, e1, e2, Math.abs(e2) / x), Math.abs(e2) / x < 0.3);
                        }
                        out.flush();
                    }
                    System.out.printf("Iteration %d\n", currentK + 1);
                    out.close();
                    return s.toString();
                }
            });
        }

        ExecutorService executor = Executors.newFixedThreadPool(20);
        try {
            for (Future<String> result : executor.invokeAll(tasks)) {
                out.write(result.get());
            }
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
            out.close();
        }

    }

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
            double q1 = quantile(q, values);
            double q2 = digest.quantile(q);
            assertEquals(String.format("At q=%g, expected %.2f vs %.2f", q, q1, q2), q1, q2, 0.03);
        }
    }

    private double cdf(final double x, List<Double> data) {
        int n1 = 0;
        int n2 = 0;
        for (Double v : data) {
            n1 += (v < x) ? 1 : 0;
            n2 += (v <= x) ? 1 : 0;
        }
        return (n1 + n2) / 2.0 / data.size();
    }

    private double quantile(final double q, List<Double> data) {
        if (data.size() == 0) {
            return Double.NaN;
        }
        if (q == 1 || data.size() == 1) {
            return data.get(data.size() - 1);
        }
        double index = q * data.size();
        if (index < 0.5) {
            return data.get(0);
        } else if (data.size() - index < 0.5) {
            return data.get(data.size() - 1);
        } else {
            index -= 0.5;
            final int intIndex = (int) index;
            return data.get(intIndex + 1) * (index - intIndex) + data.get(intIndex) * (intIndex + 1 - index);
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
     * Builds estimates of the CDF of a bunch of data points and checks that the centroids are accurately
     * positioned.  Accuracy is assessed in terms of the estimated CDF which is much more stringent than
     * checking position of quantiles with a single value for desired accuracy.
     *
     * @param gen           Random number generator that generates desired values.
     * @param sizeGuide     Control for size of the histogram.
     * @param tag           Label for the output lines
     * @param recordAllData True if the internal histogrammer should be set up to record all data it sees for
     */
    private void runTest(DigestFactory factory, AbstractContinousDistribution gen, @SuppressWarnings("SameParameterValue") double sizeGuide, double[] qValues, String tag, boolean recordAllData) {
        TDigest dist = factory.create();
        if (recordAllData) {
            dist.recordAllData();
        }

        List<Double> data = Lists.newArrayList();
        for (int i = 0; i < 100000; i++) {
            double x = gen.nextDouble();
            data.add(x);
        }
        long t0 = System.nanoTime();
        int sumW = 0;
        for (double x : data) {
            dist.add(x);
            sumW++;
        }
        System.out.printf("# %fus per point\n", (System.nanoTime() - t0) * 1e-3 / 100000);
        System.out.printf("# %d centroids\n", dist.centroids().size());
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
            sizeDump.printf("%s\t%d\t%.6f\t%.3f\t%d\n", tag, iz, q, 4 * q * (1 - q) * dist.size() / dist.compression(), centroid.count());
            qz += centroid.count();
            iz++;
        }
        assertEquals(qz, dist.size(), 1e-10);
        assertEquals(iz, dist.centroids().size());

        assertTrue(String.format("Summary is too large (got %d, wanted < %.1f)", dist.centroids().size(), 20 * sizeGuide), dist.centroids().size() < 20 * sizeGuide);
        int softErrors = 0;
        for (int i = 0; i < xValues.length; i++) {
            double x = xValues[i];
            double q = qValues[i];
            double estimate = dist.cdf(x);
            errorDump.printf("%s\t%s\t%.8g\t%.8f\t%.8f\n", tag, "cdf", x, q, estimate - q);
            assertEquals(q, estimate, 0.005);

            estimate = cdf(dist.quantile(q), data);
            errorDump.printf("%s\t%s\t%.8g\t%.8f\t%.8f\n", tag, "quantile", x, q, estimate - q);
            if (Math.abs(q - estimate) > 0.005) {
                softErrors++;
            }
            assertEquals(q, estimate, 0.012);
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
            runTest(factory(), new Uniform(0, 1, gen), 100,
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
            runTest(factory(), new Gamma(0.1, 0.1, gen), 100,
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
            runTest(factory(), mix, 100, new double[]{0.001, 0.01, 0.1, 0.3, 0.5, 0.7, 0.9, 0.99, 0.999}, "mixture", false);
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

        TDigest dist = factory(1000).create();
        List<Double> data = Lists.newArrayList();
        for (int i1 = 0; i1 < 100000; i1++) {
            double x = mix.nextDouble();
            data.add(x);
        }

        long t0 = System.nanoTime();
        for (double x : data) {
            dist.add(x);
        }

        System.out.printf("# %fus per point\n", (System.nanoTime() - t0) * 1e-3 / 100000);
        System.out.printf("# %d centroids\n", dist.centroids().size());

        // I would be happier with 5x compression, but repeated values make things kind of weird
        assertTrue("Summary is too large: " + dist.centroids().size(), dist.centroids().size() < 10 * (double) 1000);

        // all quantiles should round to nearest actual value
        for (int i = 0; i < 10; i++) {
            double z = i / 10.0;
            // we skip over troublesome points that are nearly halfway between
            for (double delta : new double[]{0.01, 0.02, 0.03, 0.07, 0.08, 0.09}) {
                double q = z + delta;
                double cdf = dist.cdf(q);
                // we also relax the tolerances for repeated values
                assertEquals(String.format("z=%.1f, q = %.3f, cdf = %.3f", z, q, cdf), z + 0.05, cdf, 0.01);

                double estimate = dist.quantile(q);
                assertEquals(String.format("z=%.1f, q = %.3f, cdf = %.3f, estimate = %.3f", z, q, cdf, estimate), Math.rint(q * 10) / 10.0, estimate, 0.001);
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
                    }, 100, new double[]{0.001, 0.01, 0.1, 0.5, 0.9, 0.99, 0.999},
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
     * Does basic sanity testing for a particular small example that used to fail.
     * See https://github.com/addthis/stream-lib/issues/138
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
            AVLTreeDigestCompactSerde.encodeInt(buf, n);
        }

        buf.flip();

        for (int i = 0; i < 3000; i++) {
            int n = AVLTreeDigestCompactSerde.decodeInt(buf);
            assertEquals(String.format("%d:", i), ref.get(i).intValue(), n);
        }
    }

    @Test
    public void compareToQDigest() throws FileNotFoundException {
        Random rand = getRandom();
        try (PrintWriter out = new PrintWriter(new FileOutputStream("qd-tree-comparison.csv"))) {
            for (int i = 0; i < repeats(); i++) {
                compareQD(out, new Gamma(0.1, 0.1, rand), "gamma", 1L << 48);
                compareQD(out, new Uniform(0, 1, rand), "uniform", 1L << 48);
            }
        }
    }

    private void compareQD(PrintWriter out, AbstractContinousDistribution gen, String tag, long scale) {
        for (double compression : new double[]{10, 20, 50, 100, 200, 500, 1000, 2000}) {
            QDigest qd = new QDigest(compression);
            TDigest dist = factory(compression).create();
            List<Double> data = Lists.newArrayList();
            for (int i = 0; i < 100000; i++) {
                double x = gen.nextDouble();
                dist.add(x);
                qd.offer((long) (x * scale));
                data.add(x);
            }
            dist.compress();
            Collections.sort(data);

            for (double q : new double[]{0.001, 0.01, 0.1, 0.2, 0.3, 0.5, 0.7, 0.8, 0.9, 0.99, 0.999}) {
                double x1 = dist.quantile(q);
                double x2 = (double) qd.getQuantile(q) / scale;
                double e1 = cdf(x1, data) - q;
                out.printf("%s\t%.0f\t%.8f\t%.10g\t%.10g\t%d\t%d\n", tag, compression, q, e1, cdf(x2, data) - q, dist.smallByteSize(), QDigest.serialize(qd).length);

            }
        }
    }

    @Test
    public void compareToStreamingQuantile() throws FileNotFoundException {
        Random rand = getRandom();

        try (PrintWriter out = new PrintWriter(new FileOutputStream("sq-tree-comparison.csv"))) {
            for (int i = 0; i < repeats(); i++) {
                compareSQ(out, new Gamma(0.1, 0.1, rand), "gamma", 1L << 48);
                compareSQ(out, new Uniform(0, 1, rand), "uniform", 1L << 48);
            }
        }
    }

    private void compareSQ(PrintWriter out, AbstractContinousDistribution gen, String tag, long scale) {
        double[] quantiles = {0.001, 0.01, 0.1, 0.2, 0.3, 0.5, 0.7, 0.8, 0.9, 0.99, 0.999};
        for (double compression : new double[]{10, 20, 50, 100, 200, 500, 1000, 2000}) {
            QuantileEstimator sq = new QuantileEstimator(1001);
            TDigest dist = factory(compression).create();
            List<Double> data = Lists.newArrayList();
            for (int i = 0; i < 100000; i++) {
                double x = gen.nextDouble();
                dist.add(x);
                sq.add(x);
                data.add(x);
            }
            dist.compress();
            Collections.sort(data);

            List<Double> qz = sq.getQuantiles();
            for (double q : quantiles) {
                double x1 = dist.quantile(q);
                double x2 = qz.get((int) (q * 1000 + 0.5));
                double e1 = cdf(x1, data) - q;
                double e2 = cdf(x2, data) - q;
                out.printf("%s\t%.0f\t%.8f\t%.10g\t%.10g\t%d\t%d\n",
                        tag, compression, q, e1, e2, dist.smallByteSize(), sq.serializedSize());

            }
        }
    }

    @Test()
    public void testSizeControl() throws IOException, InterruptedException, ExecutionException {
        // very slow running data generator.  Don't want to run this normally.  To run slow tests use
        // mvn test -DrunSlowTests=true
        assumeTrue(Boolean.parseBoolean(System.getProperty("runSlowTests")));

        final Random gen0 = getRandom();
        final PrintWriter out = new PrintWriter(new FileOutputStream("scaling.tsv"));
        out.printf("k\tsamples\tcompression\tsize1\tsize2\n");

        List<Callable<String>> tasks = Lists.newArrayList();
        for (int k = 0; k < 20; k++) {
            for (final int size : new int[]{10, 100, 1000, 10000}) {
                final int currentK = k;
                tasks.add(new Callable<String>() {
                    final Random gen = new Random(gen0.nextLong());

                    @Override
                    public String call() throws Exception {
                        System.out.printf("Starting %d,%d\n", currentK, size);
                        StringWriter s = new StringWriter();
                        PrintWriter out = new PrintWriter(s);
                        for (double compression : new double[]{2, 5, 10, 20, 50, 100, 200, 500, 1000}) {
                            TDigest dist = factory(compression).create();
                            for (int i = 0; i < size * 1000; i++) {
                                dist.add(gen.nextDouble());
                            }
                            out.printf("%d\t%d\t%.0f\t%d\t%d\n", currentK, size, compression, dist.smallByteSize(), dist.byteSize());
                            out.flush();
                        }
                        out.close();
                        return s.toString();
                    }
                });
            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(20);
        for (Future<String> result : executor.invokeAll(tasks)) {
            out.write(result.get());
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        out.close();
    }

    @Test
    public void testScaling() throws FileNotFoundException, InterruptedException, ExecutionException {
        final Random gen0 = getRandom();

        try (PrintWriter out = new PrintWriter(new FileOutputStream("error-scaling.tsv"))) {
            out.printf("pass\tcompression\tq\terror\tsize\n");

            Collection<Callable<String>> tasks = Lists.newArrayList();
            int n = Math.max(3, repeats() * repeats());
            for (int k = 0; k < n; k++) {
                final int currentK = k;
                tasks.add(new Callable<String>() {
                    final Random gen = new Random(gen0.nextLong());

                    @Override
                    public String call() throws Exception {
                        System.out.printf("Start %d\n", currentK);
                        StringWriter s = new StringWriter();
                        PrintWriter out = new PrintWriter(s);

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
                                out.printf("%d\t%.0f\t%.3f\t%.9f\t%d\n", currentK, compression, q, estimate - actual, dist.byteSize());
                                out.flush();
                            }
                        }
                        out.close();
                        System.out.printf("Finish %d\n", currentK);

                        return s.toString();
                    }
                });
            }

            ExecutorService exec = Executors.newFixedThreadPool(16);
            try {
                for (Future<String> result : exec.invokeAll(tasks)) {
                    out.write(result.get());
                }
                exec.shutdown();
                if (exec.awaitTermination(5, TimeUnit.SECONDS)) {
                    return;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Throwable e) {
                e.printStackTrace();
            }
            exec.shutdownNow();
            assertTrue("Dangling executor thread", exec.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void testMerge() throws FileNotFoundException, InterruptedException, ExecutionException {
        merge(factory());
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
            assertEquals(String.format("q=%.2f ", q), quantile(q, values), digest.quantile(q), 0.01);
        }
    }

    @Test
    public void testMontonicity() throws Exception {
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

    private double ks(double[] data, int length, TDigest digest) {
        double d1 = 0;
        double d2 = 0;
        Arrays.sort(data, 0, length);
        int i = 0;
        for (Centroid centroid : digest.centroids()) {
            double x = centroid.mean();
            while (i < length && data[i] <= x) {
                i++;
            }
            double q0a = (double) i / (length - 1);
            double q0b = (double) (i + 1) / (length - 1);
            double q0;
            if (i > 0) {
                if (i < length) {
                    q0 = (q0a * (data[i] - x) + q0b * (x - data[i - 1])) / (data[i] - data[i - 1]);
                } else {
                    q0 = 1;
                }
            } else {
                q0 = 0;
            }
            double q1 = digest.cdf(x);
            d1 = Math.max(q1 - q0, d1);
            d2 = Math.max(q0 - q1, d2);
        }
        return Math.max(d1, d2);
    }
}
