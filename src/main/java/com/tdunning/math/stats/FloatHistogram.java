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

import java.nio.LongBuffer;

/**
 * Maintains histogram buckets that are constant width
 * in base-2 floating point representation space. This is close
 * to exponential binning, but should be faster.
 */
public class FloatHistogram extends Histogram {
    private final long[] counts;
    private final double min;
    private final double max;
    private final int bitsOfPrecision;
    private final int shift;
    private int offset;

    public FloatHistogram(double min, double max) {
        this(min, max, 50);
    }

    public FloatHistogram(double min, double max, double binsPerDecade) {
        super(min, max, binsPerDecade);
        this.min = min;
        this.max = max;

        // convert binsPerDecade into bins per octave, then figure out how many bits that takes
        bitsOfPrecision = (int) Math.ceil(Math.log(binsPerDecade * Math.log10(2)) / Math.log(2));
        // we keep just the required amount of the mantissa
        shift = 52 - bitsOfPrecision;
        // The exponent in a floating point number is offset
        offset = 0x3ff << bitsOfPrecision;

        int binCount = bucketIndex(max) + 1;
        if (binCount > 10000) {
            throw new IllegalArgumentException(
                    String.format("Excessive number of bins %d resulting from min,max,binsPerDecade = %.2g, %.2g, %.2g",
                            binCount, min, max, binsPerDecade));

        }
        counts = new long[binCount];
    }

    // exposed for testing
    int bucket(double x) {
        if (x <= min) {
            return 0;
        } else if (x >= max) {
            return counts.length - 1;
        } else {
            return bucketIndex(x);
        }
    }

    private int bucketIndex(double x) {
        x = x / min;
        long floatBits = Double.doubleToLongBits(x);
        return (int) (floatBits >>> shift) - offset;
    }

    private double lowerBound(int k) {
        return min * Double.longBitsToDouble((k + (0x3ffL << bitsOfPrecision)) << (52 - bitsOfPrecision)) /* / fuzz */;
    }

    @Override
    public void add(double v) {
        counts[bucket(v)]++;
    }

    @Override
    public double[] getBounds() {
        double[] r = new double[counts.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = lowerBound(i);
        }
        return r;
    }

    @Override
    public long[] getCounts() {
        return counts;
    }

    @Override
    public long[] getCompressedCounts() {
        LongBuffer buf = LongBuffer.allocate(counts.length);
        Simple64.compress(buf, counts, 0, counts.length);
        long[] r = new long[buf.position()];
        buf.flip();
        buf.get(r);
        return r;
    }
}
