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

package com.tdunning.tdigest.quality;

import com.clearspring.analytics.stream.quantile.QDigest;
import com.tdunning.math.stats.Dist;
import com.tdunning.math.stats.MergingDigest;
import com.tdunning.math.stats.QuantileEstimator;
import com.tdunning.math.stats.TDigest;
import org.apache.mahout.math.jet.random.AbstractContinousDistribution;
import org.apache.mahout.math.jet.random.Gamma;
import org.apache.mahout.math.jet.random.Uniform;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Compares t-digest to q-digest and traditional streaming quantile algorithms.
 */
public class ComparisonTest {
    private static double M = 20;

    @Test
    public void compareToQDigest() throws FileNotFoundException {
        Random rand = new Random();
        try (PrintWriter out = new PrintWriter(new FileOutputStream("qd-tree-comparison.csv"))) {
            out.printf("tag,compression,q,e1,e2,t.size,q.size\n");

            for (int i = 0; i < M; i++) {
                compareQD(out, new Gamma(0.1, 0.1, rand), "gamma", 1L << 48);
                compareQD(out, new Uniform(0, 1, rand), "uniform", 1L << 48);
            }
        }
    }

    private void compareQD(PrintWriter out, AbstractContinousDistribution gen, String tag, long scale) {
        for (double compression : new double[]{10, 20, 50, 100, 200, 500, 1000, 2000}) {
            QDigest qd = new QDigest(compression);
            TDigest dist = new MergingDigest(compression);
            double[] data = new double[100000];
            for (int i = 0; i < 100000; i++) {
                double x = gen.nextDouble();
                dist.add(x);
                qd.offer((long) (x * scale));
                data[i] = x;
            }
            dist.compress();
            Arrays.sort(data);

            for (double q : new double[]{1e-5, 1e-4, 0.001, 0.01, 0.1, 0.5, 0.9, 0.99, 0.999, 0.9999, 0.99999}) {
                double x1 = dist.quantile(q);
                double x2 = (double) qd.getQuantile(q) / scale;
                double e1 = Dist.cdf(x1, data) - q;
                out.printf("%s,%.0f,%.8f,%.10g,%.10g,%d,%d\n", tag, compression, q, e1, Dist.cdf(x2, data) - q, dist.smallByteSize(), QDigest.serialize(qd).length);
            }
        }
    }

    @Test
    public void compareToStreamingQuantile() throws FileNotFoundException {
        Random rand = new Random();

        try (PrintWriter out = new PrintWriter(new FileOutputStream("sq-tree-comparison.csv"))) {
            out.printf("tag,compression,q,e1,e2,t.size,q.size\n");
            for (int i = 0; i < M; i++) {
                compareSQ(out, new Gamma(0.1, 0.1, rand), "gamma");
                compareSQ(out, new Uniform(0, 1, rand), "uniform");
            }
        }
    }

    private void compareSQ(PrintWriter out, AbstractContinousDistribution gen, String tag) {
        double[] quantiles = {0.001, 0.01, 0.1, 0.2, 0.3, 0.5, 0.7, 0.8, 0.9, 0.99, 0.999};
        for (double compression : new double[]{10, 20, 50, 100, 200, 500, 1000, 2000}) {
            QuantileEstimator sq = new QuantileEstimator(1001);
            TDigest dist = new MergingDigest(compression);
            double[] data = new double[100000];
            for (int i = 0; i < 100000; i++) {
                double x = gen.nextDouble();
                dist.add(x);
                sq.add(x);
                data[i] = x;
            }
            dist.compress();
            Arrays.sort(data);

            List<Double> qz = sq.getQuantiles();
            for (double q : quantiles) {
                double x1 = dist.quantile(q);
                double x2 = qz.get((int) (q * 1000 + 0.5));
                double e1 = Dist.cdf(x1, data) - q;
                double e2 = Dist.cdf(x2, data) - q;
                out.printf("%s,%.0f,%.8f,%.10g,%.10g,%d,%d\n",
                        tag, compression, q, e1, e2, dist.smallByteSize(), sq.serializedSize());

            }
        }
    }

}
