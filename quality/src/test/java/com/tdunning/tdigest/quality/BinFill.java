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

import com.google.common.collect.Lists;
import com.tdunning.math.stats.Centroid;
import com.tdunning.math.stats.Dist;
import com.tdunning.math.stats.MergingDigest;
import com.tdunning.math.stats.ScaleFunction;
import com.tdunning.math.stats.TDigest;
import org.apache.mahout.math.jet.random.AbstractDistribution;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Random;

/**
 * Plots the size of each bin for various distributions and parameters.
 * <p>
 * The bin-fill.r program can run in the same directory as this program to get some
 * visualization about how well clusters are filled.
 */
public class BinFill {
    @Test
    public void sampleFill() {
        System.out.printf("scale,delta,centroid,mean,count\n");
        for (double delta : new double[]{5, 10}) {
            double[] data = {0, 0, 3, 4, 1, 6, 0, 5, 2, 0, 3, 3, 2, 3, 0, 2, 5, 0, 3, 1};

            MergingDigest t1 = new MergingDigest(delta);
            t1.setScaleFunction(ScaleFunction.K_1);

            MergingDigest t2 = new MergingDigest(delta);
            t2.setScaleFunction(ScaleFunction.K_2);

            MergingDigest t3 = new MergingDigest(delta);
            t3.setScaleFunction(ScaleFunction.K_3);
            for (double x : data) {
                t1.add(x);
                t2.add(x);
                t3.add(x);
            }


            int i = 1;
            for (MergingDigest t : Lists.newArrayList(t1, t2, t3)) {
                System.out.printf("> %d, %.0f, %.5f, %.5f\n", i, delta, t.quantile(0.65), Dist.quantile(0.65, data));
                int j = 0;
                for (Centroid centroid : t.centroids()) {
                    System.out.printf("%d,%.0f,%d,%.5f,%d\n", i, delta, j, centroid.mean(), centroid.count());
                    j++;
                }
                i++;
            }
        }
    }

    private static final double N = 100000;

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
                                dist.setScaleFunction(f);
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
