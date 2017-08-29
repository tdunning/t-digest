package com.tdunning.tdigest.quality;

import com.tdunning.math.stats.Centroid;
import com.tdunning.math.stats.TDigest;
import org.apache.mahout.math.jet.random.AbstractContinousDistribution;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Random;

/**
 * Produce measurements of accuracy versus compression factor for fixed data size
 */
public class AccuracyTest {
    private static final int N = 1000000;

    private final Random gen = new Random();

    @Test
    public void testAccuracyVersusCompression() throws FileNotFoundException {
        PrintWriter out = new PrintWriter("accuracy.csv");
        PrintWriter sizes = new PrintWriter("accuracy-sizes.csv");
        out.printf("digest, dist, sort, q.digest, q.raw, error, compression, x, k\n");
        sizes.printf("digest, dist, sort, q.0, q.1, dk, mean, compression, count, k\n");
        for (int k = 0; k < 5; k++) {
            for (double compression : new double[]{100, 200, 500}) {
                for (Util.Distribution dist : Util.Distribution.values()) {
                    AbstractContinousDistribution dx = dist.create(gen);
                    double[] raw = new double[N];
                    for (int i = 0; i < N; i++) {
                        raw[i] = dx.nextDouble();
                    }
                    double[] sorted = Arrays.copyOf(raw, raw.length);
                    Arrays.sort(sorted);

                    for (Util.Factory factory : Util.Factory.values()) {
                        evaluate(k, out, sizes, dist, "unsorted", factory, raw, compression);
                        evaluate(k, out, sizes, dist, "sorted", factory, sorted, compression);
                    }
                }
            }
            System.out.printf("%d\n", k);
        }
        sizes.close();
        out.close();
    }

    private void evaluate(int k, PrintWriter out, PrintWriter sizes,
                          Util.Distribution dist, String sort, Util.Factory factory,
                          double[] data, double compression) {
        TDigest digest = factory.create(compression);
        for (double datum : data) {
            digest.add(datum);
        }
        double qx = 0;
        for (Centroid centroid : digest.centroids()) {
            double dq = (double) centroid.count() / N;
            double k0 = kIndex(qx, compression);
            double k1 = kIndex(qx + dq, compression);
            sizes.printf("%s,%s,%s,%.8f,%.8f,%.8f,%.8g,%.0f,%d,%d\n", factory, dist, sort, qx, qx + dq, k1 - k0, centroid.mean(), compression, centroid.count(), k);
            qx += dq;
        }
        for (double q : new double[]{0.0001, 0.001, 0.01, 0.1, 0.5, 0.9, 0.99, 0.999, 0.9999}) {
            double x = digest.quantile(q);
            double q0 = Util.cdf(x, data);
            double error = (q - q0) / Math.min(q, 1 - q);
            out.printf("%s,%s,%s,%.8f,%.8f,%.8g,%.0f,%.8g,%d\n", factory, dist, sort, q, q0, error, compression, x, k);
        }
    }

    private double kIndex(double q, double compression) {
        double u = 2 * q - 1;
        return Math.acos(-u) * compression / Math.PI;
    }
}
    
