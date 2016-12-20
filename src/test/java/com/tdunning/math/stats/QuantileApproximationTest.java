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

import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

import static junit.framework.Assert.assertEquals;

/**
 * Tests improved algorithms for estimating quantiles from centroids
 * that takes into account special cases such as a centroid with a
 * single sample at the boundary.
 */
public class QuantileApproximationTest {
    /**
     * An idealized t-digest is constructed by scanning a sorted list of samples,
     * grouping them into groups that are as large as possible without exceeding
     * the classic t-digest size limit.
     */
    private static class IdealTDigest {
        private double[] centroids;
        private int[] samples;
        private int kMax;
        private int count;

        public IdealTDigest(double compression, double[] data) {
            Arrays.sort(data);
            int n = data.length;
            double q0 = 0;
            double k0 = qTok(q0, compression);
            int m = (int) (2 * Math.ceil(Math.PI * compression) + 1);
            centroids = new double[m];
            samples = new int[m];

            int k = 0;
            for (int i = 0; i < n; i++) {
                double q1 = (double) (i + 1) / n;
                double k1 = qTok(q1, compression);
                count++;
                if (samples[k] == 0 || (k1 <= k0 + 1 && k != 0 && i != n - 1)) {
                    // the condition here guarantees all centroids have at least one sample
                    // and that each centroid meets the size limit
                    // except that the first centroid (k!=0) can have only one sample
                    // and except that the last point (i != n-1) can't be included
                    samples[k]++;
                    centroids[k] += (data[i] - centroids[k]) / samples[k];
                } else {
                    // failed the test to be included in the live centroid so we need to
                    // create a new one
                    k++;
                    samples[k] = 1;
                    centroids[k] = data[i];
                    k0 = k1;
                }
                kMax = k;
            }
        }

        /**
         * Converts quantile scale (q) to centroid index scale (k). The t-digest size
         * limit can be stated by saying that no centroid spans a range of more than 1
         * in k.
         *
         * @param q           Number of samples to the left / n
         * @param compression The scale factor for k which determines the max number of centroids
         * @return The value of k corresponding to q.
         */
        public double qTok(double q, double compression) {
            return (Math.asin(2 * q - 1) + Math.PI / 2) * compression;
        }

        double quantile(double q) {
            double index = q * count;
            if (index <= 0.5) {
                return centroids[0];
            } else if (index >= count - 0.5) {
                return centroids[kMax];
            }
            // note 0 < q < 1 implies 0 < index < count
            double sum = samples[0] / 2.0;
            for (int k = 0; k < kMax; k++) {
                // assume samples for a centroid are distributed equally on both sides
                double interval = (samples[k] + samples[k + 1]) / 2.0;
                if (sum + interval > index) {
                    return (index - sum) / interval * (centroids[k + 1] - centroids[k]) + centroids[k];
                } else {
                    sum += interval;
                }
            }
            assert Math.abs(count - sum - 0.5) < count * 1e-8;
            return centroids[kMax];
        }

        double cdf(double x) {
            if (x < centroids[0]) {
                return 0;
            } else if (x >= centroids[kMax]) {
                return 1;
            } else {
                assert x >= centroids[0];
                assert x < centroids[kMax];

                double sum = 0;
                int k = 0;
                // loop will exit before last centroid
                while (x >= centroids[k + 1]) {
                    sum += samples[k];
                    k++;
                }
                assert k >= 0;
                assert k < kMax;
                assert x >= centroids[k];
                assert x < centroids[k + 1];

                double proRata = (x - centroids[k]) / (centroids[k + 1] - centroids[k]);
                sum += samples[k] / 2.0 + proRata * (samples[k] + samples[k + 1]) / 2.0;
                return sum / count;
            }
        }
    }

    @Test
    public void testExtremes() throws Exception {
        // This verifies the limit cases for a t-digest including
        //  - min and max centroids have only one sample
        //  - cdf for x<x_0 or x>x_max are 0 and 1 respectively
        //  - cdf shows a jump to 1/2n on the left and down to 1-1/2n on the right
        double[] data = generateData(100);
        IdealTDigest t = new IdealTDigest(5, data);
        assertEquals(1, t.samples[0]);
        assertEquals(1, t.samples[t.kMax]);
        assertEquals(0, t.cdf(data[0] - 1e-10), 0);
        assertEquals(1, t.cdf(data[99] + 1e-10), 0);
        assertEquals(0.5 / 100, t.cdf(data[0] + 1e-10), 1e-8);
        assertEquals(99.5 / 100, t.cdf(data[99] - 1e-10), 1e-8);
    }

    @Test
    public void testSmallCountQuantiles() {
        double[] data = generateData(10);
        IdealTDigest t = new IdealTDigest(200, data);
        for (double q = -0.01; q < 1.01; q += 0.01) {
            double expected = sampleQuantile(data, q);
            double actual = t.quantile(q);
            assertEquals(String.format("q = %.3f", q), expected, actual, 1e-6);
        }
    }

    private double sampleQuantile(double[] data, double q) {
        if (q < 0) {
            return data[0];
        } else if (q >= 1) {
            return data[data.length - 1];
        }
        double index = q * data.length;
        if (index < 0.5) {
            return data[0];
        } else if (index >= data.length - 1.5) {
            return data[data.length - 1];
        } else {
            index -= 0.5;
            int left = (int) Math.floor(index);
            if (left >= data.length - 1) {
                // can only plausibly happen with subtle round-off issues because we know q<1
                return data[data.length - 1];
            }
            int right = left + 1;
            return (index - left) * (data[right] - data[left]) + data[left];
        }
    }

    @Test
    public void testFillIdealTD() throws Exception {
        double[] data = generateData(1000000);
        double dataSum = sum(data);

        IdealTDigest t = new IdealTDigest(100, data);
        assertEquals(1, t.samples[0]);
        assertEquals(1, t.samples[t.kMax]);
        int count = 0;
        double sum = 0;
        for (int k = 0; k <= t.kMax; k++) {
            count += t.samples[k];
            sum += t.centroids[k] * t.samples[k];
        }
        assertEquals(data.length, count);
        assertEquals(dataSum, sum, sum * 1e-12);

        for (int i = 0; i < t.kMax; i++) {
            System.out.printf("%d,%d,%.7f\n", i, t.samples[i], t.centroids[i]);
        }
    }

    private double sum(double[] data) {
        double dataSum = 0;
        for (double aData : data) {
            dataSum += aData;
        }
        return dataSum;
    }

    private double[] generateData(int n) {
        double[] data = new double[n];
        Random rand = new Random();
        for (int i = 0; i < data.length; i++) {
            data[i] = rand.nextDouble();
        }
        return data;
    }
}
