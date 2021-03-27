package com.tdunning.tdigest.quality;

import com.tdunning.math.stats.Dist;
import com.tdunning.math.stats.InterpolatingDigest;
import com.tdunning.math.stats.MergingDigest;
import com.tdunning.math.stats.ScaleFunction;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.datasketches.kll.KllFloatsSketch;
import org.apache.datasketches.req.ReqSketch;
import org.apache.datasketches.req.ReqSketchBuilder;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Random;

/**
 * Measures the achieved accuracy for the ReqSketch from Apache DataSketches.
 */
public class CompareKllTest {
    public interface Generator {
        double next();

        double inverseCDF(double q);
    }

    public class UniformSampler implements Generator {
        Random gen = new Random();

        @Override
        public double next() {
            return gen.nextDouble();
        }

        @Override
        public double inverseCDF(double q) {
            return q;
        }

        @Override
        public String toString() {
            return "uniform";
        }
    }

    public class RepetitiveSampler implements Generator {
        Random gen = new Random();
        double p;     // probability of incrementing count
        double decay; // how much p decreases each step
        double count; // currently returned value

        int[] resultCounts;
        long  samples;

        public RepetitiveSampler() {
            this(0.001, 0.98);
        }

        public RepetitiveSampler(double p, double decay) {
            this.p = p;
            this.decay = decay;
            this.count = 0;
            resultCounts = new int[100];
            samples = 0;
        }

        public double next() {
            if (gen.nextDouble() < p) {
                count++;
                p *= decay;
            }
            while (count > resultCounts.length) {
                resultCounts = Arrays.copyOf(resultCounts, 2 * resultCounts.length);
            }
            resultCounts[(int) count]++;
            samples++;
            return count;
        }

        @Override
        public double inverseCDF(double q) {
            if (q < 0 || q > 1) {
                throw new IllegalArgumentException("Value of q must be in [0,1]");
            }
            double u = samples * q;
            if (u > samples) {
                u = samples;
            }
            for (int i = 0; i < resultCounts.length; i++) {
                if (u <= resultCounts[i]) {
                    return i;
                }
                u -= resultCounts[i];
            }
            int i = resultCounts.length-1;
            while (i >= 0 && resultCounts[i] == 0) {
                i--;
            }
            return i;
        }

        @Override
        public String toString() {
            return "repetitive";
        }
    }

    public class NormalSampler implements Generator {
        Random gen = new Random();
        double mean;
        double sd;
        NormalDistribution dist;

        public NormalSampler() {
            this(0, 1);
        }

        public NormalSampler(double mean, double sd) {
            this.mean = mean;
            this.sd = sd;
            dist = new NormalDistribution(mean, sd);
        }

        public double next() {
            return gen.nextGaussian() * sd + mean;
        }

        public double inverseCDF(double q) {
            return dist.inverseCumulativeProbability(q);
        }

        @Override
        public String toString() {
            if (mean == 0 && sd == 1) {
                return "normal";
            } else {
                return String.format("normal(%.2f,%.2f)", mean, sd);
            }
        }
    }

