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

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.mahout.math.jet.random.AbstractContinousDistribution;
import org.apache.mahout.math.jet.random.AbstractDistribution;
import org.apache.mahout.math.jet.random.Gamma;
import org.apache.mahout.math.jet.random.Normal;
import org.apache.mahout.math.jet.random.Uniform;

import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.runner.CaliperMain;

public class TDigestBenchmark extends Benchmark {

    private static final int ARRAY_PAGE_SIZE = 32;

    enum TDigestFactory {
        ARRAY {
            @Override
            TDigest create(double compression) {
                return new ArrayDigest(ARRAY_PAGE_SIZE, compression);
            }
        },
        TREE {
            @Override
            TDigest create(double compression) {
                return new TreeDigest(compression);
            }
        },
        AVL_TREE {
            @Override
            TDigest create(double compression) {
                return new AVLTreeDigest(compression);
            }
        };

        abstract TDigest create(double compression);
    }

    private enum DistributionFactory {
        UNIFORM {
            @Override
            AbstractDistribution create(Random random) {
                return new Uniform(0, 1, random);
            }
        },
        SEQUENTIAL {
            @Override
            AbstractDistribution create(Random random) {
                return new AbstractContinousDistribution() {
                    double base = 0;

                    @Override
                    public double nextDouble() {
                        base += Math.PI * 1e-5;
                        return base;
                    }
                };
            }
        },
        REPEATED {
            @Override
            AbstractDistribution create(final Random random) {
                return new AbstractContinousDistribution() {
                    @Override
                    public double nextDouble() {
                        return random.nextInt(10);
                    }
                };
            }
        },
        GAMMA {
            @Override
            AbstractDistribution create(Random random) {
                return new Gamma(0.1, 0.1, random);
            }
        },
        NORMAL {
            @Override
            AbstractDistribution create(Random random) {
                return new Normal(0.1, 0.1, random);
            }
        };

        abstract AbstractDistribution create(Random random);
    }

    @Param({"10", "100", "1000"})
    double compression;

    @Param
    TDigestFactory tdigestFactory;

    @Param
    DistributionFactory distributionFactory;

    Random random;
    TDigest tdigest;
    AbstractDistribution distribution;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        random = ThreadLocalRandom.current();
        tdigest = tdigestFactory.create(compression);
        distribution = distributionFactory.create(random);
        // first values are cheap to add, so pre-fill the t-digest to have more realistic results
        for (int i = 0; i < 10000; ++i) {
            tdigest.add(distribution.nextDouble());
        }
    }

    public double timeAdd(int reps) {
        for (int i = 0; i < reps; ++i) {
            tdigest.add(distribution.nextDouble());
        }
        return tdigest.quantile(0);
    }

    public double timeAddAndQuantile(int reps) {
        double s = 0;
        for (int i = 0; i < reps; ++i) {
            tdigest.add(distribution.nextDouble());
            s += tdigest.quantile(random.nextDouble());
        }
        return s;
    }

    public static void main(String[] args) {
        CaliperMain.main(TDigestBenchmark.class, args);
    }
}
