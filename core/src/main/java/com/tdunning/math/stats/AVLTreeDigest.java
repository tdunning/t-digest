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

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class AVLTreeDigest extends AbstractTDigest {
    private final double compression;
    private AVLGroupTree summary;

    private long count = 0; // package private for testing

    /**
     * A histogram structure that will record a sketch of a distribution.
     *
     * @param compression How should accuracy be traded for size?  A value of N here will give quantile errors
     *                    almost always less than 3/N with considerably smaller errors expected for extreme
     *                    quantiles.  Conversely, you should expect to track about 5 N centroids for this
     *                    accuracy.
     */
    @SuppressWarnings("WeakerAccess")
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
    public int centroidCount() {
        return summary.size();
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

    @Override
    public void add(List<? extends TDigest> others) {
        for (TDigest other : others) {
            setMinMax(Math.min(min, other.getMin()), Math.max(max, other.getMax()));
            for (Centroid centroid : other.centroids()) {
                add(centroid.mean(), centroid.count(), recordAllData ? centroid.data() : null);
            }
        }
    }

    public void add(double x, int w, List<Double> data) {
        checkValue(x);
        if (x < min) {
            min = x;
        }
        if (x > max) {
            max = x;
        }
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
                centroid = weightedAverage(centroid, count, x, w);
                count += w;
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
            nodes[i] = centroids.next(nodes[i - 1]);
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
            if (x < values.mean(values.first())) return 0;
            else if (x > values.mean(values.first())) return 1;
            else return 0.5;
        } else {
            if (x < min) {
                return 0;
            } else if (x == min) {
                return 0.5 / size();
            }
            assert x > min;

            if (x > max) {
                return 1;
            } else if (x == max) {
                long n = size();
                return (n - 0.5) / n;
            }
            assert x < max;

            int first = values.first();
            double firstMean = values.mean(first);
            if (x > min && x < firstMean) {
                int firstCount = values.count(first);
                assert firstCount > 1;
                if (firstCount == 2) {
                    // other sample in first centroid must be on the other side of the mean
                    return 1.0 / size();
                } else {
                    // how much weight is available for interpolation?
                    double weight = firstCount / 2.0 - 1;
                    // how much is between min and here?
                    double partialWeight = (x - min) / (firstMean - min) * weight;
                    // account for sample at min along with interpolated weight
                    return (partialWeight + 1.0) / size();
                }
            }

            int last = values.last();
            double lastMean = values.mean(last);
            if (x < max && x > lastMean) {
                int lastCount = values.count(last);
                assert lastCount > 1;
                if (lastCount == 2) {
                    // other sample in first centroid must be on the other side of the mean
                    return 1.0 / size();
                } else {
                    // how much weight is available for interpolation?
                    double weight = lastCount / 2.0 - 1;
                    // how much is between min and here?
                    double partialWeight = (max - x) / (max - lastMean) * weight;
                    // account for sample at min along with interpolated weight
                    return (partialWeight + 1.0) / size();
                }
            }
            assert values.size() >= 2;
            assert x >= firstMean;
            assert x <= lastMean;

            // we scan a across the centroids
            Iterator<Centroid> it = values.iterator();
            Centroid a = it.next();
            double aMean = a.mean();
            double aWeight = a.count();
            a = it.next();
            while (a != null && a.mean() == aMean) {
                aWeight += a.count();
                if (it.hasNext()) {
                    a = it.next();
                } else {
                    a = null;
                }
            }

            if (x == aMean) {
                return aWeight / 2.0 / size();
            }
            assert x > firstMean;

            if (a == null) {
                // all centroids were at the exact same place: firstMean == lastMean
                // but we know that x > firstMean && x <= lastMean
                // contradiction!!!!
                throw new IllegalStateException("Logical contradiction");
            }

            // b is the look-ahead to the next centroid
            Centroid b = a;
            double bMean = Double.NaN;
            double bWeight = 0;
            while (b != null && (Double.isNaN(bMean) || bMean == b.mean())) {
                bMean = b.mean();
                bWeight += b.count();

                if (it.hasNext()) {
                    b = it.next();
                } else {
                    b = null;
                }
            }

            assert bMean > aMean;

            // b is now the next centroid after current interval or null

            double weightSoFar = 0;

            // scan to last element
            while (bWeight != 0) {
                assert x > aMean;
                if (x == bMean) {
                    assert bMean > aMean;
                    return (weightSoFar + aWeight + bWeight / 2.0) / size();
                }
                assert x < bMean || x > bMean;

                if (x < bMean) {
                    // we are strictly between a and b
                    if (aWeight == 1) {
                        // but a might be a singleton
                        if (bWeight == 1) {
                            // we have passed all of a, but none of b, no interpolation
                            return (weightSoFar + 1.0) / size();
                        } else {
                            // only get to interpolate b's weight because a is a singleton
                            assert x > aMean;
                            assert bMean > aMean;

                            double partialWeight = (x - aMean) / (bMean - aMean) * bWeight / 2.0;
                            return (weightSoFar + 1.0 + partialWeight) / size();
                        }
                    } else if (bWeight == 1) {
                        // only get to interpolate a's weight because b is a singleton
                        double partialWeight = (x - aMean) / (bMean - aMean) * aWeight / 2.0;
                        return (weightSoFar + aWeight / 2.0 + partialWeight) / size();
                    } else {
                        // neither is singleton
                        double partialWeight = (x - aMean) / (bMean - aMean) * (aWeight + bWeight) / 2.0;
                        return (weightSoFar + aWeight / 2.0 + partialWeight) / size();
                    }
                }
                weightSoFar += aWeight;

                assert x > bMean;

                aMean = bMean;
                aWeight = bWeight;

                bMean = Double.NaN;
                bWeight = 0;
                while (b != null && (Double.isNaN(bMean) || bMean == b.mean())) {
                    bMean = b.mean();
                    bWeight += b.count();

                    if (it.hasNext()) {
                        b = it.next();
                    } else {
                        b = null;
                    }
                }
                assert bMean > aMean;
            }
            // shouldn't be possible because x <= lastMean
            throw new IllegalStateException("Ran out of centroids");
        }
    }

    /**
     * @param q The quantile desired.  Can be in the range [0,1].
     * @return The minimum value x such that we think that the proportion of samples is &le; x is q.
     */
    @Override
    public double quantile(double q) {
        if (q < 0 || q > 1) {
            throw new IllegalArgumentException("q should be in [0,1], got " + q);
        }

        AVLGroupTree values = summary;
        if (values.size() == 0) {
            // no centroids means no data, no way to get a quantile
            return Double.NaN;
        } else if (values.size() == 1) {
            // with one data point, all quantiles lead to Rome
            return values.iterator().next().mean();
        }

        // if values were stored in a sorted array, index would be the offset we are interested in
        final double index = q * count;
        int currentNode = values.first();
        int currentWeight = values.count(currentNode);

        // weightSoFar represents the total mass to the left of the center of the current node
        double weightSoFar = currentWeight / 2.0;

        // at left boundary, we interpolate between min and first mean
        if (index < weightSoFar) {
            return (min * index + values.mean(currentNode) * (weightSoFar - index)) / weightSoFar;
        }
        for (int i = 0; i < values.size() - 1; i++) {
            int nextNode = values.next(currentNode);
            int nextWeight = values.count(nextNode);
            // this is the mass between current center and next center
            double dw = (currentWeight + nextWeight) / 2.0;
            if (weightSoFar + dw > index) {
                // centroids i and i+1 bracket our current point
                double z1 = index - weightSoFar;
                double z2 = weightSoFar + dw - index;
                return weightedAverage(values.mean(currentNode), z2, values.mean(nextNode), z1);
            }
            weightSoFar += dw;
            currentNode = nextNode;
            currentWeight = nextWeight;
        }
        // index is in the right hand side of the last node, interpolate to max
        double z1 = index - weightSoFar;
        double z2 = currentWeight / 2.0 - z1;
        return weightedAverage(values.mean(currentNode), z2, max, z1);
    }

    @Override
    public Collection<Centroid> centroids() {
        return Collections.unmodifiableCollection(summary);
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
        return 32 + summary.size() * 12;
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

    private final static int VERBOSE_ENCODING = 1;
    private final static int SMALL_ENCODING = 2;

    /**
     * Outputs a histogram as bytes using a particularly cheesy encoding.
     */
    @Override
    public void asBytes(ByteBuffer buf) {
        buf.putInt(VERBOSE_ENCODING);
        buf.putDouble(min);
        buf.putDouble(max);
        buf.putDouble((float) compression());
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
        buf.putDouble(min);
        buf.putDouble(max);
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
     * @param buf The buffer to read from.
     * @return The new histogram structure
     */
    @SuppressWarnings("WeakerAccess")
    public static AVLTreeDigest fromBytes(ByteBuffer buf) {
        int encoding = buf.getInt();
        if (encoding == VERBOSE_ENCODING) {
            double min = buf.getDouble();
            double max = buf.getDouble();
            double compression = buf.getDouble();
            AVLTreeDigest r = new AVLTreeDigest(compression);
            r.setMinMax(min, max);
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
            double min = buf.getDouble();
            double max = buf.getDouble();
            double compression = buf.getDouble();
            AVLTreeDigest r = new AVLTreeDigest(compression);
            r.setMinMax(min, max);
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
