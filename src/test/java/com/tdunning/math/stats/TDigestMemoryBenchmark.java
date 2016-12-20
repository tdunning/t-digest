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
