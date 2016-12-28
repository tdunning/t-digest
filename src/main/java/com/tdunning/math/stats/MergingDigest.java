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
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Maintains a t-digest by collecting new points in a buffer that is then sorted occasionally and merged
 * into a sorted array that contains previously computed centroids.
 * <p/>
 * This can be very fast because the cost of sorting and merging is amortized over several insertion. If
 * we keep N centroids total and have the input array is k long, then the amortized cost is something like
 * <p/>
 * N/k + log k
 * <p/>
 * These costs even out when N/k = log k.  Balancing costs is often a good place to start in optimizing an
 * algorithm.  For different values of compression factor, the following table shows estimated asymptotic
 * values of N and suggested values of k:
 * <table>
 * <thead>
 * <tr><td>Compression</td><td>N</td><td>k</td></tr>
 * </thead>
 * <tbody>
 * <tr><td>50</td><td>78</td><td>25</td></tr>
 * <tr><td>100</td><td>157</td><td>42</td></tr>
 * <tr><td>200</td><td>314</td><td>73</td></tr>
 * </tbody>
 * </table>
 * <p/>
 * The virtues of this kind of t-digest implementation include:
 * <ul>
 * <li>No allocation is required after initialization</li>
 * <li>The data structure automatically compresses existing centroids when possible</li>
 * <li>No Java object overhead is incurred for centroids since data is kept in primitive arrays</li>
 * </ul>
 * <p/>
 * The current implementation takes the liberty of using ping-pong buffers for implementing the merge resulting
 * in a substantial memory penalty, but the complexity of an in place merge was not considered as worthwhile
 * since even with the overhead, the memory cost is less than 40 bytes per centroid which is much less than half
 * what the AVLTreeDigest uses.  Speed tests are still not complete so it is uncertain whether the merge
 * strategy is faster than the tree strategy.
 */
public class MergingDigest extends AbstractTDigest {
    private final double compression;

    // points to the centroid that is currently being merged
    // if weight[lastUsedCell] == 0, then this is the number of centroids
    // else the number is lastUsedCell+1
    private int lastUsedCell;

    // sum_i weight[i]  See also unmergedWeight
    private double totalWeight = 0;

    // number of points that have been added to each merged centroid
    private double[] weight;
    // mean of points added to each merged centroid
    private double[] mean;
    // absolute min and max samples seen
    private double min, max;

    // history of all data added to centroids (for testing purposes)
    private List<List<Double>> data = null;

    // sum_i tempWeight[i]
    private double unmergedWeight = 0;

    // this is the index of the next temporary centroid
    // this is a more Java-like convention than lastUsedCell uses
    private int tempUsed = 0;
    private double[] tempWeight;
    private double[] tempMean;
    private List<List<Double>> tempData = null;


    // array used for sorting the temp centroids.  This is a field
    // to avoid allocations during operation
    private int[] order;

    /**
     * Allocates a buffer merging t-digest.  This is the normally used constructor that
     * allocates default sized internal arrays.  Other versions are available, but should
     * only be used for special cases.
     *
     * @param compression The compression factor
     */
    public MergingDigest(double compression) {
        this(compression, -1);
    }

    /**
     * If you know the size of the temporary buffer for incoming points, you can use this entry point.
     *
     * @param compression Compression factor for t-digest.  Same as 1/\delta in the paper.
     * @param bufferSize  How many samples to retain before merging.
     */
    public MergingDigest(double compression, int bufferSize) {
        // we can guarantee that we only need 2 * ceiling(compression).  
        this(compression, bufferSize, -1);
    }

    /**
     * Fully specified constructor.  Normally only used for deserializing a buffer t-digest.
     *
     * @param compression Compression factor
     * @param bufferSize  Number of temporary centroids
     * @param size        Size of main buffer
     */
    public MergingDigest(double compression, int bufferSize, int size) {
        if (size == -1) {
            size = (int) (2 * Math.ceil(compression));
        }
        if (bufferSize == -1) {
            // having a big buffer is good for speed
            bufferSize = (int) (8 * Math.ceil(compression));
        }
        this.compression = compression;

        weight = new double[size];
        mean = new double[size];
        min = Double.MAX_VALUE;
        max = -Double.MAX_VALUE;

        tempWeight = new double[bufferSize];
        tempMean = new double[bufferSize];
        order = new int[bufferSize];

        lastUsedCell = 0;
    }

    /**
     * Turns on internal data recording.
     */
    @Override
    public TDigest recordAllData() {
        super.recordAllData();
        data = new ArrayList<List<Double>>();
        tempData = new ArrayList<List<Double>>();
        return this;
    }

