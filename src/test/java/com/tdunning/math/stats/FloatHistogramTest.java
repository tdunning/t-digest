/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
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

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FloatHistogramTest {
    @Test
    public void testEmpty() {
        Histogram x = new FloatHistogram(1, 100);
        double[] bins = x.getBounds();
        assertEquals(1, bins[0], 1e-5);
        assertTrue(bins[bins.length - 1] >= 100.0);
        assertTrue(bins.length >= 100);
    }

    @Test
    public void testLinear() throws FileNotFoundException {
        int n = 10000;
        int trials = 1000;
        // this should be about 160 by theory since that is where the cuts go above 1, but
        // there is some small residual issue that causes it to be a bit bigger.
        // these values are empirical against the current implementation. The fact that many counts are small may
        // be the reason for the slight deviation.
        double idealChi2 = 165.4;
        double chi2SD = 18;
        double[] min = new double[212];
        Arrays.fill(min, 1);
        double[] max = new double[212];
        int n1above = 0;
        int n2above = 0;
        int n1below = 0;
        int n2below = 0;
        PrintWriter out = new PrintWriter("data.csv");
        try {
            out.print("j,cut,low,high,k,expected,err\n");
            double mean = 0;
            double sd = 0;
            for (int j = 0; j < trials; j++) {
                FloatHistogram x = new FloatHistogram(1e-3, 10);
                long[] counts = x.getCounts();
                double[] cuts = x.getBounds();
                assertEquals(min.length, cuts.length);
                assertEquals(max.length, cuts.length);

                Random rand = new Random();
                for (int i = 0; i < n; i++) {
                    double u = rand.nextDouble();
                    x.add(u);
                    int k = x.bucket(u);
                    min[k] = Math.min(min[k], u);
                    max[k] = Math.max(max[k], u);
                }
                double sum = 0;
                double maxErr = 0;
                int i = 0;
                while (i < cuts.length - 1 && cuts[i] < 1.1) {
                    double lowBound = Math.min(1, cuts[i]);
                    if (i == 0) {
                        lowBound = 0;
                    }
                    double highBound = Math.min(1, cuts[i + 1]);
                    double expected = n * (highBound - lowBound);


                    double err = 0;
                    if (counts[i] > 0) {
                        err = counts[i] * Math.log(counts[i] / expected);
                        sum += err;

                        if (i > 0) {
                            assertTrue(String.format("%d: %.5f > %.5f", i, cuts[i], min[i]), cuts[i] <= min[i]);
                        }
                        assertTrue(String.format("%d: %.5f > %.5f", i, max[i], cuts[i + 1]), cuts[i + 1] >= max[i]);
                    }
                    out.printf("%d,%.4f,%.4f,%.4f,%d,%.4f,%.1f\n", j, cuts[i], lowBound, highBound, counts[i], expected, err);
                    maxErr = Math.max(maxErr, err);
                    i++;
                }
                while (i < cuts.length) {
                    assertEquals(0, counts[i]);
                    i++;
                }
                sum = 2 * sum;
                if (sum > idealChi2 + 3 * chi2SD) {
                    n2above++;
                }
                if (sum > idealChi2 + 2 * chi2SD) {
                    n1above++;
                }
                if (sum < idealChi2 - 3 * chi2SD) {
                    n2below++;
                }
                if (sum < idealChi2 - 2 * chi2SD) {
                    n1below++;
                }
                double old = mean;
                mean += (sum - mean) / (j + 1);
                sd += (sum - mean) * (sum - old);
            }
            System.out.printf("Chi^2 against ideal = %.4f ± %.1f\n", mean, Math.sqrt(sd / trials));
            // verify that the chi^2 score for counts is as expected
            assertEquals("chi^2 > expect + 2*sd too often", 0, n1above, 0.05 * trials);
            // 3*sigma above for a chi^2 distribution happens more than you might think
            assertEquals("chi^2 > expect + 3*sd too often", 0, n2above, 0.01 * trials);
            // the bottom side of the chi^2 distribution is a bit tighter
            assertEquals("chi^2 < expect - 2*sd too often", 0, n1below, 0.03 * trials);
            assertEquals("chi^2 < expect - 3*sd too often", 0, n2below, 0.06 * trials);
        } finally {
            out.close();
        }
    }

    /**
     * The point of this test is to make sure that the floating point representation
     * can be used as a quick approximation of log_2
     *
     * @throws FileNotFoundException If we can't open an output file
     */
    @Test
    public void testFitToLog() throws FileNotFoundException {
        double scale = Math.pow(2, 52);
        double x = 0.001;
        // 4 bits, worst case is mid octave
        double lowerBound = 1 / 16.0 * Math.sqrt(2);
        PrintWriter out = new PrintWriter("log-fit.csv");
        try {
            out.printf("x,y1,y2\n");
            while (x < 10) {
                long xz = Double.doubleToLongBits(x);
                // the magic 0x3ff is the offset for the floating point exponent
                double v1 = xz / scale - 0x3ff;
                double v2 = Math.log(x) / Math.log(2);
                out.printf("%.6f,%.6f,%.6f\n", x, v1, v2);
                assertTrue(v2 - v1 > 0);
                assertTrue(v2 - v1 < lowerBound);
                x *= 1.02;
            }
        } finally {
            out.close();
        }
    }

    @Test
    public void testCompression() throws Exception {
        int n = 1000000;
        FloatHistogram x = new FloatHistogram(1e-3, 10);

        Random rand = new Random();
        for (int i = 0; i < n; i++) {
            x.add(rand.nextDouble());
        }
        long[] compressed = x.getCompressedCounts();
        System.out.printf("%d\n", compressed.length);
        long[] uncompressed = new long[x.getCounts().length];
        long[] counts = x.getCounts();

        int k = Simple64.decompress(LongBuffer.wrap(compressed), uncompressed);
        assertEquals(k, counts.length);
        for (int i = 0; i < uncompressed.length; i++) {
            assertEquals(counts[i], uncompressed[i]);
        }
    }
}