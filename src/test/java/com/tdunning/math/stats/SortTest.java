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
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

public class SortTest {
    @Test
    public void testEmpty() {
        Sort.sort(new int[]{}, new double[]{});
    }

    @Test
    public void testOne() {
        int[] order = new int[1];
        Sort.sort(order, new double[]{1});
        Assert.assertEquals(0, order[0]);
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

        Assert.assertEquals(order.length, counts.size());

        for (Integer count : counts) {
            Assert.assertEquals(1, counts.count(count));
        }
    }
}