    @Override
    void add(double x, int w, Centroid base) {
        add(x, w, base.data());
    }

    @Override
    public void add(double x, int w) {
        add(x, w, (List<Double>) null);
    }

    public void add(double x, int w, List<Double> history) {
        if (Double.isNaN(x)) {
            throw new IllegalArgumentException("Cannot add NaN to t-digest");
        }
        if (tempUsed >= tempWeight.length - lastUsedCell - 1) {
            mergeNewValues();
        }
        int where = tempUsed++;
        tempWeight[where] = w;
        tempMean[where] = x;
        unmergedWeight += w;

        if (data != null) {
            if (tempData == null) {
                tempData = new ArrayList<List<Double>>();
            }
            while (tempData.size() <= where) {
                tempData.add(new ArrayList<Double>());
            }
            if (history == null) {
                history = Collections.singletonList(x);
            }
            tempData.get(where).addAll(history);
        }
    }

    private void mergeNewValues() {
        if (unmergedWeight > 0) {
            System.arraycopy(mean, 0, tempMean, tempUsed, lastUsedCell);
            System.arraycopy(weight, 0, tempWeight, tempUsed, lastUsedCell);
            tempUsed += lastUsedCell;

            if (tempData != null) {
                for (int i = 0; i < lastUsedCell; i++) {
                    assert data != null;
                    tempData.add(data.get(i));
                }
                data = new ArrayList<List<Double>>();
            }
            Sort.sort(order, tempMean, tempUsed);

            lastUsedCell = 0;
            totalWeight += unmergedWeight;
            unmergedWeight = 0;

            assert tempUsed > 0;
            mean[lastUsedCell] = tempMean[order[0]];
            weight[lastUsedCell] = tempWeight[order[0]];
            double wSoFar = weight[lastUsedCell];
            if (data != null) {
                assert data != null;
                data.add(tempData.get(0));
            }

            double k1 = 0;

            // weight will contain all zeros
            double wLimit = totalWeight * integratedQ(k1 + 1);
            for (int i = 1; i < tempUsed; i++) {
                int ix = order[i];
                double projectedW = wSoFar + tempWeight[ix];
                if (projectedW <= wLimit) {
                    // next point will fit
                    wSoFar = projectedW;

                    // merge into existing centroid
                    weight[lastUsedCell] += tempWeight[ix];
                    mean[lastUsedCell] = mean[lastUsedCell] + (tempMean[ix] - mean[lastUsedCell]) * tempWeight[ix] / weight[lastUsedCell];
                    tempWeight[ix] = 0;

                    if (data != null) {
                        while (data.size() <= lastUsedCell) {
                            data.add(new ArrayList<Double>());
                        }
                        assert tempData != null;
                        assert data.get(lastUsedCell) != tempData.get(ix);
                        data.get(lastUsedCell).addAll(tempData.get(ix));
                    }
                } else {
                    // didn't fit ... move to next output, copy out first centroid
                    k1 = integratedLocation(wSoFar / totalWeight);
                    wLimit = totalWeight * integratedQ(k1 + 1);
                    lastUsedCell++;
                    wSoFar += tempWeight[ix];
                    mean[lastUsedCell] = tempMean[ix];
                    weight[lastUsedCell] = tempWeight[ix];
                    tempWeight[ix] = 0;

                    if (data != null) {
                        assert tempData != null;
                        assert data.size() == lastUsedCell;
                        data.add(tempData.get(ix));
                    }
                }
            }
            // points to next empty cell
            lastUsedCell++;

            double sum = 0;
            for (int i = 0; i < lastUsedCell; i++) {
                sum += weight[i];
            }
            assert sum == totalWeight;

            tempUsed = 0;
            if (data != null) {
                tempData = new ArrayList<List<Double>>();
            }

            // contents of tempWeight will have been entirely zeroed by now

            // swap pointers for working space and merge space
            if (totalWeight > 0) {
                min = Math.min(min, mean[0]);
                max = Math.max(max, mean[lastUsedCell - 1]);
            }
        }
    }

    /**
     * Exposed for testing.
     */
    int checkWeights() {
        return checkWeights(weight, totalWeight, lastUsedCell);
    }

