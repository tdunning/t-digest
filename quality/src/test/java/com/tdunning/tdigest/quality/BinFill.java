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

package com.tdunning.tdigest.quality;

import com.tdunning.math.stats.Centroid;
import com.tdunning.math.stats.MergingDigest;
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
            for (Util.Factory factory : Util.Factory.values()) {
                for (Util.Distribution distribution : Util.Distribution.values()) {
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
                            double k1 = MergingDigest.integratedLocation(q1, dist.compression(), dist.size());
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
