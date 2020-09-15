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
import org.apache.commons.math3.util.Pair;
import org.apache.mahout.common.RandomUtils;
import org.apache.mahout.math.jet.random.AbstractContinousDistribution;
import org.apache.mahout.math.jet.random.Uniform;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

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



    @Test
    public void writeAsymmetricScaleFunctionResults() {

        List<ScaleFunction> scaleFcns = Arrays.asList(ScaleFunction.K_0, ScaleFunction.K_1,
                ScaleFunction.K_2, ScaleFunction.K_3, ScaleFunction.K_1_GLUED,
                ScaleFunction.K_2_GLUED, ScaleFunction.K_3_GLUED, ScaleFunction.K_QUADRATIC);

        int numTrials = 100;

        Map<String, Pair<ScaleFunction, Boolean>> digestParams = new HashMap<>();

        for (ScaleFunction fcn : scaleFcns) {
            if (fcn.toString().endsWith("GLUED") || fcn.toString().endsWith("QUADRATIC")) {
                digestParams.put(fcn.toString(), new Pair<>(fcn, false));
            } else {
                digestParams.put(fcn.toString() + "_USUAL", new Pair<>(fcn, false));
            }
        }
        writeSeveralDigestUniformResults(digestParams, numTrials, "../docs/asymmetric/data/merging/");

    }

    public void writeSeveralDigestUniformResults(Map<String, Pair<ScaleFunction, Boolean>> digestParams, int numTrials,
                                                 String writeLocation) {

        int trialSize = 1_000_000;
        double compression = 100;
        double[] quants = new double[]{0.00001, 0.0001, 0.001, 0.01, 0.1,
                0.5, 0.9, 0.99, 0.999, 0.9999, 0.99999};

        Map<String, List<Integer>> centroidCounts= new HashMap<>();

        for (Map.Entry<String, Pair<ScaleFunction, Boolean>> entry : digestParams.entrySet()) {
            centroidCounts.put(entry.getKey(), new ArrayList<Integer>());
            try {
                Map<Double, List<String>> records = new HashMap<>();
                for (double q : quants) {
                    records.put(q, new ArrayList<String>());
                }
                for (int j = 0; j < numTrials; j++) {
                        MergingDigest digest = (MergingDigest) factory(compression).create();
                        digest.setScaleFunction(entry.getValue().getFirst());
                        digest.setUseAlternatingSort(entry.getValue().getSecond());
                        Random rand = new Random();
                        AbstractContinousDistribution gen = new Uniform(50, 51, rand);
                        double[] data = new double[trialSize];
                        for (int i = 0; i < trialSize; i++) {
                            data[i] = gen.nextDouble();
                            digest.add(data[i]);
                        }
                        Arrays.sort(data);
                        digest.compress();
                        for (double q : quants) {
                            double x1 = Dist.quantile(q, data);
                            double q1 = Dist.cdf(x1, data);
                            double q2 = digest.cdf(x1);
                            records.get(q).add(String.valueOf(Math.abs(q1 - q2)) + "," +
                                    String.valueOf(Math.abs(q1 - q2) / Math.min(q, 1 - q)) + "\n");
                        }
                        centroidCounts.get(entry.getKey()).add(digest.centroids().size());
                }
                for (double q : quants) {
                    FileWriter csvWriter = new FileWriter(writeLocation + entry.getKey() + "_" + String.valueOf(q) + ".csv");
                    csvWriter.append("error_q,norm_error_q\n");
                    for (String obs : records.get(q)) {
                        csvWriter.append(obs);
                    }
                    csvWriter.flush();
                    csvWriter.close();
                }

                FileWriter csvWriter = new FileWriter(writeLocation + entry.getKey()  + "_centroid_counts.csv");
                csvWriter.append("centroid_count\n");
                for (Integer ct : centroidCounts.get(entry.getKey())) {
                    csvWriter.append(ct.toString()).append("\n");
                }
                csvWriter.flush();
                csvWriter.close();

            } catch (IOException e) {
                System.out.println(e.toString());
                return;
            }
        }
    }
}
