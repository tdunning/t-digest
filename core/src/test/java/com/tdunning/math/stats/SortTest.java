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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SortTest {
    @Test
    public void testReverse() throws Exception {
        int[] x = new int[0];

        // don't crash with no input
        Sort.reverse(x);

        // reverse stuff!
        x = new int[]{1, 2, 3, 4, 5};
        Sort.reverse(x);
        for (int i = 0; i < 5; i++) {
            assertEquals(5 - i, x[i]);
        }

        // reverse some stuff back
        Sort.reverse(x, 1, 3);
        assertEquals(5, x[0]);
        assertEquals(2, x[1]);
        assertEquals(3, x[2]);
        assertEquals(4, x[3]);
        assertEquals(1, x[4]);

        // another no-op
        Sort.reverse(x, 3, 0);
        assertEquals(5, x[0]);
        assertEquals(2, x[1]);
        assertEquals(3, x[2]);
        assertEquals(4, x[3]);
        assertEquals(1, x[4]);

        x = new int[]{1, 2, 3, 4, 5, 6};
        Sort.reverse(x);
        for (int i = 0; i < 6; i++) {
            assertEquals(6 - i, x[i]);
        }
    }

    @Test
    public void testEmpty() {
        Sort.sort(new int[]{}, new double[]{});
    }

    @Test
    public void testOne() {
        int[] order = new int[1];
        Sort.sort(order, new double[]{1});
        assertEquals(0, order[0]);
    }

    @Test
    public void testIdentical() {
        int[] order = new int[6];
        double[] values = new double[6];

        Sort.sort(order, values);
        checkOrder(order, values);
    }

    @Test
    public void testRepeated() {
        int n = 50;
        int[] order = new int[n];
        double[] values = new double[n];
        for (int i = 0; i < n; i++) {
            values[i] = Math.rint(10 * ((double) i / n)) / 10.0;
        }

        Sort.sort(order, values);
        checkOrder(order, values);
    }

    @Test
    public void testShort() {
        int[] order = new int[6];
        double[] values = new double[6];

        // all duplicates
        for (int i = 0; i < 6; i++) {
            values[i] = 1;
        }

        Sort.sort(order, values);
        checkOrder(order, values);

        values[0] = 0.8;
        values[1] = 0.3;

        Sort.sort(order, values);
        checkOrder(order, values);

        values[5] = 1.5;
        values[4] = 1.2;

        Sort.sort(order, values);
        checkOrder(order, values);
    }

    @Test
    public void testLonger() {
        int[] order = new int[20];
        double[] values = new double[20];
        for (int i = 0; i < 20; i++) {
            values[i] = (i * 13) % 20;
        }
        Sort.sort(order, values);
        checkOrder(order, values);
    }

    @Test
    public void testMultiPivots() {
        // more pivots than low split on first pass
        // multiple pivots, but more low data on second part of recursion
        int[] order = new int[30];
        double[] values = new double[30];
        for (int i = 0; i < 9; i++) {
            values[i] = i + 20 * (i % 2);
        }

        for (int i = 9; i < 20; i++) {
            values[i] = 10;
        }

        for (int i = 20; i < 30; i++) {
            values[i] = i - 20 * (i % 2);
        }
        values[29] = 29;
        values[24] = 25;
        values[26] = 25;

        Sort.sort(order, values);
        checkOrder(order, values);
    }

    @Test
    public void testMultiPivotsInPlace() {
        // more pivots than low split on first pass
        // multiple pivots, but more low data on second part of recursion
        double[] keys = new double[30];
        for (int i = 0; i < 9; i++) {
            keys[i] = i + 20 * (i % 2);
        }

        for (int i = 9; i < 20; i++) {
            keys[i] = 10;
        }

        for (int i = 20; i < 30; i++) {
            keys[i] = i - 20 * (i % 2);
        }
        keys[29] = 29;
        keys[24] = 25;
        keys[26] = 25;

        double[] v = valuesFromKeys(keys, 0);

        Sort.sort(keys, v);
        checkOrder(keys, 0, keys.length, v);
    }

    @Test
    public void testRandomized() {
        Random rand = new Random();

        for (int k = 0; k < 100; k++) {
            int[] order = new int[30];
            double[] values = new double[30];
            for (int i = 0; i < 30; i++) {
                values[i] = rand.nextDouble();
            }

            Sort.sort(order, values);
            checkOrder(order, values);
        }
    }

    @Test
    public void testRandomizedShortSort() throws Exception {
        Random rand = new Random();

        for (int k = 0; k < 100; k++) {
            double[] keys = new double[30];
            for (int i = 0; i < 10; i++) {
                keys[i] = i;
            }
            for (int i = 10; i < 20; i++) {
                keys[i] = rand.nextDouble();
            }
            for (int i = 20; i < 30; i++) {
                keys[i] = i;
            }
            double[] v0 = valuesFromKeys(keys, 0);
            double[] v1 = valuesFromKeys(keys, 1);

            Sort.sort(keys, 10, 10, v0, v1);
            checkOrder(keys, 10, 10, v0, v1);
            checkValues(keys, 0, keys.length, v0, v1);
            for (int i = 0; i < 10; i++) {
                assertEquals(i, keys[i], 0);
            }
            for (int i = 20; i < 30; i++) {
                assertEquals(i, keys[i], 0);
            }
        }
    }

    /**
     * Generates a vector of values corresponding to a vector of keys.
     *
     * @param keys A vector of keys
     * @param k    Which value vector to generate
     * @return The new vector containing frac(key_i * 3 * 5^k)
     */
    private double[] valuesFromKeys(double[] keys, int k) {
        double[] r = new double[keys.length];
        double scale = 3;
        for (int i = 0; i < k; i++) {
            scale = scale * 5;
        }
        for (int i = 0; i < keys.length; i++) {
            r[i] = fractionalPart(keys[i] * scale);
        }
        return r;
    }

    /**
     * Verifies that keys are in order and that each value corresponds to the keys
     *
     * @param key    Array of keys
     * @param start  The starting offset of keys and values to check
     * @param length The number of keys and values to check
     * @param values Arrays of associated values. Value_{ki} = frac(key_i * 3 * 5^k)
     */
    private void checkOrder(double[] key, int start, int length, double[]... values) {
        assert start + length <= key.length;

        for (int i = start; i < start + length - 1; i++) {
            assertTrue(String.format("bad ordering at %d, %f > %f", i, key[i], key[i + 1]), key[i] <= key[i + 1]);
        }

        checkValues(key, start, length, values);
    }

    private void checkValues(double[] key, int start, int length, double[]... values) {
        double scale = 3;
        for (int k = 0; k < values.length; k++) {
            double[] v = values[k];
            assertEquals(key.length, v.length);
            for (int i = start; i < length; i++) {
                assertEquals(String.format("value %d not correlated, key=%.5f, k=%d, v=%.5f", i, key[i], k, values[k][i]),
                        fractionalPart(key[i] * scale), values[k][i], 0);
            }
            scale = scale * 5;
        }
    }


    private double fractionalPart(double v) {
        return v - Math.floor(v);
    }

    private void checkOrder(int[] order, double[] values) {
        double previous = -Double.MAX_VALUE;
        Multiset<Integer> counts = HashMultiset.create();
        for (int i = 0; i < values.length; i++) {
            counts.add(i);
            double v = values[order[i]];
            if (v < previous) {
                throw new IllegalArgumentException("Values out of order at %d");
            }
            previous = v;
        }

        assertEquals(order.length, counts.size());

        for (Integer count : counts) {
            assertEquals(1, counts.count(count));
        }
    }
}