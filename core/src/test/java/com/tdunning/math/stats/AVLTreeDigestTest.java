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

    @Test
    public void testRepeatedValuesDoNotBridgeAnEmptyRange() {
        // see https://github.com/tdunning/t-digest/issues/230
        // ranks 94..100 all hold 1000, so there is nothing to interpolate
        for (int trial = 0; trial < 20; trial++) {
            TDigest digest = factory(100).create();
            for (int i = 0; i < 93; i++) {
                digest.add(1);
            }
            for (int i = 0; i < 7; i++) {
                digest.add(1000);
            }
            assertEquals(1, digest.quantile(0.9), 0);
            assertEquals(1000, digest.quantile(0.95), 0);
            assertEquals(1000, digest.quantile(0.99), 0);
        }
    }
}
