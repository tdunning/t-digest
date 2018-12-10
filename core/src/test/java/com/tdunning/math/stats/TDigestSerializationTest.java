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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.xml.bind.DatatypeConverter;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Verifies that the various TDigest implementations can be serialized.
 *
 * Serializability is important, for example, if we want to use t-digests with Spark.
 */
public class TDigestSerializationTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testMergingDigest() {
        assertSerializesAndDeserializes(new MergingDigest(100));
    }

    @Test
    public void testAVLTreeDigest() {
        assertSerializesAndDeserializes(new AVLTreeDigest(100));
    }

    private <T extends TDigest> void assertSerializesAndDeserializes(T tdigest) {
        assertNotNull(SerializationUtils.deserialize(SerializationUtils.serialize(tdigest)));

        final Random gen = new Random();
        for (int i = 0; i < 100000; i++) {
            tdigest.add(gen.nextDouble());
        }
        T roundTrip = SerializationUtils.deserialize(SerializationUtils.serialize(tdigest));

        assertTDigestEquals(tdigest, roundTrip);
    }

    private void assertTDigestEquals(TDigest t1, TDigest t2) {
        assertEquals(t1.getMin(), t2.getMin(), 0);
        assertEquals(t1.getMax(), t2.getMax(), 0);
        Iterator<Centroid> cx = t2.centroids().iterator();
        for (Centroid c1 : t1.centroids()) {
            Centroid c2 = cx.next();
            assertEquals(c1.count(), c2.count());
            assertEquals(c1.mean(), c2.mean(), 1e-10);
        }
        assertFalse(cx.hasNext());
        assertNotNull(t2);
    }

    @Test
    public void testCentroidCountSanityCheck() {
        final byte[] brokenBytes = DatatypeConverter.parseBase64Binary("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AAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAgAAAAEAAAABAAAAAgAAAAEAAAACAAAAAgAAAAEAAAACAAAAAgAAAAEAAAABAAAAAgAAAAIAAAABAAAAAQAAAAMAAAABAAAAAQAAAAIAAAABAAAAAgAAAAIAAAACAAAAAwAAAAEAAAADAAAAAwAAAAMAAAACAAAAAwAAAAMAAAACAAAAAwAAAAMAAAABAAAAAgAAAAIAAAADAAAAAgAAAAQAAAAEAAAABAAAAAIAAAAEAAAAAgAAAAQAAAAEAAAAAgAAAAMAAAAEAAAABQAAAAUAAAAFAAAABQAAAAIAAAAGAAAAAwAAAAUAAAAEAAAABQAAAAcAAAAFAAAABwAAAAYAAAADAAAAAwAAAAcAAAAFAAAACAAAAAcAAAAHAAAABwAAAAgAAAADAAAABQAAAAcAAAAGAAAACAAAAAcAAAAGAAAACAAAAA0AAAANAAAACAAAAAgAAAALAAAACAAAAAgAAAAHAAAABgAAAAgAAAAEAAAABwAAAAQAAAALAAAACgAAAAYAAAAIAAAACwAAAAoAAAAHAAAACwAAAAsAAAAGAAAACAAAAAkAAAALAAAACQAAAA8AAAANAAAACQAAAAwAAAARAAAADQAAABEAAAATAAAACgAAAAwAAAAIAAAACQAAAAwAAAANAAAAFQAAABEAAAAIAAAADwAAAA4AAAAPAAAACwAAAA4AAAAKAAAAEAAAAA8AAAAKAAAABAAAAAYAAAAQAAAADwAAAA4AAAARAAAACgAAAB0AAAASAAAADwAAABoAAAAQAAAAGQAAAAkAAAAJAAAADAAAAA4AAAAYAAAAGQAAABgAAAAeAAAAGwAAABQAAAAbAAAAIAAAACUAAAAcAAAAGAAAABkAAAAeAAAAIQAAACgAAAAjAAAAHQAAAC8AAAA6AAAAKAAAACAAAAAeAAAAIgAAAC8AAAAvAAAAJQAAACYAAAAqAAAAIQAAAB4AAAAYAAAAEwAAABkAAAAmAAAAMQAAABsAAAAgAAAAJwAAAD8AAAAyAAAAGwAAADAAAAAlAAAAKQAAACYAAAAsAAAANAAAACgAAAAe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");
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Centroid count is invalid: 20000000. Limit is: 653");
        AVLTreeDigest.fromBytes(ByteBuffer.wrap(brokenBytes));
    }
}
