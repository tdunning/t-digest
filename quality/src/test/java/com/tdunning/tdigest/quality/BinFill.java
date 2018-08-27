package com.tdunning;

import com.tdunning.math.stats.Centroid;
import com.tdunning.math.stats.TDigest;
import org.apache.mahout.math.jet.random.AbstractDistribution;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Random;

/**
 * Plots the size of each bin for various distributions.
 */
public class BinFill {

    public static final double N = 100000;

    public static void main(String[] args) throws FileNotFoundException {
        try (PrintWriter out = new PrintWriter("bin-fill.csv")) {
            out.printf("iteration,dist,algo,q,x,k0,k1,dk,q0,q1,count\n");
            for (TDigestBench.TDigestFactory factory : TDigestBench.TDigestFactory.values()) {
                for (TDigestBench.DistributionFactory distribution : TDigestBench.DistributionFactory.values()) {
                    AbstractDistribution gen = distribution.create(new Random());
                    for (int i = 0; i < 10; i++) {
                        TDigest dist = factory.create();
                        for (int j = 0; j < N; j++) {
                            dist.add(gen.nextDouble());
                        }
                        double q0 = 0;
                        double k0 = 0;
                        for (Centroid c : dist.centroids()) {
                            double q1 = q0 + (double) c.count() / N;
                            double k1 = TDigest.integratedLocation(20, q1);
                            out.printf("%d,%s,%s,%.7f,%.7f,%.7f,%.7f,%.7f,%.7f,%.7f,%d\n",
                                    i, distribution, factory, (q0 + q1) / 2, c.mean(),
                                    k0, k1, k1 - k0, q0, q1, c.count());
                            q0 = q1;
                            k0 = k1;
                        }
                    }
                }
            }
        }
    }
}