    /**
     * Creates data files for visualizing errors for a variety of conditions for both
     * t-digest and for the relative and absolute error versions of the KLL algorithm.
     *
     * The `kll-accuracy.csv` data file contains a table with relative and absolute errors. These are
     * computed for different algorithms, relative error KLL, absolute error KLL,
     * MergingDigest with K_2, MergingDigest with K_3 and InterpolatingDigest with K_3 (coded as `req`, `kll`,
     * `t2`, `t3` or `i3`), values of size parameter k, number of samples (dithered), number of samples (nominal)
     * quantile of interest, errors both absolute and relative.
     *
     * The dithering of the number of samples is done to prevent an algorithm from "cheating" if a desired quantile
     * falls on exactly the same boundary every time as it would, for instance if q=1e-2 and n=100,000. By varying
     * n over a few percent these unrealistic results are washed away.
     *
     * A second data file called `kll-sizes.csv` contains the number of retained samples (or centroids) and estimated
     * size in bytes for digests under the same conditions.
     *
     * These results can be visualized by using the `draw.kll` function from `quality/accuracy.r` which will write
     * a file `kll-comparison.pdf`
     *
     * @throws FileNotFoundException If output files cannot be opened
     */
    @Test
    public void testUniform() throws FileNotFoundException {
        Random rand = new Random();
        try (PrintWriter accuracy = new PrintWriter("kll-accuracy.csv");
             PrintWriter sizes = new PrintWriter("kll-sizes.csv")) {
            accuracy.printf("iteration, alg, dist, k, n, n0, q, x.res, x.actual, abs.error, rel.error\n");
            sizes.printf("iteration, alg, dist, k, n, n0, samples, bytes\n");
            for (int n0 : new int[]{100, 1000, 10000, 100000, 1000000}) {
                int[] kllSizes = {8, 10, 20, 50, 100, 200};
                int[] tdSizes = {50, 100, 200, 300, 500, 1000};
                for (int kx = 0; kx < kllSizes.length; kx++) {
                    System.out.printf("n = %d, k = %d\n", n0, kx);
                    for (Generator gen : new Generator[]{new UniformSampler(), new NormalSampler(3, 1), new RepetitiveSampler()}) {
                        for (int i = 0; i < 50; i++) {
                            // dither the number of points to avoid making q*n an exact integer
                            int n = (int) (n0 * (1 + 0.1 * rand.nextDouble()));

                            ReqSketch res = new ReqSketchBuilder().setK(kllSizes[kx]).setHighRankAccuracy(false).build();
                            KllFloatsSketch kll = new KllFloatsSketch(kllSizes[kx]);
                            MergingDigest t2 = new MergingDigest(tdSizes[kx]);
                            t2.setScaleFunction(ScaleFunction.K_2);
                            MergingDigest t3 = new MergingDigest(tdSizes[kx]);
                            t3.setScaleFunction(ScaleFunction.K_3);
                            InterpolatingDigest ix = new InterpolatingDigest(tdSizes[kx]);
                            ix.setScaleFunction(ScaleFunction.K_2);

                            double[] data = new double[n];
                            RepetitiveSampler s = new RepetitiveSampler();
                            for (int j = 0; j < n; j++) {
                                double x = gen.next();
                                data[j] = x;
                                res.update((float) x);
                                kll.update((float) x);
                                t2.add(x);
                                t3.add(x);
                                ix.add(x);
                            }
                            Arrays.sort(data);

                            int sizeInBytes = res.getSerializationBytes();
                            sizes.printf("%d,req,%s,%d,%d,%d,%d,%d\n", i, gen, kllSizes[kx], n, n0, res.getRetainedItems(), sizeInBytes);
                            sizes.printf("%d,kll,%s,%d,%d,%d,%d,%d\n", i, gen, kllSizes[kx], n, n0, kll.getNumRetained(), kll.getSerializedSizeBytes());
                            sizes.printf("%d,t2,%s,%d,%d,%d,%d,%d\n", i, gen, tdSizes[kx], n, n0, t2.centroidCount(), t2.smallByteSize());
                            sizes.printf("%d,t3,%s,%d,%d,%d,%d,%d\n", i, gen, tdSizes[kx], n, n0, t3.centroidCount(), t3.smallByteSize());
                            sizes.printf("%d,i2,%s,%d,%d,%d,%d,%d\n", i, gen, tdSizes[kx], n, n0, ix.centroidCount(), ix.smallByteSize());
                            for (double q : new double[]{1e-6, 1e-5, 1e-4, 0.001, 0.01, 0.1, 0.5}) {
                                double ref = Dist.quantile(q, data);

                                double x1 = res.getQuantile(q);
                                double tolerance = Math.max(1e-7, Math.min(x1, ref));
                                accuracy.printf("%d,req,%s,%d,%d,%d,%.6f,%.5f,%.5f,%.5f,%.5f\n",
                                        i, gen, kllSizes[kx], n, n0, q, x1, ref, Math.abs(x1 - ref), Math.abs(x1 - ref) / tolerance);

                                double x2 = kll.getQuantile(q);
                                tolerance = Math.max(1e-7, Math.min(x2, ref));
                                accuracy.printf("%d,kll,%s,%d,%d,%d,%.6f,%.5f,%.5f,%.5f,%.5f\n",
                                        i, gen, kllSizes[kx], n, n0, q, x2, ref, Math.abs(x2 - ref), Math.abs(x2 - ref) / tolerance);

                                double x3 = t2.quantile(q);
                                tolerance = Math.max(1e-7, Math.min(x3, ref));
                                accuracy.printf("%d,t2,%s,%d,%d,%d,%.6f,%.5f,%.5f,%.5f,%.5f\n",
                                        i, gen, tdSizes[kx], n, n0, q, x3, ref, Math.abs(x3 - ref), Math.abs(x3 - ref) / tolerance);

                                double x4 = t3.quantile(q);
                                tolerance = Math.max(1e-7, Math.min(x4, ref));
                                accuracy.printf("%d,t3,%s,%d,%d,%d,%.6f,%.5f,%.5f,%.5f,%.5f\n",
                                        i, gen, tdSizes[kx], n, n0, q, x4, ref, Math.abs(x4 - ref), Math.abs(x4 - ref) / tolerance);

                                double x5 = ix.quantile(q);
                                tolerance = Math.max(1e-7, Math.min(x5, ref));
                                accuracy.printf("%d,i2,%s,%d,%d,%d,%.6f,%.5f,%.5f,%.5f,%.5f\n",
                                        i, gen, tdSizes[kx], n, n0, q, x5, ref, Math.abs(x5 - ref), Math.abs(x5 - ref) / tolerance);
                            }
                        }

                    }
                }
            }

        }
    }
}
