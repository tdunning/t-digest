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

import com.clearspring.analytics.stream.quantile.QDigest;
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

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class TreeDigestTest extends TDigestTest {
    @BeforeClass
    public static void setup() throws IOException {
        TDigestTest.setup("tree");
    }

    private DigestFactory<TreeDigest> factory = new DigestFactory<TreeDigest>() {
        @Override
        public TreeDigest create() {
            return new TreeDigest(100);
        }
    };

    @Before
    public void testSetUp() {
        RandomUtils.useTestSeed();
    }

    @Test
    public void testUniform() {
        Random gen = RandomUtils.getRandom();
        for (int i = 0; i < repeats(); i++) {
            runTest(factory, new Uniform(0, 1, gen), 100,
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
        Random gen = RandomUtils.getRandom();
        for (int i = 0; i < repeats(); i++) {
            runTest(factory, new Gamma(0.1, 0.1, gen), 100,
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
        final Random gen = RandomUtils.getRandom();
        AbstractContinousDistribution mix = new AbstractContinousDistribution() {
            AbstractContinousDistribution normal = new Normal(0, 1e-5, gen);
            AbstractContinousDistribution uniform = new Uniform(-1, 1, gen);

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
            runTest(factory, mix, 100, new double[]{0.001, 0.01, 0.1, 0.3, 0.5, 0.7, 0.9, 0.99, 0.999}, "mixture", false);
        }
    }

    @Test
    public void testRepeatedValues() {
        final Random gen = RandomUtils.getRandom();

        // 5% of samples will be 0 or 1.0.  10% for each of the values 0.1 through 0.9
        AbstractContinousDistribution mix = new AbstractContinousDistribution() {
            @Override
            public double nextDouble() {
                return Math.rint(gen.nextDouble() * 10) / 10.0;
            }
        };

        TreeDigest dist = new TreeDigest((double) 1000);
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
        assertTrue("Summary is too large", dist.centroids().size() < 10 * (double) 1000);

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
            runTest(factory, new AbstractContinousDistribution() {
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
        Random gen = RandomUtils.getRandom();
        TreeDigest dist = new TreeDigest(100);
        for (int i = 0; i < 100000; i++) {
            double x = gen.nextDouble();
            dist.add(x);
        }
        dist.compress();

        ByteBuffer buf = ByteBuffer.allocate(20000);
        dist.asBytes(buf);
        assertTrue(buf.position() < 11000);
        assertEquals(buf.position(), dist.byteSize());
        buf.clear();

        dist.asSmallBytes(buf);
        assertTrue(buf.position() < 6000);
        assertEquals(buf.position(), dist.smallByteSize());

        System.out.printf("# big %d bytes\n", buf.position());

        buf.flip();
        TreeDigest dist2 = TreeDigest.fromBytes(buf);
        assertEquals(dist.centroids().size(), dist2.centroids().size());
        assertEquals(dist.compression(), dist2.compression(), 0);
        assertEquals(dist.size(), dist2.size());

        for (double q = 0; q < 1; q += 0.01) {
            assertEquals(dist.quantile(q), dist2.quantile(q), 1e-8);
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
        dist2 = TreeDigest.fromBytes(buf);
        assertEquals(dist.centroids().size(), dist2.centroids().size());
        assertEquals(dist.compression(), dist2.compression(), 0);
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

    @Test
    public void testIntEncoding() {
        Random gen = RandomUtils.getRandom();
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

    @Test
    public void compareToQDigest() throws FileNotFoundException {
        Random rand = RandomUtils.getRandom();
        PrintWriter out = new PrintWriter(new FileOutputStream("qd-tree-comparison.csv"));
        try {
            for (int i = 0; i < repeats(); i++) {
                compareQD(out, new Gamma(0.1, 0.1, rand), "gamma", 1L << 48);
                compareQD(out, new Uniform(0, 1, rand), "uniform", 1L << 48);
            }
        } finally {
            out.close();
        }
    }

    private void compareQD(PrintWriter out, AbstractContinousDistribution gen, String tag, long scale) {
        for (double compression : new double[]{2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000}) {
            QDigest qd = new QDigest(compression);
            TreeDigest dist = new TreeDigest(compression);
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
        Random rand = RandomUtils.getRandom();

        PrintWriter out = new PrintWriter(new FileOutputStream("sq-tree-comparison.csv"));
        try {

            for (int i = 0; i < repeats(); i++) {
                compareSQ(out, new Gamma(0.1, 0.1, rand), "gamma", 1L << 48);
                compareSQ(out, new Uniform(0, 1, rand), "uniform", 1L << 48);
            }
        } finally {
            out.close();
        }
    }

    private void compareSQ(PrintWriter out, AbstractContinousDistribution gen, String tag, long scale) {
        double[] quantiles = {0.001, 0.01, 0.1, 0.2, 0.3, 0.5, 0.7, 0.8, 0.9, 0.99, 0.999};
        for (double compression : new double[]{2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000}) {
            QuantileEstimator sq = new QuantileEstimator(1001);
            TreeDigest dist = new TreeDigest(compression);
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

        final Random gen0 = RandomUtils.getRandom();
        final PrintWriter out = new PrintWriter(new FileOutputStream("scaling-tree.tsv"));
        out.printf("k\tsamples\tcompression\tsize1\tsize2\n");

        List<Callable<String>> tasks = Lists.newArrayList();
        for (int k = 0; k < 20; k++) {
            for (final int size : new int[]{10, 100, 1000, 10000}) {
                final int currentK = k;
                tasks.add(new Callable<String>() {
                    Random gen = new Random(gen0.nextLong());

                    @Override
                    public String call() throws Exception {
                        System.out.printf("Starting %d,%d\n", currentK, size);
                        StringWriter s = new StringWriter();
                        PrintWriter out = new PrintWriter(s);
                        for (double compression : new double[]{2, 5, 10, 20, 50, 100, 200, 500, 1000}) {
                            TreeDigest dist = new TreeDigest(compression);
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

        for (Future<String> result : Executors.newFixedThreadPool(20).invokeAll(tasks)) {
            out.write(result.get());
        }

        out.close();
    }

    @Test
    public void testScaling() throws FileNotFoundException, InterruptedException, ExecutionException {
        final Random gen0 = RandomUtils.getRandom();

        PrintWriter out = new PrintWriter(new FileOutputStream("error-scaling.tsv"));
        try {
            out.printf("pass\tcompression\tq\terror\tsize\n");

            Collection<Callable<String>> tasks = Lists.newArrayList();
            int n = Math.max(3, repeats() * repeats());
            for (int k = 0; k < n; k++) {
                final int currentK = k;
                tasks.add(new Callable<String>() {
                    Random gen = new Random(gen0.nextLong());

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

                        for (double compression : new double[]{2, 5, 10, 20, 50, 100, 200, 500, 1000}) {
                            TreeDigest dist = new TreeDigest(compression);
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
            for (Future<String> result : exec.invokeAll(tasks)) {
                out.write(result.get());
            }
        } finally {
            out.close();
        }
    }

    @Test
    public void testMerge() throws FileNotFoundException, InterruptedException, ExecutionException {
        merge(new DigestFactory<TreeDigest>() {
            @Override
            public TreeDigest create() {
                return new TreeDigest(50);
            }
        });
    }

    @Test
    public void testEmpty() {
        empty(new TreeDigest(100));
    }

    @Test
    public void testMergeEmpty() {
        final Random gen0 = RandomUtils.getRandom();
        List<TDigest> subData = new ArrayList<TDigest>();
        subData.add(new TreeDigest(10));
        TreeDigest foo = new TreeDigest(10);
        AbstractTDigest.merge(subData, gen0, foo);
        empty(foo);
    }

    @Test
    public void testSingleValue() {
        singleValue(new TreeDigest(100));
    }

    @Test
    public void testFewValues() {
        final TDigest digest = new TreeDigest(100);
        fewValues(digest);
    }

    @Test
    public void testMoreThan2BValues() {
        final TDigest digest = new TreeDigest(100);
        moreThan2BValues(digest);
    }

    @Test
    public void testExtremeQuantiles() {
        // t-digest shouldn't merge extreme nodes, but let's still test how it would
        // answer to extreme quantiles in that case ('extreme' in the sense that the
        // quantile is either before the first node or after the last one)
        TreeDigest digest = new TreeDigest(100);
        digest.add(10, 3);
        digest.add(20, 1);
        digest.add(40, 5);
        // this group tree is roughly equivalent to the following sorted array:
        // [ ?, 10, ?, 20, ?, ?, 50, ?, ? ]
        // and we expect it to compute approximate missing values:
        // [ 5, 10, 15, 20, 30, 40, 50, 60, 70]
        List<Double> values = Arrays.asList(5., 10., 15., 20., 30., 40., 50., 60., 70.);
        for (int i = 0; i < digest.size(); ++i) {
            final double q = 1.0 / (digest.size() - 1); // a quantile that matches an array index
            assertEquals(quantile(q, values), digest.quantile(q), 0.01);
        }
    }

    @Test
    public void testSorted() {
        final TDigest digest = factory.create();
        sorted(digest);
    }

    @Test
    public void testNaN() {
        final TDigest digest = factory.create();
        nan(digest);
    }
}
