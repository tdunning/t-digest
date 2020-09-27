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

import com.carrotsearch.randomizedtesting.annotations.Seed;
import com.google.common.collect.Lists;
import org.apache.mahout.common.RandomUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//to freeze the tests with a particular seed, put the seed on the next line
//@Seed("84527677CF03B566:A6FF596BDDB2D59D")
@Seed("1CD6F48E8CA53BD1:379C5BDEB3A02ACB")
public class MergingDigestTest extends TDigestTest {
    @BeforeClass
    public static void setup() throws IOException {
        TDigestTest.setup("merge");
    }

    protected DigestFactory factory(final double compression) {
        return new DigestFactory() {
            @Override
            public TDigest create() {
                return new MergingDigest(compression);
            }
        };
    }

    @Before
    public void testSetUp() {
        RandomUtils.useTestSeed();
    }

    @Override
    protected TDigest fromBytes(ByteBuffer bytes) {
        return MergingDigest.fromBytes(bytes);
    }

    // This test came from PR#145 by github user pulver
    @Test
    public void testNanDueToBadInitialization() {
        int compression = 30;
        int factor = 5;
        MergingDigest md = new MergingDigest(compression, (factor + 1) * compression, compression);

        final int M = 10;
        List<MergingDigest> mds = new ArrayList<>();
        for (int i = 0; i < M; ++i) {
            mds.add(new MergingDigest(compression, (factor + 1) * compression, compression));
        }

        // Fill all digests with values (0,10,20,...,80).
        List<Double> raw = Lists.newArrayList();
        for (int i = 0; i < 9; ++i) {
            double x = 10 * i;
            md.add(x);
            raw.add(x);
            for (int j = 0; j < M; ++j) {
                mds.get(j).add(x);
                raw.add(x);
            }
        }
        Collections.sort(raw);

        // Merge all mds one at a time into md.
        for (int i = 0; i < M; ++i) {
            List<MergingDigest> singleton = new ArrayList<>();
            singleton.add(mds.get(i));
            md.add(singleton);
        }
//        md.add(mds);

//        Assert.assertFalse(Double.isNaN(md.quantile(0.01)));
        // Output
        System.out.printf("%4s\t%10s\t%10s\t%10s\t%10s\n", "q", "estimated", "actual", "error_cdf", "error_q");
        String dashes = "==========";

        System.out.printf("%4s\t%10s\t%10s\t%10s\t%10s\n", dashes.substring(0, 4), dashes, dashes, dashes, dashes);
        for (double q : new double[]{0.01, 0.05, 0.1, 0.25, 0.5, 0.75, 0.90, 0.95, 0.99}) {
            double est = md.quantile(q);
            double actual = Dist.quantile(q, raw);
            double qx = md.cdf(actual);
            Assert.assertEquals(q, qx, 0.08);
            Assert.assertEquals(est, actual, 3.5);
            System.out.printf("%4.2f\t%10.2f\t%10.2f\t%10.2f\t%10.2f\n", q, est, actual, Math.abs(est - actual), Math.abs(qx - q));
        }
    }

}