    private int checkWeights(double[] w, double total, int last) {
        int badCount = 0;

        int n = last;
        if (w[n] > 0) {
            n++;
        }

        double k1 = 0;
        double q = 0;
        for (int i = 0; i < n; i++) {
            double dq = w[i] / total;
            double k2 = integratedLocation(q + dq);
            if (k2 - k1 > 1 && w[i] != 1) {
                System.out.printf("Oversize centroid at %d, k0=%.2f, k1=%.2f, dk=%.2f, w=%.2f, q=%.4f\n", i, k1, k2, k2 - k1, w[i], q);
                badCount++;
            }
            if (k2 - k1 > 1.5 && w[i] != 1) {
                throw new IllegalStateException(String.format("Egregiously oversized centroid at %d, k0=%.2f, k1=%.2f, dk=%.2f, w=%.2f, q=%.4f\n", i, k1, k2, k2 - k1, w[i], q));
            }
            q += dq;
            k1 = k2;
        }

        return badCount;
    }

    /**
     * Converts a quantile into a centroid scale value.  The centroid scale is nominally
     * the number k of the centroid that a quantile point q should belong to.  Due to
     * round-offs, however, we can't align things perfectly without splitting points
     * and centroids.  We don't want to do that, so we have to allow for offsets.
     * In the end, the criterion is that any quantile range that spans a centroid
     * scale range more than one should be split across more than one centroid if
     * possible.  This won't be possible if the quantile range refers to a single point
     * or an already existing centroid.
     * <p/>
     * This mapping is steep near q=0 or q=1 so each centroid there will correspond to
     * less q range.  Near q=0.5, the mapping is flatter so that centroids there will
     * represent a larger chunk of quantiles.
     *
     * @param q The quantile scale value to be mapped.
     * @return The centroid scale value corresponding to q.
     */
    private double integratedLocation(double q) {
        return compression * (Math.asin(2 * q - 1) + Math.PI / 2) / Math.PI;
    }

    private double integratedQ(double k) {
        return (Math.sin(Math.min(k, compression) * Math.PI / compression - Math.PI / 2) + 1) / 2;
    }

    @Override
    public void compress() {
        mergeNewValues();
    }

    @Override
    public long size() {
        return (long) (totalWeight + unmergedWeight);
    }

    @Override
    public double cdf(double x) {
        mergeNewValues();

        if (lastUsedCell == 0) {
            if (weight[lastUsedCell] == 0) {
                // no data to examine
                return Double.NaN;
            } else {
                // exactly one centroid, probably have max==min
                if (x < min) {
                    return 0;
                } else if (x > max) {
                    return 1;
                } else if (max - min < Double.MIN_NORMAL) {
                    // x lands right on our only sample
                    return 0.5;
                } else {
                    // interpolate
                    return (x - min) / (max - min);
                }
            }
        } else {
            int n = lastUsedCell;
            if (weight[n] > 0) {
                n++;
            }

            if (x < min) {
                return 0;
            }

            if (x >= max) {
                return 1;
            }

            // we now know that there are at least two centroids
            double r = 0;
            // we scan a across the centroids
            double a = min;
            double aCount;

            double b = min;
            double bCount = 0;

            double left;
            double right = 0;

            // to find enclosing pair of centroids (counting min as a virtual centroid
            for (int it = 0; it < n; it++) {
                left = b - (a + right);
                a = b;
                aCount = bCount;

                b = mean[it];
                bCount = weight[it];
                right = (b - a) * aCount / (aCount + bCount);

                // we know that x >= a-left
                if (x < a + right) {
                    double value = (r + aCount * interpolate(x, a - left, a + right)) / totalWeight;
                    return value > 0.0 ? value : 0.0;
                }

                r += aCount;
            }

            left = b - (a + right);
            a = b;
            aCount = bCount;
            right = max - a;

            // for the last element, use max to determine right
            if (x < a + right) {
                return (r + aCount * interpolate(x, a - left, a + right)) / totalWeight;
            } else {
                return 1;
            }
        }
    }

    @Override
    public double quantile(double q) {
        if (q < 0 || q > 1) {
            throw new IllegalArgumentException("q should be in [0,1], got " + q);
        }
        mergeNewValues();

        if (lastUsedCell == 0 && weight[lastUsedCell] == 0) {
            return Double.NaN;
        } else if (lastUsedCell == 0) {
            return mean[0];
        }

        // we know that there are at least two centroids now

        int n = lastUsedCell;

        // if values were stored in a sorted array, index would be the offset we are interested in
        final double index = q * totalWeight;

        double left;
        double a;
        double aCount;

        double right = min;
        double b = mean[0];
        double bCount = weight[0];

        double weightSoFar = 0;
        for (int it = 1; it < n; it++) {
            a = b;
            aCount = bCount;
            left = right;

            b = mean[it];
            bCount = weight[it];
            right = (bCount * a + aCount * b) / (aCount + bCount);

            if (index < weightSoFar + aCount) {
                // belongs to left side of a
                double p = (index - weightSoFar) / aCount;
                return left * (1 - p) + right * p;
            }
            weightSoFar += aCount;
        }

        left = right;
        aCount = bCount;
        right = max;

        if (index < weightSoFar + aCount) {
            // belongs to left side of a
            double p = (index - weightSoFar) / aCount;
            return left * (1 - p) + right * p;
        } else {
            return max;
        }
    }

