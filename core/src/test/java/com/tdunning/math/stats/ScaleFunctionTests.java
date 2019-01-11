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

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Validate internal consistency of scale functions.
 */
public class ScaleFunctionTests {
    @Test
    public void asinApproximation() {
        for (double x = 0; x < 1; x += 1e-4) {
            assertEquals(Math.asin(x), ScaleFunction.fastAsin(x), 1e-6);
        }
        assertEquals(Math.asin(1), ScaleFunction.fastAsin(1), 0);
        assertTrue(Double.isNaN(ScaleFunction.fastAsin(1.0001)));
    }

    /**
     * Test that the basic single pass greedy t-digest construction has expected behavior with all scale functions.
     * <p>
     * This also throws off a diagnostic file that can be visualized if desired under the name of
     * scale-function-sizes.csv
     */
    @Test
    public void testSize() throws FileNotFoundException {
        try (PrintWriter out = new PrintWriter("scale-function-sizes.csv")) {
            out.printf("alg,delta,n,m,singles\n");
            for (double compression : new double[]{20, 50, 100, 200, 500}) {
                for (double n : new double[]{100, 200, 500, 1e3, 5e3, 10e3, 100e3, 1e6}) {
                    Map<String, Integer> clusterCount = new HashMap<>();
                    for (ScaleFunction k : ScaleFunction.values()) {
                        if (k.toString().equals("K_0")) {
                            continue;
                        }
                        double k0 = k.k(0, compression, n);
                        int m = 0;
                        int singles = 0;
                        for (int i = 0; i < n; ) {
                            double cnt = 1;
                            while (i + cnt < n && k.k((i + cnt) / (n - 1), compression, n) - k0 < 1) {
                                cnt++;
                            }
                            if (cnt == 1) {
                                singles++;
                            }
                            double size = max(k.max(i / (n - 1), compression, n), k.max((i + cnt) / (n - 1), compression, n));

                            // check that we didn't cross the midline (which makes the size limit very conservative)
                            double left = i - (n - 1) / 2;
                            double right = i + cnt - (n - 1) / 2;
                            boolean sameSide = left * right > 0;
                            if (!k.toString().endsWith("NO_NORM") && sameSide) {
                                assertTrue(String.format("%s %.0f %.0f %.3f vs %.3f @ %.3f", k, compression, n, cnt, size, i / (n - 1)),
                                        cnt == 1 || cnt <= max(1.1 * size, size + 1));
                            }
                            i += cnt;
                            k0 = k.k(i / (n - 1), compression, n);
                            m++;
                        }
                        clusterCount.put(k.toString(), m);
                        out.printf("%s,%.0f,%.0f,%d,%d\n", k, compression, n, m, singles);

                        if (!k.toString().endsWith("NO_NORM")) {
                            assertTrue(String.format("%s %d, %.0f", k, m, compression),
                                    n < 3 * compression || (m >= compression / 3 && m <= compression));
                        }
                    }
                    // make sure that the approximate version gets same results
                    assertEquals(clusterCount.get("K_1"), clusterCount.get("K_1_FAST"));
                }
            }
        }
    }

    /**
     * Validates the fast asin approximation
     */
    @Test
    public void testApproximation() {
        double worst = 0;
        double old = Double.NEGATIVE_INFINITY;
        for (double x = -1; x < 1; x += 0.00001) {
            double ex = Math.asin(x);
            double actual = ScaleFunction.fastAsin(x);
            double error = ex - actual;
//            System.out.printf("%.8f, %.8f, %.8f, %.12f\n", x, ex, actual, error * 1e6);
            assertEquals("Bad approximation", 0, error, 1e-6);
            assertTrue("Not monotonic", actual >= old);
            worst = Math.max(worst, Math.abs(error));
            old = actual;
        }
        assertEquals(Math.asin(1), ScaleFunction.fastAsin(1), 0);
        System.out.printf("worst = %.5g\n", worst);
    }

    /**
     * Validates that the forward and reverse scale functions are as accurate as intended.
     */
    @Test
    public void testInverseScale() {
        for (ScaleFunction f : ScaleFunction.values()) {
            double tolerance = f.toString().contains("FAST") ? 2e-4 : 1e-10;
            System.out.printf("F = %s\n", f);

            for (double n : new double[]{1000, 3000, 10000, 100000}) {
                double epsilon = 1.0 / n;
                for (double compression : new double[]{20, 100, 1000}) {
                    double oldK = f.k(0, compression, n);
                    for (int i = 1; i < n; i++) {
                        double q = i / n;
                        double k = f.k(q, compression, n);
                        assertTrue(String.format("monoticity %s(%.0f, %.0f) @ %.5f", f, compression, n, q),
                                k > oldK);
                        oldK = k;

                        double qx = f.q(k, compression, n);
                        double kx = f.k(qx, compression, n);
                        assertEquals(String.format("Q: %s(%.0f, %.0f) @ %.5f", f, compression, n, q), q, qx, 1e-6);
                        double absError = abs(k - kx);
                        double relError = absError / max(0.01, max(abs(k), abs(kx)));
                        assertEquals(
                                String.format("K: %s(%.0f, %.0f) @ %.5f [%.5g, %.5g]",
                                        f, compression, n, q, absError, relError),
                                0, absError, tolerance);
                        assertEquals(
                                String.format("K: %s(%.0f, %.0f) @ %.5f [%.5g, %.5g]",
                                        f, compression, n, q, absError, relError),
                                0, relError, tolerance);
                    }
                    assertTrue(f.k(0, compression, n) < f.k(epsilon, compression, n));
                    assertTrue(f.k(1, compression, n) > f.k(1 - epsilon, compression, n));
                }
            }
        }
    }
}
