package com.tdunning.tdigest.quality;

import com.google.common.collect.Lists;
import com.tdunning.math.stats.Centroid;
import com.tdunning.math.stats.MergingDigest;
import com.tdunning.math.stats.TDigest;
import org.apache.mahout.math.jet.random.AbstractContinousDistribution;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Produce measurements of accuracy versus compression factor for fixed data size
 */
public class AccuracyTest {
    private static final int N = 1000000;

    private final Random gen = new Random();

    @Test
    public void testTreeAccuracy() throws IOException, InterruptedException {
        // TODO there is a fair bit of duplicated code here
        String head = Util.getHash(true).substring(0, 10);
        String experiment = "tree-digest";
        new File("tests").mkdirs();
        PrintWriter out = new PrintWriter(String.format("tests/accuracy-%s-%s.csv", experiment, head));
        PrintWriter sizes = new PrintWriter(String.format("tests/accuracy-sizes-%s-%s.csv", experiment, head));
        PrintWriter cdf = new PrintWriter(String.format("tests/accuracy-cdf-%s-%s.csv", experiment, head));
        out.printf("digest, dist, sort, q.digest, q.raw, error, compression, x, k, clusters\n");
        cdf.printf("digest, dist, sort, x.digest, x.raw, error, compression, q, k, clusters\n");
        sizes.printf("digest, dist, sort, q.0, q.1, dk, mean, compression, count, k, clusters\n");
        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 4);
        Collection<Callable<Integer>> tasks = new ArrayList<>();
        AtomicInteger lines = new AtomicInteger();
        long t0 = System.nanoTime();
        for (int k = 0; k < 20; k++) {
            int finalK = k;
            tasks.add(() -> {
                for (Util.Distribution dist : Collections.singleton(Util.Distribution.UNIFORM)) {
//                        for (Util.Distribution dist : Util.Distribution.values()) {
                    AbstractContinousDistribution dx = dist.create(gen);
                    double[] raw = new double[N];
                    for (int i = 0; i < N; i++) {
                        raw[i] = dx.nextDouble();
                    }
                    double[] sorted = Arrays.copyOf(raw, raw.length);
                    Arrays.sort(sorted);

                    for (double compression : new double[]{20, 50, 100, 200, 500}) {
                        for (Util.Factory factory : Collections.singleton(Util.Factory.TREE)) {
//                                    for (Util.Factory factory : Util.Factory.values()) {
                            TDigest digest = factory.create(compression);
                            for (double datum : raw) {
                                digest.add(datum);
                            }
                            evaluate(finalK, out, sizes, cdf, dist, "unsorted", factory, sorted, compression, factory.create(compression));

                            digest = factory.create(compression);
                            for (double datum : sorted) {
                                digest.add(datum);
                            }
                            evaluate(finalK, out, sizes, cdf, dist, "sorted", factory, sorted, compression, factory.create(compression));
                        }
                    }
                }
                int count = lines.incrementAndGet();
                long t = System.nanoTime();
                double duration = (t - t0) * 1e-9;
                System.out.printf("%d, %d, %.2f, %.3f\n", finalK, count, duration, count / duration);
                return finalK;
            });
        }
        pool.invokeAll(tasks);
        sizes.close();
        out.close();
        cdf.close();
    }

    @Test
    public void testAccuracyVersusCompression() throws IOException, InterruptedException {
        String head = Util.getHash(true).substring(0, 10);
        String experiment = "pre-digest";
        MergingDigest.useConservativeLimit = true;
        MergingDigest.useWeightLimit = true;
        MergingDigest.useAlternatingSort = true;
        new File("tests").mkdirs();
        PrintWriter out = new PrintWriter(String.format("tests/accuracy-%s-%s.csv", experiment, head));
        PrintWriter sizes = new PrintWriter(String.format("tests/accuracy-sizes-%s-%s.csv", experiment, head));
        PrintWriter cdf = new PrintWriter(String.format("tests/accuracy-cdf-%s-%s.csv", experiment, head));
        out.printf("digest, dist, sort, q.digest, q.raw, error, compression, x, k, clusters\n");
        cdf.printf("digest, dist, sort, x.digest, x.raw, error, compression, q, k, clusters\n");
        sizes.printf("digest, dist, sort, q.0, q.1, dk, mean, compression, count, k, clusters\n");
        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 4);
        Collection<Callable<Integer>> tasks = new ArrayList<>();
        AtomicInteger lines = new AtomicInteger();
        long t0 = System.nanoTime();
        for (int k = 0; k < 40; k++) {
            int finalK = k;
            tasks.add(() -> {
                for (Util.Distribution dist : Collections.singleton(Util.Distribution.UNIFORM)) {
//                        for (Util.Distribution dist : Util.Distribution.values()) {
                    AbstractContinousDistribution dx = dist.create(gen);
                    double[] raw = new double[N];
                    for (int i = 0; i < N; i++) {
                        raw[i] = dx.nextDouble();
                    }
                    double[] sorted = Arrays.copyOf(raw, raw.length);
                    Arrays.sort(sorted);

                    for (double compression : new double[]{100, 200, 500}) {
//                            for (double compression : new double[]{100, 200, 500}) {
                        for (Util.Factory factory : Collections.singleton(Util.Factory.MERGE)) {
//                                    for (Util.Factory factory : Util.Factory.values()) {
                            TDigest digest = factory.create(compression);
                            for (double datum : raw) {
                                digest.add(datum);
                            }
                            evaluate(finalK, out, sizes, cdf, dist, "unsorted", factory, sorted, compression, digest);

                            digest = factory.create(compression);
                            for (double datum : sorted) {
                                digest.add(datum);
                            }
                            evaluate(finalK, out, sizes, cdf, dist, "sorted", factory, sorted, compression, digest);

                            for (int batchSize: new int[]{100,200,500,1000,2000,5000,10000}) {
                                digest = factory.create(compression);
                                TDigest subDigest = new MergingDigest(compression);
                                int i = 0;
                                for (double datum : raw) {
                                    digest.add(datum);
                                    i++;
                                    if (i > batchSize) {
                                        digest.add(subDigest);
                                        subDigest = new MergingDigest(compression);
                                        i = 0;
                                    }
                                }
                                if (subDigest.size() > 0) {
                                    digest.add(subDigest);
                                }
                                evaluate(finalK, out, sizes, cdf, dist, "batch-" + batchSize, factory, sorted, compression, digest);
                            }
                        }
                    }
                }
                int count = lines.incrementAndGet();
                long t = System.nanoTime();
                double duration = (t - t0) * 1e-9;
                System.out.printf("%d, %d, %.2f, %.3f\n", finalK, count, duration, count / duration);
                return finalK;
            });
        }
        pool.invokeAll(tasks);
        sizes.close();
        out.close();
        cdf.close();
    }

    private void evaluate(int k, PrintWriter out, PrintWriter sizes, PrintWriter cdf,
                          Util.Distribution dist, String sort, Util.Factory factory,
                          double[] sorted, double compression, TDigest digest) {
        int clusters = digest.centroidCount();
        double qx = 0;
        for (Centroid centroid : digest.centroids()) {
            double dq = (double) centroid.count() / N;
            double k0 = kIndex(qx, compression);
            double k1 = kIndex(qx + dq, compression);
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (sizes) {
                sizes.printf("%s,%s,%s,%.8f,%.8f,%.8f,%.8g,%.0f,%d,%d,%d\n",
                        factory, dist, sort, qx, qx + dq, k1 - k0, centroid.mean(), compression, centroid.count(), k, clusters);
            }
            qx += dq;
        }
        for (double q : new double[]{0.0001, 0.001, 0.01, 0.1, 0.5, 0.9, 0.99, 0.999, 0.9999}) {
            double x = Util.quantile(q, sorted);
            double q1 = digest.cdf(x);
            double q0 = Util.cdf(x, sorted);
            double error = (q1 - q0) / Math.min(q1, 1 - q1);
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (out) {
                out.printf("%s,%s,%s,%.8f,%.8f,%.8g,%.0f,%.8g,%d,%d\n", factory, dist, sort, q1, q0, error, compression, x, k, clusters);
            }
        }
        for (double q : new double[]{0.0001, 0.001, 0.01, 0.1, 0.5, 0.9, 0.99, 0.999, 0.9999}) {
            double x1 = digest.quantile(q);
            double x0 = Util.quantile(q, sorted);
            double error = (x1 - x0) / Math.min(x1, 1 - x1);
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (cdf) {
                cdf.printf("%s,%s,%s,%.8f,%.8f,%.8g,%.0f,%.8g,%d,%d\n", factory, dist, sort, x1, x0, error, compression, q, k, clusters);
            }
        }
    }

    private double kIndex(double q, double compression) {
        double u = 2 * q - 1;
        return Math.acos(-u) * compression / Math.PI;
    }

    @Test
    public void testBucketFill() throws FileNotFoundException, InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 2);
        Collection<Callable<Integer>> tasks = new ArrayList<>();
        AtomicInteger lines = new AtomicInteger();
        long t0 = System.nanoTime();

        PrintWriter samples = new PrintWriter("accuracy-samples.csv");
        samples.printf("digest, dist, sort, compression, k, centroid, centroid.down, i, x, mean, q0, q1\n");
        for (int k = 0; k < 20; k++) {
            int finalK = k;
            tasks.add(() -> {
                for (double compression : new double[]{100}) {
                    for (Util.Distribution dist : Util.Distribution.values()) {
                        AbstractContinousDistribution dx = dist.create(gen);
                        double[] raw = new double[N];
                        for (int i = 0; i < N; i++) {
                            raw[i] = dx.nextDouble();
                        }
                        double[] sorted = Arrays.copyOf(raw, raw.length);
                        Arrays.sort(sorted);

                        for (Util.Factory factory : Collections.singletonList(Util.Factory.MERGE)) {
                            evaluate2(finalK, dist, samples, "unsorted", factory, raw, compression);
                            evaluate2(finalK, dist, samples, "sorted", factory, sorted, compression);
                        }
                    }
                }
                int count = lines.incrementAndGet();
                long t = System.nanoTime();
                double duration = (t - t0) * 1e-9;
                System.out.printf("%d, %d, %.2f, %.3f\n", finalK, count, duration, count / duration);
                return finalK;
            });
        }
        pool.invokeAll(tasks);
        samples.close();
    }

    private void evaluate2(int k,
                           Util.Distribution dist, PrintWriter samples, String sort, Util.Factory factory,
                           double[] data, double compression) {
        TDigest digest = factory.create(compression);
        digest.recordAllData();

        for (double datum : data) {
            digest.add(datum);
        }

        double qx = 0;
        int cx = 0;
        Collection<Centroid> centroids = digest.centroids();
        for (Centroid centroid : centroids) {
            double dq = (double) centroid.count() / N;
            if (qx < 0.05 || Math.abs(qx - 0.5) < 0.025 || qx > 0.95) {
                int sx = 0;
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (samples) {
                    for (Double x : centroid.data()) {
                        samples.printf("%s,%s,%s,%.0f,%d,%d,%d,%d,%.8f,%.8f,%.8f,%.8f\n",
                                factory, dist, sort, compression,
                                k, cx, centroids.size() - cx - 1, sx, x, centroid.mean(), qx, qx + dq);
                        sx++;
                    }
                }
            }
            qx += dq;
            cx++;
        }
    }
}