    @Override
    public Collection<Centroid> centroids() {
        // we don't actually keep centroid structures around so we have to fake it
        compress();
        return new AbstractCollection<Centroid>() {
            @Override
            public Iterator<Centroid> iterator() {
                return new Iterator<Centroid>() {
                    int i = 0;

                    @Override
                    public boolean hasNext() {
                        return i < lastUsedCell;
                    }

                    @Override
                    public Centroid next() {
                        Centroid rc = new Centroid(mean[i], (int) weight[i], data != null ? data.get(i) : null);
                        i++;
                        return rc;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("Default operation");
                    }
                };
            }

            @Override
            public int size() {
                return lastUsedCell;
            }
        };
    }

    @Override
    public double compression() {
        return compression;
    }

    @Override
    public int byteSize() {
        compress();
        // format code, compression(float), buffer-size(int), temp-size(int), #centroids-1(int),
        // then two doubles per centroid
        return lastUsedCell * 16 + 40;
    }

    @Override
    public int smallByteSize() {
        compress();
        // format code(int), compression(float), buffer-size(short), temp-size(short), #centroids-1(short),
        // then two floats per centroid
        return lastUsedCell * 8 + 30;
    }

    /**
     * Over-ride the min and max values for testing purposes
     */
    void setMinMax(double min, double max) {
        this.min = min;
        this.max = max;
    }

    public enum Encoding {
        VERBOSE_ENCODING(1), SMALL_ENCODING(2);

        private final int code;

        Encoding(int code) {
            this.code = code;
        }
    }

    @Override
    public void asBytes(ByteBuffer buf) {
        compress();
        buf.putInt(Encoding.VERBOSE_ENCODING.code);
        buf.putDouble(min);
        buf.putDouble(max);
        buf.putFloat((float) compression);
        buf.putFloat((float) compression);
        buf.putInt(mean.length);
        buf.putInt(tempMean.length);
        buf.putInt(lastUsedCell);
        for (int i = 0; i < lastUsedCell; i++) {
            buf.putDouble(weight[i]);
            buf.putDouble(mean[i]);
        }
    }

    @Override
    public void asSmallBytes(ByteBuffer buf) {
        compress();
        buf.putInt(Encoding.SMALL_ENCODING.code);    // 4
        buf.putDouble(min);                          // + 8
        buf.putDouble(max);                          // + 8
        buf.putFloat((float) compression);           // + 4
        buf.putShort((short) mean.length);           // + 2
        buf.putShort((short) tempMean.length);       // + 2
        buf.putShort((short) lastUsedCell);          // + 2 = 30
        for (int i = 0; i < lastUsedCell; i++) {
            buf.putFloat((float) weight[i]);
            buf.putFloat((float) mean[i]);
        }
    }

    public static MergingDigest fromBytes(ByteBuffer buf) {
        int encoding = buf.getInt();
        if (encoding == Encoding.VERBOSE_ENCODING.code) {
            double min = buf.getDouble();
            double max = buf.getDouble();
            double compression = buf.getFloat();
            int n = buf.getInt();
            int bufferSize = buf.getInt();
            MergingDigest r = new MergingDigest(compression, bufferSize, n);
            r.min = min;
            r.max = max;
            r.lastUsedCell = buf.getInt();
            for (int i = 0; i < r.lastUsedCell; i++) {
                r.weight[i] = buf.getDouble();
                r.mean[i] = buf.getDouble();

                r.totalWeight += r.weight[i];
            }
            return r;
        } else if (encoding == Encoding.SMALL_ENCODING.code) {
            double min = buf.getDouble();
            double max = buf.getDouble();
            double compression = buf.getFloat();
            int n = buf.getShort();
            int bufferSize = buf.getShort();
            MergingDigest r = new MergingDigest(compression, bufferSize, n);
            r.min = min;
            r.max = max;
            r.lastUsedCell = buf.getShort();
            for (int i = 0; i < r.lastUsedCell; i++) {
                r.weight[i] = buf.getFloat();
                r.mean[i] = buf.getFloat();

                r.totalWeight += r.weight[i];
            }
            return r;
        } else {
            throw new IllegalStateException("Invalid format for serialized histogram");
        }

    }
}
