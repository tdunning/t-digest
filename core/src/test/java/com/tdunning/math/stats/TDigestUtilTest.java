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

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;

import com.tdunning.math.stats.serde.AVLTreeDigestCompactSerde;
import org.junit.Test;

import com.google.common.collect.Lists;

public class TDigestUtilTest extends AbstractTest {

    @Test
    public void testIntEncoding() {
        Random gen = getRandom();
        ByteBuffer buf = ByteBuffer.allocate(10000);
        List<Integer> ref = Lists.newArrayList();
        for (int i = 0; i < 3000; i++) {
            int n = gen.nextInt();
            n = n >>> (i / 100);
            ref.add(n);
            AVLTreeDigestCompactSerde.encodeInt(buf, n);
        }

        buf.flip();

        for (int i = 0; i < 3000; i++) {
            int n = AVLTreeDigestCompactSerde.decodeInt(buf);
            assertEquals(String.format("%d:", i), ref.get(i).intValue(), n);
        }
    }
}