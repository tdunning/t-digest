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

import com.google.common.collect.Lists;
import org.apache.mahout.common.RandomUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class GroupTreeTest extends AbstractTest{
    @Test
    public void testSimpleAdds() {
        GroupTree x = new GroupTree();
        assertNull(x.floor(new Centroid(34)));
        assertNull(x.ceiling(new Centroid(34)));
        assertEquals(0, x.size());
        assertEquals(0, x.sum());

        x.add(new Centroid(1));
        Centroid centroid = new Centroid(2);
        centroid.add(3, 1);
        centroid.add(4, 1);
        x.add(centroid);

        assertEquals(2, x.size());
        assertEquals(4, x.sum());
    }

    @Test
    public void testBalancing() {
        GroupTree x = new GroupTree();
        for (int i = 0; i < 101; i++) {
            x.add(new Centroid(i));
        }

        assertEquals(101, x.sum());
        assertEquals(101, x.size());

        x.checkBalance();
    }

    @Test
    public void testIterators() {
        GroupTree x = new GroupTree();
        for (int i = 0; i < 101; i++) {
            x.add(new Centroid(i / 2));
        }

        assertEquals(0, x.first().mean(), 0);
        assertEquals(50, x.last().mean(), 0);

        Iterator<Centroid> ix = x.iterator();
        for (int i = 0; i < 101; i++) {
            assertTrue(ix.hasNext());
            Centroid z = ix.next();
            assertEquals(i / 2, z.mean(), 0);
        }
        assertFalse(ix.hasNext());

        // 34 is special since it is the smallest element of the right hand sub-tree
        Iterable<Centroid> z = x.tailSet(new Centroid(34, 1, 0));
        ix = z.iterator();
        for (int i = 68; i < 101; i++) {
            assertTrue(ix.hasNext());
            Centroid v = ix.next();
            assertEquals(i / 2, v.mean(), 0);
        }
        assertFalse(ix.hasNext());

        ix = z.iterator();
        for (int i = 68; i < 101; i++) {
            Centroid v = ix.next();
            assertEquals(i / 2, v.mean(), 0);
        }

        z = x.tailSet(new Centroid(33, 1, 0));
        ix = z.iterator();
        for (int i = 66; i < 101; i++) {
            assertTrue(ix.hasNext());
            Centroid v = ix.next();
            assertEquals(i / 2, v.mean(), 0);
        }
        assertFalse(ix.hasNext());

        z = x.tailSet(x.ceiling(new Centroid(34, 1, 0)));
        ix = z.iterator();
        for (int i = 68; i < 101; i++) {
            assertTrue(ix.hasNext());
            Centroid v = ix.next();
            assertEquals(i / 2, v.mean(), 0);
        }
        assertFalse(ix.hasNext());

        z = x.tailSet(x.floor(new Centroid(34, 1, 0)));
        ix = z.iterator();
        for (int i = 67; i < 101; i++) {
            assertTrue(ix.hasNext());
            Centroid v = ix.next();
            assertEquals(i / 2, v.mean(), 0);
        }
        assertFalse(ix.hasNext());
    }

    @Test
    public void testFloor() {
        // mostly tested in other tests
        GroupTree x = new GroupTree();
        for (int i = 0; i < 101; i++) {
            x.add(new Centroid(i / 2));
        }

        assertNull(x.floor(new Centroid(-30)));
    }


    @Test
    public void testRemoveAndSums() {
        GroupTree x = new GroupTree();
        for (int i = 0; i < 101; i++) {
            x.add(new Centroid(i / 2));
        }
        Centroid g = x.ceiling(new Centroid(2, 1, 0));
        x.remove(g);
        g.add(3, 1);
        x.add(g);

        assertEquals(0, x.headCount(new Centroid(-1)));
        assertEquals(0, x.headSum(new Centroid(-1)));
        assertEquals(0, x.headCount(new Centroid(0, 1, 0)));
        assertEquals(0, x.headSum(new Centroid(0, 1, 0)));
        assertEquals(0, x.headCount(x.ceiling(new Centroid(0, 1, 0))));
        assertEquals(0, x.headSum(x.ceiling(new Centroid(0, 1, 0))));
        assertEquals(2, x.headCount(new Centroid(1, 1, 0)));
        assertEquals(2, x.headSum(new Centroid(1, 1, 0)));

        g = x.tailSet(new Centroid(2.1)).iterator().next();
        assertEquals(2.5, g.mean(), 1e-9);

        int i = 0;
        for (Centroid gx : x) {
            if (i > 10) {
                break;
            }
            System.out.printf("%d:%.1f(%d)\t", i++, gx.mean(), gx.count());
        }
        assertEquals(5, x.headCount(new Centroid(2.1, 1, 0)));
        assertEquals(5, x.headSum(new Centroid(2.1, 1, 0)));

        assertEquals(6, x.headCount(new Centroid(2.7, 1, 0)));
        assertEquals(7, x.headSum(new Centroid(2.7, 1, 0)));

        assertEquals(101, x.headCount(new Centroid(200)));
        assertEquals(102, x.headSum(new Centroid(200)));
    }

    @Before
    public void setUp() {
        RandomUtils.useTestSeed();
    }

    @Test
    public void testRandomRebalance() {
        GroupTree x = new GroupTree();
        List<Double> y = Lists.newArrayList();
        for (int i = 0; i < 1000; i++) {
            double v = randomDouble();
            x.add(new Centroid(v));
            y.add(v);
            x.checkBalance();
        }

        Collections.sort(y);

        Iterator<Double> i = y.iterator();
        for (Centroid centroid : x) {
            assertEquals(i.next(), centroid.mean(), 0.0);
        }

        for (int j = 0; j < 100; j++) {
            double v = y.get(randomInt(y.size() - 1));
            y.remove(v);
            x.remove(x.floor(new Centroid(v)));
        }

        Collections.sort(y);
        i = y.iterator();
        for (Centroid centroid : x) {
            assertEquals(i.next(), centroid.mean(), 0.0);
        }

        for (int j = 0; j < y.size(); j++) {
            double v = y.get(j);
            y.set(j, v + 10);
            Centroid g = x.floor(new Centroid(v));
            x.remove(g);
            x.checkBalance();
            g.add(g.mean() + 20, 1);
            x.add(g);
            x.checkBalance();
        }

        i = y.iterator();
        for (Centroid centroid : x) {
            assertEquals(i.next(), centroid.mean(), 0.0);
        }
    }
}
