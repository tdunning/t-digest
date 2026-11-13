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

import org.apache.mahout.common.RandomUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

public class AVLGroupTreeTest extends AbstractTest {

    @Before
    public void setUp() {
        RandomUtils.useTestSeed();
    }

    @Test
    public void testSimpleAdds() {
        AVLGroupTree x = new AVLGroupTree(false);
        assertEquals(IntAVLTree.NIL, x.floor(34));
        assertEquals(IntAVLTree.NIL, x.first());
        assertEquals(IntAVLTree.NIL, x.last());
        assertEquals(0, x.size());
        assertEquals(0, x.sum());

        x.add(new Centroid(1));
        assertEquals(1, x.sum());
        Centroid centroid = new Centroid(2);
        centroid.add(3, 1);
        centroid.add(4, 1);
        x.add(centroid);

        assertEquals(2, x.size());
        assertEquals(4, x.sum());
    }

    @Test
    public void testBalancing() {
        AVLGroupTree x = new AVLGroupTree(false);
        for (int i = 0; i < 101; i++) {
            x.add(new Centroid(i));
        }

        assertEquals(101, x.size());
        assertEquals(101, x.sum());

        x.checkBalance();
        x.checkAggregates();
    }

    @Test
    public void testFloor() {
        // mostly tested in other tests
        AVLGroupTree x = new AVLGroupTree(false);
        for (int i = 0; i < 101; i++) {
            x.add(new Centroid(i / 2));
        }

        assertEquals(IntAVLTree.NIL, x.floor(-30));

        for (Centroid centroid : x) {
            assertEquals(centroid.mean(), x.mean(x.floor(centroid.mean() + 0.1)), 0);
        }
    }

    @Test
    public void testHeadSum() {
        AVLGroupTree x = new AVLGroupTree(false);
        for (int i = 0; i < 1000; ++i) {
            x.add(randomDouble(), randomIntBetween(1, 10), null);
        }
        long sum = 0;
        long last = -1;
        for (int node = x.first(); node != IntAVLTree.NIL; node = x.next(node)) {
            assertEquals(sum, x.headSum(node));
            sum += x.count(node);
            last = x.count(node);
        }
        assertEquals(last, x.count(x.last()));
    }

    @Test
    public void testFloorSum() {
        AVLGroupTree x = new AVLGroupTree(false);
        int total = 0;
        for (int i = 0; i < 1000; ++i) {
            int count = randomIntBetween(1, 10);
            x.add(randomDouble(), count, null);
            total += count;
        }
        assertEquals(IntAVLTree.NIL, x.floorSum(-1));
        for (long i = 0; i < total + 10; ++i) {
            final int floorNode = x.floorSum(i);
            assertTrue(x.headSum(floorNode) <= i);
            final int next = x.next(floorNode);
            assertTrue(next == IntAVLTree.NIL || x.headSum(next) > i);
        }
    }


    @Test
    public void testInPlaceUpdateKeepsAggregates() {
        // see https://github.com/tdunning/t-digest/issues/230
        AVLGroupTree x = new AVLGroupTree(false);
        x.add(new Centroid(2));
        x.add(new Centroid(1));
        x.add(new Centroid(3));

        // the mean is unchanged, so update() works in place
        x.update(x.first(), 1, 2, null, false);

        assertEquals(4, x.sum());
        assertEquals(3, x.headSum(x.last()));
        x.checkAggregates();
    }

    @Test
    public void testAggregatesSurviveInPlaceUpdatesAndMerges() {
        Random gen = getRandom();
        AVLGroupTree x = new AVLGroupTree(false);
        for (int i = 0; i < 1000; i++) {
            // few distinct values, so most nodes have neighbors with the same mean
            x.add(new Centroid(gen.nextInt(50)));
        }
        checkHeadSums(x);

        for (int i = 0; i < 10000; i++) {
            int node = x.floor(gen.nextInt(50) + 0.5);
            if (node == IntAVLTree.NIL) {
                node = x.first();
            }
            int after = x.next(node);
            if (after != IntAVLTree.NIL && gen.nextInt(10) == 0 && x.size() > 100) {
                // what AVLTreeDigest.compress() does: fold the next node into this one, in place
                int w0 = x.count(node);
                int w1 = x.count(after);
                double mean = (x.mean(node) * w0 + x.mean(after) * w1) / (w0 + w1);
                x.update(node, mean, w0 + w1, null, true);
                x.remove(after);
            } else {
                // what AVLTreeDigest.add() does with a repeated value: same mean, more weight
                x.update(node, x.mean(node), x.count(node) + 1 + gen.nextInt(3), null, false);
            }
            if (i % 250 == 0) {
                checkHeadSums(x);
            }
        }
        checkHeadSums(x);
        x.checkAggregates();
        x.checkBalance();
    }

    private void checkHeadSums(AVLGroupTree x) {
        long expected = 0;
        for (int node = x.first(); node != IntAVLTree.NIL; node = x.next(node)) {
            assertEquals(expected, x.headSum(node));
            expected += x.count(node);
        }
        assertEquals(expected, x.sum());
    }
}
