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
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class AVLTreeDigest extends AbstractTDigest {

    private double compression;
    private AVLGroupTree summary;
    long count = 0; // package private for testing

    /**
     * A histogram structure that will record a sketch of a distribution.
     *
     * @param compression How should accuracy be traded for size?  A value of N here will give quantile errors
     *                    almost always less than 3/N with considerably smaller errors expected for extreme
     *                    quantiles.  Conversely, you should expect to track about 5 N centroids for this
     *                    accuracy.
     */
    public AVLTreeDigest(double compression) {
        this.compression = compression;
        summary = new AVLGroupTree(false);
    }

    @Override
    public TDigest recordAllData() {
        if (summary.size() != 0) {
            throw new IllegalStateException("Can only ask to record added data on an empty summary");
        }
        summary = new AVLGroupTree(true);
        return super.recordAllData();
    }

    @Override
    void add(double x, int w, Centroid base) {
        if (x != base.mean() || w != base.count()) {
            throw new IllegalArgumentException();
        }
        add(x, w, base.data());
    }

    @Override
    public void add(double x, int w) {
        add(x, w, (List<Double>) null);
    }

    public void add(double x, int w, List<Double> data) {
        int start = summary.floor(x);
        if (start == IntAVLTree.NIL) {
            start = summary.first();
        }

        if (start == IntAVLTree.NIL) { // empty summary
            assert summary.size() == 0;
            summary.add(x, w, data);
            count = w;
        } else {
            double minDistance = Double.MAX_VALUE;
            int lastNeighbor = IntAVLTree.NIL;
            for (int neighbor = start; neighbor != IntAVLTree.NIL; neighbor = summary.next(neighbor)) {
                double z = Math.abs(summary.mean(neighbor) - x);
                if (z < minDistance) {
                    start = neighbor;
                    minDistance = z;
                } else if (z > minDistance) {
                    // as soon as z increases, we have passed the nearest neighbor and can quit
                    lastNeighbor = neighbor;
                    break;
                }
            }

            int closest = IntAVLTree.NIL;
            long sum = summary.headSum(start);
            double n = 0;
            for (int neighbor = start; neighbor != lastNeighbor; neighbor = summary.next(neighbor)) {
                assert minDistance == Math.abs(summary.mean(neighbor) - x);
                double q = count == 1 ? 0.5 : (sum + (summary.count(neighbor) - 1) / 2.0) / (count - 1);
                double k = 4 * count * q * (1 - q) / compression;

                // this slightly clever selection method improves accuracy with lots of repeated points
                if (summary.count(neighbor) + w <= k) {
                    n++;
                    if (gen.nextDouble() < 1 / n) {
                        closest = neighbor;
                    }
                }
                sum += summary.count(neighbor);
            }

            if (closest == IntAVLTree.NIL) {
                summary.add(x, w, data);
            } else {
                // if the nearest point was not unique, then we may not be modifying the first copy
                // which means that ordering can change
                double centroid = summary.mean(closest);
                int count = summary.count(closest);
                List<Double> d = summary.data(closest);
                if (d != null) {
                    if (w == 1) {
                        d.add(x);
                    } else {
                        d.addAll(data);
                    }
                }
                count += w;
                centroid += w * (x - centroid) / count;
                summary.update(closest, centroid, count, d);
            }
            count += w;

            if (summary.size() > 20 * compression) {
                // may happen in case of sequential points
                compress();
            }
        }
    }

    @Override
    public void compress() {
        if (summary.size() <= 1) {
            return;
        }

        AVLGroupTree centroids = summary;
        this.summary = new AVLGroupTree(recordAllData);

        final int[] nodes = new int[centroids.size()];
        nodes[0] = centroids.first();
        for (int i = 1; i < nodes.length; ++i) {
            nodes[i] = centroids.next(nodes[i-1]);
            assert nodes[i] != IntAVLTree.NIL;
        }
        assert centroids.next(nodes[nodes.length - 1]) == IntAVLTree.NIL;

        for (int i = centroids.size() - 1; i > 0; --i) {
            final int other = gen.nextInt(i + 1);
            final int tmp = nodes[other];
            nodes[other] = nodes[i];
            nodes[i] = tmp;
        }

        for (int node : nodes) {
            add(centroids.mean(node), centroids.count(node), centroids.data(node));
        }
    }

    @Override
    public void compress(GroupTree other) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the number of samples represented in this histogram.  If you want to know how many
     * centroids are being used, try centroids().size().
     *
     * @return the number of samples that have been added.
     */
    @Override
    public long size() {
        return count;
    }

    /**
     * @param x the value at which the CDF should be evaluated
     * @return the approximate fraction of all samples that were less than or equal to x.
     */
    @Override
    public double cdf(double x) {
        AVLGroupTree values = summary;
        if (values.size() == 0) {
            return Double.NaN;
        } else if (values.size() == 1) {
            return x < values.mean(values.first()) ? 0 : 1;
        } else {
            double r = 0;

            // we scan a across the centroids
            Iterator<Centroid> it = values.iterator();
            Centroid a = it.next();

            // b is the look-ahead to the next centroid
            Centroid b = it.next();

            // initially, we set left width equal to right width
            double left = (b.mean() - a.mean()) / 2;
            double right = left;

            // scan to next to last element
            while (it.hasNext()) {
                if (x < a.mean() + right) {
                    return (r + a.count() * interpolate(x, a.mean() - left, a.mean() + right)) / count;
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
                return (r + a.count() * interpolate(x, a.mean() - left, a.mean() + right)) / count;
            } else {
                return 1;
            }
        }
    }

    /**
     * @param q The quantile desired.  Can be in the range [0,1].
     * @return The minimum value x such that we think that the proportion of samples is <= x is q.
     */
    @Override
    public double quantile(double q) {
        if (q < 0 || q > 1) {
            throw new IllegalArgumentException("q should be in [0,1], got " + q);
        }

        AVLGroupTree values = summary;
        if (values.size() == 0) {
            return Double.NaN;
        } else if (values.size() == 1) {
            return values.iterator().next().mean();
        }

        // if values were stored in a sorted array, index would be the offset we are interested in
        final double index = q * (count - 1);

        double previousMean = Double.NaN, previousIndex = 0;
        int next = values.floorSum((long) index);
        assert next != IntAVLTree.NIL;
        long total = values.headSum(next);
        final int prev = values.prev(next);
        if (prev != IntAVLTree.NIL) {
            previousMean = values.mean(prev);
            previousIndex = total - (values.count(prev) + 1.0) / 2;
        }

        while (true) {
            final double nextIndex = total + (values.count(next) - 1.0) / 2;
            if (nextIndex >= index) {
                if (Double.isNaN(previousMean)) {
                    // special case 1: the index we are interested in is before the 1st centroid
                    assert total == 0 : total;
                    if (nextIndex == previousIndex) {
                        return values.mean(next);
                    }
                    // assume values grow linearly between index previousIndex=0 and nextIndex2
                    int next2 = values.next(next);
                    final double nextIndex2 = total + values.count(next) + (values.count(next2) - 1.0) / 2;
                    previousMean = (nextIndex2 * values.mean(next) - nextIndex * values.mean(next2)) / (nextIndex2 - nextIndex);
                }
                // common case: we found two centroids previous and next so that the desired quantile is
                // after 'previous' but before 'next'
                return quantile(previousIndex, index, nextIndex, previousMean, values.mean(next));
            } else if (values.next(next) == IntAVLTree.NIL) {
                // special case 2: the index we are interested in is beyond the last centroid
                // again, assume values grow linearly between index previousIndex and (count - 1)
                // which is the highest possible index
                final double nextIndex2 = count - 1;
                final double nextMean2 = (values.mean(next) * (nextIndex2 - previousIndex) - previousMean * (nextIndex2 - nextIndex)) / (nextIndex - previousIndex);
                return quantile(nextIndex, index, nextIndex2, values.mean(next), nextMean2);
            }
            total += values.count(next);
            previousMean = values.mean(next);
            previousIndex = nextIndex;
            next = values.next(next);
        }
    }

    @Override
    public int centroidCount() {
        return summary.size();
    }

    @Override
    public Iterable<? extends Centroid> centroids() {
        return summary;
    }

    @Override
    public double compression() {
        return compression;
    }

    /**
     * Returns an upper bound on the number bytes that will be required to represent this histogram.
     */
    @Override
    public int byteSize() {
        return 4 + 8 + 4 + summary.size() * 12;
    }

    /**
     * Returns an upper bound on the number of bytes that will be required to represent this histogram in
     * the tighter representation.
     */
    @Override
    public int smallByteSize() {
        int bound = byteSize();
        ByteBuffer buf = ByteBuffer.allocate(bound);
        asSmallBytes(buf);
        return buf.position();
    }

    public final static int VERBOSE_ENCODING = 1;
    public final static int SMALL_ENCODING = 2;

    /**
     * Outputs a histogram as bytes using a particularly cheesy encoding.
     */
    @Override
    public void asBytes(ByteBuffer buf) {
        buf.putInt(VERBOSE_ENCODING);
        buf.putDouble(compression());
        buf.putInt(summary.size());
        for (Centroid centroid : summary) {
            buf.putDouble(centroid.mean());
        }

        for (Centroid centroid : summary) {
            buf.putInt(centroid.count());
        }
    }

    @Override
    public void asSmallBytes(ByteBuffer buf) {
        buf.putInt(SMALL_ENCODING);
        buf.putDouble(compression());
        buf.putInt(summary.size());

        double x = 0;
        for (Centroid centroid : summary) {
            double delta = centroid.mean() - x;
            x = centroid.mean();
            buf.putFloat((float) delta);
        }

        for (Centroid centroid : summary) {
            int n = centroid.count();
            encode(buf, n);
        }
    }

    /**
     * Reads a histogram from a byte buffer
     *
     * @return The new histogram structure
     */
    public static AVLTreeDigest fromBytes(ByteBuffer buf) {
        int encoding = buf.getInt();
        if (encoding == VERBOSE_ENCODING) {
            double compression = buf.getDouble();
            AVLTreeDigest r = new AVLTreeDigest(compression);
            int n = buf.getInt();
            double[] means = new double[n];
            for (int i = 0; i < n; i++) {
                means[i] = buf.getDouble();
            }
            for (int i = 0; i < n; i++) {
                r.add(means[i], buf.getInt());
            }
            return r;
        } else if (encoding == SMALL_ENCODING) {
            double compression = buf.getDouble();
            AVLTreeDigest r = new AVLTreeDigest(compression);
            int n = buf.getInt();
            double[] means = new double[n];
            double x = 0;
            for (int i = 0; i < n; i++) {
                double delta = buf.getFloat();
                x += delta;
                means[i] = x;
            }

            for (int i = 0; i < n; i++) {
                int z = decode(buf);
                r.add(means[i], z);
            }
            return r;
        } else {
            throw new IllegalStateException("Invalid format for serialized histogram");
        }
    }

}
