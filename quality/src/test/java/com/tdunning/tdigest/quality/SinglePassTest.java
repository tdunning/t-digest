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
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.*;

/**
 * By setting the buffer size on the MergingDigest to larger than the number of data points,
 * we get to see the theoretical performance of a t-digest.
 */
public class SinglePassTest {
    private static final int N = 200000;

    /**
     * This test builds t-digests in a single pass with such a large buffer that all of the data is
     * sorted in one batch. This avoids questions about the accuracy of the merging strategy and tests
     * the basic error rates from the idea of the t-digest itself.
     *
     * This test produces two data files that describe the results of the test.
     *
     * The first file is called limit-errors.csv. It contains data about the accuracy of the t-digest
     * at values of q that are evenly spaced in logit space (i.e. even spacing of log10(q/1-q)). This
     * results in points that are closely spaced near q=0 and near q=1. At each point, the value of q,
     * the corresponding quantile estimate x1=F^{-1}(q), the actual value x1 (from the data samples),
     * the round-trip quantile q1=F(x) as estimated by the t-digest and the actual round-trip quantile
     * q2 as computed from the original data are given.
     *
     * The second file is called limit-sizes.csv and gives the centroid weights and locations in terms
     * of x and q for each centroid in the t-digest. In addition, q2=F(x) and x2=F^{-1}(q) are given as
     * estimated from the original data.
     *
     * All of these tests are done under a variety of parameter settings including compression from 10 to
     * 500, centroid merging strategy and such.
     *
     *
     * @throws FileNotFoundException If output files can't be opened.
     */
    @Test
    public void testConservativeBuild() throws FileNotFoundException {
        try (PrintWriter errors = new PrintWriter(new FileOutputStream("limit-errors.csv"));
             PrintWriter buckets = new PrintWriter(new FileOutputStream("limit-sizes.csv"))) {
            errors.printf("pass,x1,x2,q,q1,q2,error,compression,conservative\n");
            buckets.printf("pass,compression,conservative,i,q,mean,count,q2,x2\n");

            Random gen = new Random();
            for (int pass = 0; pass < 50; pass++) {
                System.out.printf("%d\n", pass);
                for (boolean conservative : new boolean[]{true, false}) {
                    String flag = conservative ? "conservative" : "aggressive";
                    for (double compression : new double[]{20, 50, 100, 200, 300, 500}) {
                        double[] data = new double[N];
                        MergingDigest digest = new MergingDigest(compression, 2 * N);
                        digest.limitType = conservative;

                        for (int i = 0; i < N; i++) {
                            double x = gen.nextDouble();
                            data[i] = x;
                            digest.add(x);
                        }

                        Arrays.sort(data);
                        int i = 0;
                        double sum = 0;
                        for (Centroid centroid : digest.centroids()) {
                            double q = (sum + centroid.count() / 2.0)/digest.size();
                            sum += centroid.count();
                            buckets.printf("%d,%.1f,%s,%d,%.12f,%.12f,%d,%.12f,%.12f\n",
                                    pass, compression, flag, i++, q, centroid.mean(), centroid.count(),
                                    Util.cdf(centroid.mean(), data), Util.quantile(q, data));
                        }
                        if (sum != digest.size()) {
                            System.out.printf("Oops ... total mismatch %.5f != %5d\n", sum, digest.size());
                        }

                        for (double lq =-6; lq < 6.01; lq += 0.25) {
                            double q = 1 / (1 + Math.pow(10, -lq));
                            double x1 = Util.quantile(q, data);
                            double x2 = digest.quantile(q);
                            double q1 = digest.cdf(x1);
                            double q2 = Util.cdf(x1, data);
                            errors.printf("%d,%.12f,%.12f,%.12f,%.12f,%.12f,%.12f,%.0f,%s\n",
                                    pass, x1, x2, q, q1, q2, Math.abs(q1 - q2) / q1, compression, flag);
                        }
                    }
                }
            }
        }
    }
}
