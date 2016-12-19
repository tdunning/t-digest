/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
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

/**
 * Abstract class that describes how a Histogram should work.
 */
public abstract class Histogram {
    public Histogram(double min, double max, double binsPerDecade) {
        if (max <= 2 * min) {
            throw new IllegalArgumentException(String.format("Illegal/nonsensical min,max (%.2f, %.2g)", min, max));
        }
        if (min <= 0 || max <= 0) {
            throw new IllegalArgumentException("Min and max must be positive");
        }
        if (binsPerDecade < 5 || binsPerDecade > 10000) {
            throw new IllegalArgumentException(
                    String.format("Unreasonable number of bins per decade %.2g. Expected value in range [5,10000]",
                            binsPerDecade));
        }
    }

    abstract void add(double v);

    abstract double[] getBounds();

    abstract long[] getCounts();

    abstract long[] getCompressedCounts();
}
