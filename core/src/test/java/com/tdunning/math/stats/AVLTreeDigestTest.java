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

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Random;

public class AVLTreeDigestTest extends TDigestTest {
    @BeforeClass
    public static void setup() throws IOException {
        TDigestTest.setup("avl-tree");
    }

    protected DigestFactory factory(final double compression) {
        return new DigestFactory() {
            @Override
            public TDigest create() {
                return new AVLTreeDigest(compression);
            }
        };
    }

    @Override
    protected TDigest fromBytes(ByteBuffer bytes) {
        return AVLTreeDigest.fromBytes(bytes);
    }

    @Override
    public void testRepeatedValues() {
        // disabled for AVLTreeDigest for now
    }

    @Override
    public void testSingletonInACrowd() {
        // disabled for AVLTreeDigest for now
    }

    @Override
    public void singleSingleRange() {
        // disabled for AVLTreeDigest for now
    }

    @Test
    public void testRandomNumberGenerator() {
        //The AVLTreeDigest constructor with a specific random Obj
        //should generate predictable random numbers.
        //Testing that the random obj is serialized/deserialized properly is done elsewhere.
        //This test simply confirms that the `randomness` is consistent if random objects with specific seeds are used

        int compression = 100;
        int randomSeed = 42;
        AVLTreeDigest seededTree1 = new AVLTreeDigest(compression, new Random(randomSeed));
        AVLTreeDigest seededTree2 = new AVLTreeDigest(compression, new Random(randomSeed));
        AVLTreeDigest unseededTree = new AVLTreeDigest(compression);

        Random rng = new Random();

        for (int i = 0; i < 100_100; i++) {
            int value = rng.nextInt(100_100);
            seededTree1.add(value);
            seededTree2.add(value);
            unseededTree.add(value);
        }

        //Check that the two seeded trees resulted in the same tree
        //However, we cannot guarantee the unseeded tree is shares no similarity with the seeded ones
        assertEquals(seededTree1.quantile(0.5), seededTree2.quantile(0.5), 0.0);
        Iterator<Centroid> cx = seededTree2.centroids().iterator();
        for (Centroid c1 : seededTree1.centroids()) {
            Centroid c2 = cx.next();
            assertEquals(c1.count(), c2.count());
            assertEquals(c1.mean(), c2.mean(), 1e-10);
        }

        Long t1Val = seededTree1.getRandomNumberGenerator().nextLong();
        Long t2Val = seededTree2.getRandomNumberGenerator().nextLong();
        Long unseededVal = unseededTree.getRandomNumberGenerator().nextLong();
        assertEquals(t1Val, t2Val);
        assertNotSame(t1Val, unseededVal);
    }
}
