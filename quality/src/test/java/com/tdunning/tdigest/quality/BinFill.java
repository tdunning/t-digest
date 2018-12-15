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
import com.tdunning.math.stats.ScaleFunction;
import com.tdunning.math.stats.TDigest;
import org.apache.mahout.math.jet.random.AbstractDistribution;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Random;

/**
 * Plots the size of each bin for various distributions and parameters.
 *
 * The bin-fill.r program can run in the same directory as this program to get some
 * visualization about how well clusters are filled.
 */
public class BinFill {

    public static final double N = 100000;

    public static void main(String[] args) throws FileNotFoundException {
        try (PrintWriter out = new PrintWriter("bin-fill.csv")) {
            out.printf("iteration,dist,algo,scale,q,x,k0,k1,dk,q0,q1,count,max0,max1\n");

            // for all scale functions except the non-normalized ones
            for (ScaleFunction f : ScaleFunction.values()) {
                if (f.toString().contains("NO_NORM")) {
                    continue;
                }
                System.out.printf("%s\n", f);

                // for all kinds of t-digests
                for (Util.Factory factory : Util.Factory.values()) {
                    // for different distributions of values
                    for (Util.Distribution distribution : Util.Distribution.values()) {
                        AbstractDistribution gen = distribution.create(new Random());
                        // do multiple passes
                        for (int i = 0; i < 10; i++) {
                            TDigest dist = factory.create();
                            if (dist instanceof MergingDigest) {
                                // can only set scale function on merging digest right now ...
                                // ability for TreeDigest coming soon
                                ((MergingDigest) dist).setScaleFunction(f);
                            }
                            for (int j = 0; j < N; j++) {
                                dist.add(gen.nextDouble());
                            }

                            // now dump stats for the centroids
                            double q0 = 0;
                            double k0 = 0;
                            for (Centroid c : dist.centroids()) {
                                double q1 = q0 + (double) c.count() / N;
                                double k1 = f.k(q1, dist.compression(), dist.size());
                                out.printf("%d,%s,%s,%s,%.7f,%.7f,%.7f,%.7f,%.7f,%.7f,%.7f,%d,%.1f,%.1f\n",
                                        i, distribution, factory, f, (q0 + q1) / 2, c.mean(),
                                        k0, k1, k1 - k0, q0, q1, c.count(),
                                        f.max(q0, dist.compression(), dist.size()),
                                        f.max(q1, dist.compression(), dist.size())
                                );
                                q0 = q1;
                                k0 = k1;
                            }
                        }
                    }
                }
            }
        }
    }
}
