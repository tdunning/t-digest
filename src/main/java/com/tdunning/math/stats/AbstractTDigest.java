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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public abstract class AbstractTDigest extends TDigest {
    protected Random gen = new Random();
    protected boolean recordAllData = false;

    public static double interpolate(double x, double x0, double x1) {
        return (x - x0) / (x1 - x0);
    }

    public static void encode(ByteBuffer buf, int n) {
        int k = 0;
        while (n < 0 || n > 0x7f) {
            byte b = (byte) (0x80 | (0x7f & n));
            buf.put(b);
            n = n >>> 7;
            k++;
            if (k >= 6) {
                throw new IllegalStateException("Size is implausibly large");
            }
        }
        buf.put((byte) n);
    }

    public static int decode(ByteBuffer buf) {
        int v = buf.get();
        int z = 0x7f & v;
        int shift = 7;
        while ((v & 0x80) != 0) {
            if (shift > 28) {
                throw new IllegalStateException("Shift too large in decode");
            }
            v = buf.get();
            z += (v & 0x7f) << shift;
            shift += 7;
        }
        return z;
    }

    abstract void add(double x, int w, Centroid base);

    protected static TDigest merge(Iterable<TDigest> subData, Random gen, TDigest r) {
        List<Centroid> centroids = new ArrayList<Centroid>();
        boolean recordAll = false;
        for (TDigest digest : subData) {
            for (Centroid centroid : digest.centroids()) {
                centroids.add(centroid);
            }
            recordAll |= digest.isRecording();
        }
        Collections.shuffle(centroids, gen);
        if (recordAll) {
            r.recordAllData();
        }

        for (Centroid c : centroids) {
            if (r.isRecording()) {
                // TODO should do something better here.
            }
            ((AbstractTDigest) r).add(c.mean(), c.count(), c);
        }
        return r;
    }

    static double quantile(double previousIndex, double index, double nextIndex, double previousMean, double nextMean) {
        final double delta = nextIndex - previousIndex;
        final double previousWeight = (nextIndex - index) / delta;
        final double nextWeight = (index - previousIndex) / delta;
        return previousMean * previousWeight + nextMean * nextWeight;
    }

    /**
     * Sets up so that all centroids will record all data assigned to them.  For testing only, really.
     */
    @Override
    public TDigest recordAllData() {
        recordAllData = true;
        return this;
    }

    @Override
    public boolean isRecording() {
        return recordAllData;
    }

    /**
     * Adds a sample to a histogram.
     *
     * @param x The value to add.
     */
    @Override
    public void add(double x) {
        add(x, 1);
    }

    @Override
    public void add(TDigest other) {
        List<Centroid> tmp = new ArrayList<Centroid>();
        for (Centroid centroid : other.centroids()) {
            tmp.add(centroid);
        }

        Collections.shuffle(tmp, gen);
        for (Centroid centroid : tmp) {
            add(centroid.mean(), centroid.count(), centroid);
        }
    }

    protected Centroid createCentroid(double mean, int id) {
        return new Centroid(mean, id, recordAllData);
    }

    @Override
    public double quantile(double q) {
        if (q < 0 || q > 1) {
            throw new IllegalArgumentException("q should be in [0,1], got " + q);
        }

        final int centroidCount = centroids().size();
        if (centroidCount == 0) {
            return Double.NaN;
        } else if (centroidCount == 1) {
            return centroids().iterator().next().mean();
        }

        // if values were stored in a sorted array, index would be the offset we are interested in
        final double index = q * (size() - 1);

        double previousMean = Double.NaN, previousIndex = 0;
        long total = 0;
        // Jump over pages until we reach the page containing the quantile we are interested in
        Iterator<? extends Centroid> it = centroids().iterator();
        Centroid next;
        while (true) {
            next = it.next();
            final double nextIndex = total + (next.count() - 1.0) / 2;
            if (nextIndex >= index) {
                if (Double.isNaN(previousMean)) {
                    assert total == 0;
                    // special case 1: the index we are interested in is before the 1st centroid
                    if (nextIndex == previousIndex) {
                        return next.mean();
                    }
                    // assume values grow linearly between index previousIndex=0 and nextIndex2
                    Centroid next2 = it.next();
                    final double nextIndex2 = total + next.count() + (next2.count() - 1.0) / 2;
                    previousMean = (nextIndex2 * next.mean() - nextIndex * next2.mean()) / (nextIndex2 - nextIndex);
                }
                // common case: we found two centroids previous and next so that the desired quantile is
                // after 'previous' but before 'next'
                return quantile(previousIndex, index, nextIndex, previousMean, next.mean());
            } else if (!it.hasNext()) {
                // special case 2: the index we are interested in is beyond the last centroid
                // again, assume values grow linearly between index previousIndex and (count - 1)
                // which is the highest possible index
                final double nextIndex2 = size() - 1;
                final double nextMean2 = (next.mean() * (nextIndex2 - previousIndex) - previousMean * (nextIndex2 - nextIndex)) / (nextIndex - previousIndex);
                return quantile(nextIndex, index, nextIndex2, next.mean(), nextMean2);
            }
            total += next.count();
            previousMean = next.mean();
            previousIndex = nextIndex;
        }
    }

    @Override
    public double cdf(double x) {
        if (size() == 0) {
            return Double.NaN;
        } else if (size() == 1) {
            return x < centroids().iterator().next().mean() ? 0 : 1;
        } else {
            double r = 0;

            // we scan a across the centroids
            Iterator<? extends Centroid> it = centroids().iterator();
            Centroid a = it.next();

            // b is the look-ahead to the next centroid
            Centroid b = it.next();

            // initially, we set left width equal to right width
            double left = (b.mean() - a.mean()) / 2;
            double right = left;

            // scan to next to last element
            while (it.hasNext()) {
                if (x < a.mean() + right) {
                    return (r + a.count() * AbstractTDigest.interpolate(x, a.mean() - left, a.mean() + right)) / size();
                }
                r += a.count();

                a = b;
                b = it.next();

                left = right;
                right = (b.mean() - a.mean()) / 2;
            }

            // for the last element, assume right width is same as left
            left = right;
            a = b;
            if (x < a.mean() + right) {
                return (r + a.count() * AbstractTDigest.interpolate(x, a.mean() - left, a.mean() + right)) / size();
            } else {
                return 1;
            }
        }
    }
}
