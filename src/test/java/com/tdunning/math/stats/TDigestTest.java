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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.apache.mahout.common.RandomUtils;
import org.apache.mahout.math.jet.random.AbstractContinousDistribution;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Common test methods for TDigests
 */
public class TDigestTest {
    protected static PrintWriter sizeDump;
    protected static PrintWriter errorDump;
    protected static PrintWriter deviationDump;

    @BeforeClass
    public static void setup() throws IOException {
        sizeDump = new PrintWriter(new FileWriter("sizes.csv"));
        sizeDump.printf("tag\ti\tq\tk\tactual\n");

        errorDump = new PrintWriter((new FileWriter("errors.csv")));
        errorDump.printf("dist\ttag\tx\tQ\terror\n");

        deviationDump = new PrintWriter((new FileWriter("deviation.csv")));
        deviationDump.printf("tag\tQ\tk\tx\tmean\tleft\tright\tdeviation\n");
    }

    @AfterClass
    public static void teardown() {
        sizeDump.close();
        errorDump.close();
        deviationDump.close();
    }


    protected void merge(final DigestFactory<? extends TDigest> factory) throws FileNotFoundException, InterruptedException, ExecutionException {
        final Random gen0 = RandomUtils.getRandom();
        PrintWriter out = new PrintWriter(new File("merge.tsv"));
        out.printf("type\tparts\tq\te0\te1\te2\te2.rel\n");

        List<Callable<String>> tasks = Lists.newArrayList();
        for (int k = 0; k < repeats(); k++) {
            final int currentK = k;
            tasks.add(new Callable<String>() {
                Random gen = new Random(gen0.nextLong());

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

                        for (int i = 0; i < 100000; i++) {
                            double x = gen.nextDouble();
                            data.add(x);
                            dist.add(x);
                            subs.get(i % parts).add(x);
                        }
                        dist.compress();
                        Collections.sort(data);

                        // collect the raw data from the sub-digests
                        List<Double> data2 = Lists.newArrayList();
                        for (TDigest digest : subs) {
                            for (Centroid centroid : digest.centroids()) {
                                Iterables.addAll(data2, centroid.data());
                            }
                        }
                        Collections.sort(data2);

                        // verify that the raw data all got recorded
                        assertEquals(data.size(), data2.size());
                        Iterator<Double> ix = data.iterator();
                        for (Double x : data2) {
                            assertEquals(ix.next(), x);
                        }

                        // now merge the sub-digests
                        TDigest dist2 = AbstractTDigest.merge(subs, gen, factory.create());

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

                        for (double q : new double[]{0.001, 0.01, 0.1, 0.2, 0.3, 0.5}) {
                            double z = quantile(q, data);
                            double e1 = dist.quantile(q) - z;
                            double e2 = dist2.quantile(q) - z;
                            out.printf("quantile\t%d\t%.6f\t%.6f\t%.6f\t%.6f\t%.6f\n", parts, q, z - q, e1, e2, Math.abs(e2) / q);
                            assertTrue(String.format("parts=%d, q=%.4f, e1=%.5f, e2=%.5f, rel=%.4f", parts, q, e1, e2, Math.abs(e2) / q), Math.abs(e2) / q < 0.1);
                            assertTrue(String.format("parts=%d, q=%.4f, e1=%.5f, e2=%.5f, rel=%.4f", parts, q, e1, e2, Math.abs(e2) / q), Math.abs(e2) < 0.015);
                        }

                        for (double x : new double[]{0.001, 0.01, 0.1, 0.2, 0.3, 0.5}) {
                            double z = cdf(x, data);
                            double e1 = dist.cdf(x) - z;
                            double e2 = dist2.cdf(x) - z;

                            out.printf("cdf\t%d\t%.6f\t%.6f\t%.6f\t%.6f\t%.6f\n", parts, x, z - x, e1, e2, Math.abs(e2) / x);
                            assertTrue(String.format("parts=%d, x=%.4f, e1=%.5f, e2=%.5f", parts, x, e1, e2), Math.abs(e2) < 0.015);
                            assertTrue(String.format("parts=%d, x=%.4f, e1=%.5f, e2=%.5f", parts, x, e1, e2), Math.abs(e2) / x < 0.1);
                        }
                        out.flush();
                    }
                    System.out.printf("Iteration %d\n", currentK + 1);
                    out.close();
                    return s.toString();
                }
            });
        }

        for (Future<String> result : Executors.newFixedThreadPool(20).invokeAll(tasks)) {
            out.write(result.get());
        }
        out.close();
    }

    protected void singleValue(TDigest digest) {
        final double value = RandomUtils.getRandom().nextDouble() * 1000;
        digest.add(value);
        final double q = RandomUtils.getRandom().nextDouble();
        for (double qValue : new double[] {0, q, 1}) {
            assertEquals(value, digest.quantile(qValue), 0.001f);
        }
    }

    protected void fewValues(TDigest digest) {
        // When there are few values in the tree, quantiles should be exact
        final Random r = RandomUtils.getRandom();
        final int length = r.nextInt(10);
        final List<Double> values = new ArrayList<Double>();
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
        for (double q : new double [] {0, 1e-10, r.nextDouble(), 0.5, 1-1e-10, 1}) {
            assertEquals(quantile(q, values), digest.quantile(q), 0.01);
        }
    }

    protected double cdf(final double x, List<Double> data) {
        int n1 = 0;
        int n2 = 0;
        for (Double v : data) {
            n1 += (v < x) ? 1 : 0;
            n2 += (v <= x) ? 1 : 0;
        }
        return (n1 + n2) / 2.0 / data.size();
    }

    protected double quantile(final double q, List<Double> data) {
        if (data.size() == 0) {
            return Double.NaN;
        }
        if (q == 1 || data.size() == 1) {
            return data.get(data.size() - 1);
        }
        final double index = q * (data.size() - 1);
        final int intIndex = (int) index;
        return data.get(intIndex + 1) * (index - intIndex) + data.get(intIndex) * (intIndex + 1 - index);
    }

    protected int repeats() {
        return Boolean.parseBoolean(System.getProperty("runSlowTests")) ? 10 : 1;
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
    protected void runTest(DigestFactory<? extends TDigest> factory, AbstractContinousDistribution gen, double sizeGuide, double[] qValues, String tag, boolean recordAllData) {
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
            assertEquals(String.format("Lost count at %d", sumW), sumW, dist.size());
        }
        dist.compress();
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

        assertTrue(String.format("Summary is too large (got %d, wanted < %.1f)", dist.centroids().size(), 11 * sizeGuide), dist.centroids().size() < 11 * sizeGuide);
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

    protected void empty(TDigest digest) {
        final double q = RandomUtils.getRandom().nextDouble();
        assertTrue(Double.isNaN(digest.quantile(q)));
    }

    protected void moreThan2BValues(TDigest digest) {
        Random gen = RandomUtils.getRandom();
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
        final double[] quantiles = new double[] {0, 0.1, 0.5, 0.9, 1, gen.nextDouble()};
        Arrays.sort(quantiles);
        double prev = Double.NEGATIVE_INFINITY;
        for (double q : quantiles) {
            final double v = digest.quantile(q);
            assertTrue(v >= prev);
            prev = v;
        }
    }

    @Test
    public void testMergeEmpty() {
        final Random gen0 = RandomUtils.getRandom();
        List<TDigest> subData = new ArrayList();
        subData.add(new TreeDigest(10));
        TreeDigest foo = new TreeDigest(10);
        AbstractTDigest.merge(subData, gen0, foo);
        empty(foo);
    }

    public interface DigestFactory<T extends TDigest> {
        T create();
    }

    protected void sorted(TDigest digest) {
        Random gen = RandomUtils.getRandom();
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

    protected void nan(TDigest digest) {
        Random gen = RandomUtils.getRandom();
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
}
