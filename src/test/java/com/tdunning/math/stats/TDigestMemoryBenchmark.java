package com.tdunning.math.stats;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import com.carrotsearch.sizeof.RamUsageEstimator;
import com.tdunning.math.stats.TDigestBenchmark.TDigestFactory;

public class TDigestMemoryBenchmark {

    private static final double COMPRESSION = 100;

    public static void main(String[] args) {
        System.out.println("Average bytes per centroid");
        System.out.println("==========================");
        Random random = ThreadLocalRandom.current();
        for (TDigestFactory factory : TDigestBenchmark.TDigestFactory.values()) {
            TDigest tdigest = factory.create(COMPRESSION);
            // Use a uniform distribution to know the average case
            for (int i = 0; i < 10000; ++i) {
                tdigest.add(random.nextDouble());
            }
            System.out.println(factory + "\t" + memoryUsagePerCentroid(tdigest));
        }
    }

    private static double memoryUsagePerCentroid(TDigest tdigest) {
        final long totalSize = RamUsageEstimator.sizeOf(tdigest);
        return (double) totalSize / tdigest.centroids().size();
    }

}
