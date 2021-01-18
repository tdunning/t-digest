package com.tdunning.tdigest.quality;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.tdunning.math.stats.*;
import org.apache.mahout.math.jet.random.AbstractContinousDistribution;
import org.junit.Test;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Produce measurements of accuracy versus compression factor for fixed data size
 */
public class AccuracyTest {
    private static final int N = 1_000_000;

    private final Random gen = new Random();

    /**
     * Generates information that demonstrates that t-digests can be merged without major loss of
     * accuracy.
     */
    @Test
    public void merge() {
        final Random seedGenerator = new Random();
        try (PrintWriter out = new PrintWriter(new File("merge.csv"))) {
            out.printf("type,parts,q,e0,e1,e2,e2.rel,e3\n");

            List<Callable<String>> tasks = Lists.newArrayList();
            for (int k = 0; k < 20; k++) {
                final int currentK = k;
                tasks.add(new Callable<String>() {
                    final Random gen = new Random(seedGenerator.nextLong());

                    @Override
                    public String call() {
                        StringWriter s = new StringWriter();
                        PrintWriter out = new PrintWriter(s);
                        System.out.printf("Starting %d\n", currentK);

                        for (int parts : new int[]{2, 5, 10, 20, 50, 100}) {
                            ArrayList<Double> data = Lists.newArrayList();

                            TDigest dist = new MergingDigest(100);
                            dist.recordAllData();

                            // we accumulate the data into multiple sub-digests
                            List<TDigest> subs = Lists.newArrayList();
                            for (int i = 0; i < parts; i++) {
                                subs.add(new MergingDigest(100).recordAllData());
                            }
                            List<TDigest> highRes = Lists.newArrayList();
                            for (int i = 0; i < parts; i++) {
                                highRes.add(new MergingDigest(200));
                            }

                            int[] cnt = new int[parts];
                            for (int i = 0; i < 100000; i++) {
                                double x = gen.nextDouble();
                                data.add(x);
                                dist.add(x);
                                subs.get(i % parts).add(x);
                                highRes.get(i % parts).add(x);
                                cnt[i % parts]++;
                            }
                            dist.compress();
                            Collections.sort(data);

                            // collect the raw data from the sub-digests
                            List<Double> data2 = Lists.newArrayList();
                            int i = 0;
                            int k = 0;
                            int totalByCount = 0;
                            for (TDigest digest : subs) {
                                assertEquals("Sub-digest size check", cnt[i], digest.size());
                                int k2 = 0;
                                for (Centroid centroid : digest.centroids()) {
                                    Iterables.addAll(data2, centroid.data());
                                    assertEquals("Centroid consistency", centroid.count(), centroid.data().size());
                                    k2 += centroid.data().size();
                                }
                                totalByCount += cnt[i];
                                k += k2;
                                assertEquals("Sub-digest centroid sum check", cnt[i], k2);
                                assertEquals("Sub-digest centroid sum check", cnt[i], subs.get(i).size());
                                i++;
                            }
                            assertEquals("Sub-digests don't add up to the right size", data.size(), k);
                            assertEquals("Counts don't match up", data.size(), totalByCount);

                            // verify that the raw data all got recorded
                            Collections.sort(data2);
                            assertEquals(data.size(), data2.size());
                            Iterator<Double> ix = data.iterator();
                            for (Double x : data2) {
                                assertEquals(ix.next(), x);
                            }

                            // now merge the sub-digests
                            TDigest dist2 = new MergingDigest(100).recordAllData();
                            dist2.add(subs);
                            assertEquals(String.format("Digest count is wrong %d vs %d", totalByCount, dist2.size()), totalByCount, dist2.size());

                            // verify the merged result has the right data
                            List<Double> data3 = Lists.newArrayList();
                            for (Centroid centroid : dist2.centroids()) {
                                Iterables.addAll(data3, centroid.data());
                            }
                            Collections.sort(data3);
                            assertEquals(String.format("Total data size %d vs %d", data.size(), data3.size()), data.size(), data3.size());
                            ix = data.iterator();
                            for (Double x : data3) {
                                assertEquals(ix.next(), x);
                            }

                            TDigest dist3 = new MergingDigest(100);
                            dist3.add(highRes);

                            final double[] allData = new double[data.size()];
                            int iz = 0;
                            for (double x : data) {
                                allData[iz++] = x;
                            }
                            for (double q : new double[]{0.001, 0.01, 0.1, 0.2, 0.3, 0.5}) {
                                double z = Dist.quantile(q, allData);
                                double e1 = dist.quantile(q) - z;
                                double e2 = dist2.quantile(q) - z;
                                double e2Relative = Math.abs(e2) / q;
                                double e3 = dist3.quantile(q) - z;
                                out.printf("quantile,%d,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f\n", parts, q, z - q, e1, e2, e2Relative, e3);
                                assertTrue(String.format("Relative error: parts=%d, q=%.4f, e1=%.5f, e2=%.5f, rel=%.4f, e3=%.4f", parts, q, e1, e2, e2Relative, e3), e2Relative < 0.4);
                                assertTrue(String.format("Absolute error: parts=%d, q=%.4f, e1=%.5f, e2=%.5f, rel=%.4f, e3=%.4f", parts, q, e1, e2, e2Relative, e3), Math.abs(e2) < 0.015);
                            }

                            for (double x : new double[]{0.001, 0.01, 0.1, 0.2, 0.3, 0.5}) {
                                double z = Dist.cdf(x, allData);
                                double e1 = dist.cdf(x) - z;
                                double e2 = dist2.cdf(x) - z;
                                double e3 = dist3.cdf(z) - z;

                                out.printf("cdf,%d,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f\n", parts, x, z - x, e1, e2, Math.abs(e2) / x, e3);
                                assertTrue(String.format("Absolute cdf: parts=%d, x=%.4f, e1=%.5f, e2=%.5f", parts, x, e1, e2), Math.abs(e2) < 0.015);
                                assertTrue(String.format("Relative cdf: parts=%d, x=%.4f, e1=%.5f, e2=%.5f, rel=%.3f", parts, x, e1, e2, Math.abs(e2) / x), Math.abs(e2) / x < 0.4);
                            }
                            out.flush();
                        }
                        System.out.printf("    Finishing %d\n", currentK + 1);
                        out.close();
                        return s.toString();
                    }
                });
            }

            ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 2);
            try {
                for (Future<String> result : executor.invokeAll(tasks)) {
                    out.write(result.get());
                }
            } catch (Throwable e) {
                fail(e.getMessage());
            } finally {
                executor.shutdownNow();
                executor.awaitTermination(10, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            fail("Tasks interrupted");
        } catch (FileNotFoundException e) {
            fail("Couldn't write to data output file merge.csv");
        }
    }

    @Test
    public void testTreeAccuracy() throws IOException, InterruptedException {
        // TODO there is a fair bit of duplicated code here
        String head = Git.getHash(true).substring(0, 10);
        String experiment = "tree-digest";
        //noinspection ResultOfMethodCallIgnored
        new File("tests").mkdirs();
        PrintWriter quantiles = new PrintWriter(String.format("tests/accuracy-%s-%s.csv", experiment, head));
        PrintWriter sizes = new PrintWriter(String.format("tests/accuracy-sizes-%s-%s.csv", experiment, head));
        PrintWriter cdf = new PrintWriter(String.format("tests/accuracy-cdf-%s-%s.csv", experiment, head));
        quantiles.printf("digest, dist, sort, q.digest, q.raw, error, compression, x, k, clusters\n");
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
                            evaluate(finalK, quantiles, sizes, cdf, dist, "unsorted", sorted, compression, factory.create(compression));

                            digest = factory.create(compression);
                            for (double datum : sorted) {
                                digest.add(datum);
                            }
                            evaluate(finalK, quantiles, sizes, cdf, dist, "sorted", sorted, compression, factory.create(compression));
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
        quantiles.close();
        cdf.close();
    }

    @Test
    public void testAccuracyVersusCompression() throws IOException, InterruptedException {
        String head = Git.getHash(true).substring(0, 10);
        String experiment = "digest";
        //noinspection ResultOfMethodCallIgnored
        new File("tests").mkdirs();
        try (PrintWriter out = new PrintWriter(String.format("tests/accuracy-%s-%s.csv", experiment, head));
             PrintWriter cdf = new PrintWriter(String.format("tests/accuracy-cdf-%s-%s.csv", experiment, head));
             PrintWriter sizes = new PrintWriter(String.format("tests/accuracy-sizes-%s-%s.csv", experiment, head))) {
            out.printf("digest, dist, sort, q.digest, q.raw, error, compression, q, x, k, clusters\n");
            cdf.printf("digest, dist, sort, x.digest, x.raw, error, compression, q, k, clusters\n");
            sizes.printf("digest, dist, sort, q.0, q.1, dk, mean, compression, count, k, clusters\n");

            AtomicBoolean abort = new AtomicBoolean(false);
            ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 4);
            Collection<Callable<Integer>> tasks = new ArrayList<>();
            AtomicInteger lines = new AtomicInteger();
            long t0 = System.nanoTime();
            for (int k = 0; k < 50; k++) {
                int finalK = k;
                tasks.add(() -> {
//                            for (Util.Distribution dist : Collections.singleton(Util.Distribution.UNIFORM)) {
                    for (Util.Distribution dist : Util.Distribution.values()) {
                        AbstractContinousDistribution dx = dist.create(gen);
                        int size = (int) (N + new Random().nextGaussian() * 1000);
                        double[] raw = new double[size];
                        for (int i = 0; i < size; i++) {
                            raw[i] = dx.nextDouble();
                        }
                        double[] sorted = Arrays.copyOf(raw, raw.length);
                        Arrays.sort(sorted);

                        for (boolean useWeightLimit : new boolean[]{true, false}) {
                            for (ScaleFunction scale : ScaleFunction.values()) {
                                if (abort.get()) {
                                    // some alternative failed don't even try to continue
                                    return 0;
                                }
                                if (scale.toString().contains("_NO_NORM") || scale.toString().equals("K_0")
                                        || scale.toString().contains("FAST") || scale.toString().contains("kSize")) {
                                    continue;
                                }
                                for (double compression : new double[]{50, 100, 200, 500, 1000}) {
                                    //                            for (double compression : new double[]{100, 200, 500}) {
                                    for (Util.Factory factory : Collections.singleton(Util.Factory.MERGE)) {
                                        //                                    for (Util.Factory factory : Util.Factory.values()) {
                                        TDigest digest = factory.create(compression);
                                        MergingDigest.useWeightLimit = useWeightLimit;
                                        try {
                                            digest.setScaleFunction(scale);
                                        } catch (IllegalArgumentException e) {
                                            // not all scale functions work with different weight limit strategies
                                            continue;
                                        }
                                        if (digest.toString().contains("K_3")) {
                                            continue;
                                        }
                                        try {
                                            for (double datum : raw) {
                                                digest.add(datum);
                                            }
                                            digest.compress();
                                            evaluate(finalK, out, sizes, cdf, dist, "unsorted", sorted, compression, digest);
                                        } catch (Throwable e) {
                                            System.err.printf("Aborting test with %s, %b, %.0f\n", digest, useWeightLimit, compression);
                                            e.printStackTrace();
                                            abort.set(true);
                                            throw e;
                                        }
                                    }
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
            assertFalse("Tasks aborted in test", abort.get());
        }
    }

    private void evaluate(int k, PrintWriter quantiles, PrintWriter sizes, PrintWriter cdf,
                          Util.Distribution dist, String sort,
                          double[] sorted, double compression, TDigest digest) {
        int clusters = digest.centroidCount();
        double qx = 0;
        for (Centroid centroid : digest.centroids()) {
            double dq = (double) centroid.count() / sorted.length;
            double k0 = ((MergingDigest) digest).getScaleFunction().k(qx, compression, digest.size());
            double k1 = ((MergingDigest) digest).getScaleFunction().k(qx + dq, compression, digest.size());
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (sizes) {
                sizes.printf("%s,%s,%s,%.8f,%.8f,%.8f,%.8g,%.0f,%d,%d,%d\n",
                        digest, dist, sort, qx, qx + dq, k1 - k0, centroid.mean(), compression, centroid.count(), k, clusters);
            }
            qx += dq;
        }
        for (double q : new double[]{1e-6, 1e-5, 0.0001, 0.001, 0.01, 0.1, 0.5, 0.9, 0.99, 0.999, 0.9999, 1 - 1e-5, 1 - 1e-6}) {
            double x = Dist.quantile(q, sorted);
            double q1 = digest.cdf(x);
            double q0 = Dist.cdf(x, sorted);
            double error = (q1 - q0) / Math.min(q1, 1 - q1);
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (quantiles) {
                quantiles.printf("%s,%s,%s,%.8f,%.8f,%.8g,%.0f,%.8g,%.8g,%d,%d\n", digest, dist, sort, q1, q0, error, compression, q, x, k, clusters);
            }
        }
        for (double q : new double[]{1e-6, 1e-5, 0.0001, 0.001, 0.01, 0.1, 0.5, 0.9, 0.99, 0.999, 0.9999, 1 - 1e-5, 1 - 1e-6}) {
            double x1 = digest.quantile(q);
            double x0 = Dist.quantile(q, sorted);
            double error = (x1 - x0) / Math.min(x1, 1 - x1);
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (cdf) {
                cdf.printf("%s,%s,%s,%.8f,%.8f,%.8g,%.0f,%.8g,%d,%d\n", digest, dist, sort, x1, x0, error, compression, q, k, clusters);
            }
        }
    }

    /**
     * Prints the actual samples that went into a few clusters near the tails and near the median.
     * <p>
     * This is important for testing how close to ideal a real-world t-digest might be. In particular,
     * it lets us visualize how clusters are shaped in sample space to look for smear or skew.
     * <p>
     * The accuracy.r script produces a visualization of the data produced by this test.
     *
     * @throws FileNotFoundException If output file can't be opened.
     * @throws InterruptedException  If threads are interrupted (we don't ever expect that to happen).
     */
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
//                        double[] sorted = Arrays.copyOf(raw, raw.length);
//                        Arrays.sort(sorted);
                        for (ScaleFunction scale : new ScaleFunction[]{ScaleFunction.K_2, ScaleFunction.K_3}) {
                            MergingDigest digest = new MergingDigest(compression);
                            digest.recordAllData();
                            digest.setScaleFunction(scale);

                            evaluate2(finalK, dist, samples, raw, compression, digest);
//                            evaluate2(finalK, dist, samples, "sorted", factory, sorted, compression);
                        }
                    }
                    //                  }
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

    private void evaluate2(int k, Util.Distribution dist, PrintWriter samples,
                           double[] data, double compression, TDigest digest) {

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
                                digest, dist, "unsorted", compression,
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

