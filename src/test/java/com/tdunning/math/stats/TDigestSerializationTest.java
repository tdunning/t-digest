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

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Verifies that the various TDigest implementations can be serialized.
 *
 * Serializability is important, for example, if we want to use t-digests with Spark.
 */
public class TDigestSerializationTest {
    @Test
    public void testMergingDigest() {
        assertSerializesAndDeserializes(new MergingDigest(100));
    }

    @Test
    public void testAVLTreeDigest() {
        assertSerializesAndDeserializes(new AVLTreeDigest(100));
    }

    private void assertSerializesAndDeserializes(TDigest tdigest) {
        assertNotNull(SerializationUtils.deserialize(SerializationUtils.serialize(tdigest)));

        tdigest.add(1);
        assertNotNull(SerializationUtils.deserialize(SerializationUtils.serialize(tdigest)));
    }
}
