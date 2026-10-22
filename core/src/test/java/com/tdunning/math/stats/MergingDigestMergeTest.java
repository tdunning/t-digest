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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class MergingDigestMergeTest {
    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][] {
                {false, false}, {true, false},
                {false, true}, {true, true}
        });
    }

    private final boolean bulk;
    private final boolean record;

    public MergingDigestMergeTest(boolean bulk, boolean record) {
        this.bulk = bulk;
        this.record = record;
    }

    private TDigest source() {
        TDigest source = new MergingDigest(100);
        return record ? source.recordAllData() : source;
    }

    private void merge(TDigest target, TDigest source) {
        if (bulk) {
            target.add(Collections.singletonList(source));
        } else {
            target.add(source);
        }
    }

    @Test
    public void mergeWithPendingValues() {
        TDigest target = new MergingDigest(100);
        TDigest source = source();
        if (record) {
            target.recordAllData();
        }
        for (int i = 0; i < 4000; i++) {
            (i < 1000 ? target : source).add(i / 3999.0);
        }

        merge(target, source);

        assertEquals(4000, target.size());
        assertEquals(0, target.quantile(0), 0);
        assertEquals(1, target.quantile(1), 0);
        assertEquals(0.5, target.quantile(0.5), 0.002);
        assertEquals(3000, source.size());
        assertEquals(1000 / 3999.0, source.quantile(0), 0);
        assertEquals(1, source.quantile(1), 0);
        if (record) {
            int count = 0;
            for (Centroid centroid : target.centroids()) {
                assertEquals(centroid.count(), centroid.data().size());
                count += centroid.data().size();
            }
            assertEquals(4000, count);
            count = 0;
            for (Centroid centroid : source.centroids()) {
                assertEquals(centroid.count(), centroid.data().size());
                count += centroid.data().size();
            }
            assertEquals(3000, count);
        }
        target.add(2);
        assertEquals(4001, target.size());
        assertEquals(2, target.quantile(1), 0);
    }

    @Test
    public void mergeSmallAvlSource() {
        TDigest target = new MergingDigest(100);
        TDigest source = new AVLTreeDigest(100);
        if (record) {
            target.recordAllData();
            source.recordAllData();
        }
        for (int i = 0; i < 20; i++) {
            target.add(i);
            source.add(i + 20);
        }
        merge(target, source);
        assertEquals(40, target.size());
        assertEquals(0, target.quantile(0), 0);
        assertEquals(39, target.quantile(1), 0);
        assertEquals(19.5, target.quantile(0.5), 0.5);
        assertEquals(20, source.size());
        if (record) {
            int count = 0;
            for (Centroid centroid : source.centroids()) {
                assertEquals(centroid.count(), centroid.data().size());
                count += centroid.data().size();
            }
            assertEquals(20, count);
        }
    }

    @Test
    public void mergeUnrecordedSource() {
        TDigest target = new MergingDigest(100);
        TDigest source = new MergingDigest(100);
        if (record) {
            target.recordAllData();
        }
        for (int i = 0; i < 20; i++) {
            target.add(i);
            source.add(i + 20);
        }
        merge(target, source);
        assertEquals(40, target.size());
        assertEquals(0, target.quantile(0), 0);
        assertEquals(39, target.quantile(1), 0);
        if (record) {
            int count = 0;
            for (Centroid centroid : target.centroids()) {
                assertEquals(centroid.count(), centroid.data().size());
                count += centroid.data().size();
            }
            assertEquals(40, count);
        }
        assertFalse(source.isRecording());
    }

    @Test
    public void mergeEmptySource() {
        TDigest target = new MergingDigest(100);
        if (record) {
            target.recordAllData();
        }
        merge(target, source());
        assertEquals(0, target.size());
        assertTrue(Double.isNaN(target.quantile(0.5)));
        target.add(42);
        merge(target, source());
        assertEquals(1, target.size());
        assertEquals(42, target.quantile(0.5), 0);
    }
}
