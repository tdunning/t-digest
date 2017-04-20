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

    // points to the first unused centroid
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
    @SuppressWarnings("WeakerAccess")
    public MergingDigest(double compression) {
        this(compression, -1);
    }

    /**
     * If you know the size of the temporary buffer for incoming points, you can use this entry point.
     *
     * @param compression Compression factor for t-digest.  Same as 1/\delta in the paper.
     * @param bufferSize  How many samples to retain before merging.
     */
    @SuppressWarnings("WeakerAccess")
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
    @SuppressWarnings("WeakerAccess")
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
        data = new ArrayList<>();
        tempData = new ArrayList<>();
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
                tempData = new ArrayList<>();
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

    public void add(double[] m, double[] w, int count, List<List<Double>> data) {
        if (m.length != w.length) {
            throw new IllegalArgumentException("Arrays not same length");
        }
        if (m.length < count + lastUsedCell) {
            // make room to add existing centroids
            double[] m1 = new double[count + lastUsedCell];
            System.arraycopy(m, 0, m1, 0, count);
            m = m1;
            double[] w1 = new double[count + lastUsedCell];
            System.arraycopy(w, 0, w1, 0, count);
            w = w1;
        }
        double total = 0;
        for (int i = 0; i < count; i++) {
            total += w[i];
        }
        merge(m, w, count, data, null, total);
    }

    @Override
    public void add(List<? extends TDigest> others) {
        if (others.size() == 0) {
            return;
        }
        int size = lastUsedCell;
        for (TDigest other : others) {
            other.compress();
            size += other.centroidCount();
        }

        double[] m = new double[size];
        double[] w = new double[size];
        List<List<Double>> data;
        if (recordAllData) {
            data = new ArrayList<>();
        } else {
            data = null;
        }
        int offset = 0;
        for (TDigest other : others) {
            if (other instanceof MergingDigest) {
                MergingDigest md = (MergingDigest) other;
                System.arraycopy(md.mean, 0, m, offset, md.lastUsedCell);
                System.arraycopy(md.weight, 0, w, offset, md.lastUsedCell);
                if (data != null) {
                    for (Centroid centroid : other.centroids()) {
                        data.add(centroid.data());
                    }
                }
                offset += md.lastUsedCell;
            } else {
                for (Centroid centroid : other.centroids()) {
                    m[offset] = centroid.mean();
                    w[offset] = centroid.count();
                    if (recordAllData){
                        assert data != null;
                        data.add(centroid.data());
                    }
                    offset++;
                }
            }
        }
        add(m, w, size, data);
    }

    private void mergeNewValues() {
        if (unmergedWeight > 0) {
            merge(tempMean, tempWeight, tempUsed, tempData, order, unmergedWeight);
            tempUsed = 0;
            unmergedWeight = 0;
            if (data != null) {
                tempData = new ArrayList<>();
            }

        }
    }

    private void merge(double[] incomingMean, double[] incomingWeight, int incomingCount, List<List<Double>> incomingData, int[] incomingOrder, double unmergedWeight) {
        System.arraycopy(mean, 0, incomingMean, incomingCount, lastUsedCell);
        System.arraycopy(weight, 0, incomingWeight, incomingCount, lastUsedCell);
        incomingCount += lastUsedCell;

        if (incomingData != null) {
            for (int i = 0; i < lastUsedCell; i++) {
                assert data != null;
                incomingData.add(data.get(i));
            }
            data = new ArrayList<>();
        }
        if (incomingOrder == null) {
            incomingOrder = new int[incomingCount];
        }
        Sort.sort(incomingOrder, incomingMean, incomingCount);

        lastUsedCell = 0;
        totalWeight += unmergedWeight;

        assert incomingCount > 0;
        mean[lastUsedCell] = incomingMean[incomingOrder[0]];
        weight[lastUsedCell] = incomingWeight[incomingOrder[0]];
        double wSoFar = weight[lastUsedCell];
        if (data != null) {
            assert incomingData != null;
            data.add(incomingData.get(incomingOrder[0]));
        }

        double k1 = 0;

        // weight will contain all zeros
        double wLimit = totalWeight * integratedQ(k1 + 1);
        for (int i = 1; i < incomingCount; i++) {
            int ix = incomingOrder[i];
            double projectedW = wSoFar + incomingWeight[ix];
            if (projectedW <= wLimit) {
                // next point will fit
                wSoFar = projectedW;

                // merge into existing centroid
                weight[lastUsedCell] += incomingWeight[ix];
                mean[lastUsedCell] = mean[lastUsedCell] + (incomingMean[ix] - mean[lastUsedCell]) * incomingWeight[ix] / weight[lastUsedCell];
                incomingWeight[ix] = 0;

                if (data != null) {
                    while (data.size() <= lastUsedCell) {
                        data.add(new ArrayList<Double>());
                    }
                    assert incomingData != null;
                    assert data.get(lastUsedCell) != incomingData.get(ix);
                    data.get(lastUsedCell).addAll(incomingData.get(ix));
                }
            } else {
                // didn't fit ... move to next output, copy out first centroid
                k1 = integratedLocation(wSoFar / totalWeight);
                wLimit = totalWeight * integratedQ(k1 + 1);
                lastUsedCell++;
                wSoFar += incomingWeight[ix];
                mean[lastUsedCell] = incomingMean[ix];
                weight[lastUsedCell] = incomingWeight[ix];
                incomingWeight[ix] = 0;

                if (data != null) {
                    assert incomingData != null;
                    assert data.size() == lastUsedCell;
                    data.add(incomingData.get(ix));
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

        if (totalWeight > 0) {
            min = Math.min(min, mean[0]);
            max = Math.max(max, mean[lastUsedCell - 1]);
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
            // no data to examine
            return Double.NaN;
        } else if (lastUsedCell == 1) {
            // exactly one centroid, should have max==min
            double width = max - min;
            if (x < min) {
                return 0;
            } else if (x > max) {
                return 1;
            } else if (x - min <= width) {
                // min and max are too close together to do any viable interpolation
                return 0.5;
            } else {
                // interpolate if somehow we have weight > 0 and max != min
                return (x - min) / (max - min);
            }
        } else {
            int n = lastUsedCell;
            if (x <= min) {
                return 0;
            }

            if (x >= max) {
                return 1;
            }

            // check for the left tail
            if (x <= mean[0]) {
                // note that this is different than mean[0] > min ... this guarantees interpolation works
                if (mean[0] - min > 0) {
                    return (x - min) / (mean[0] - min) * weight[0] / totalWeight / 2;
                } else {
                    return 0;
                }
            }
            assert x > mean[0];

            // and the right tail
            if (x >= mean[n - 1]) {
                if (max - mean[n - 1] > 0) {
                    return 1 - (max - x) / (max - mean[n - 1]) * weight[n - 1] / totalWeight / 2;
                } else {
                    return 1;
                }
            }
            assert x < mean[n - 1];

            // we know that there are at least two centroids and x > mean[0] && x < mean[n-1]
            // that means that there are either a bunch of consecutive centroids all equal at x
            // or there are consecutive centroids, c0 <= x and c1 > x
            double weightSoFar = weight[0] / 2;
            for (int it = 0; it < n; it++) {
                if (mean[it] == x) {
                    double w0 = weightSoFar;
                    while (it < n && mean[it + 1] == x) {
                        weightSoFar += (weight[it] + weight[it + 1]);
                        it++;
                    }
                    return (w0 + weightSoFar) / 2 / totalWeight;
                }
                if (mean[it] <= x && mean[it + 1] > x) {
                    if (mean[it + 1] - mean[it] > 0) {
                        double dw = (weight[it] + weight[it + 1]) / 2;
                        return (weightSoFar + dw * (x - mean[it]) / (mean[it + 1] - mean[it])) / totalWeight;
                    } else {
                        // this is simply caution against floating point madness
                        // it is conceivable that the centroids will be different
                        // but too near to allow safe interpolation
                        double dw = (weight[it] + weight[it + 1]) / 2;
                        return weightSoFar + dw / totalWeight;
                    }
                }
                weightSoFar += (weight[it] + weight[it + 1]) / 2;
            }
            // it should not be possible for the loop fall through
            throw new IllegalStateException("Can't happen ... loop fell through");
        }
    }

    @Override
    public double quantile(double q) {
        if (q < 0 || q > 1) {
            throw new IllegalArgumentException("q should be in [0,1], got " + q);
        }
        mergeNewValues();

        if (lastUsedCell == 0 && weight[lastUsedCell] == 0) {
            // no centroids means no data, no way to get a quantile
            return Double.NaN;
        } else if (lastUsedCell == 0) {
            // with one data point, all quantiles lead to Rome
            return mean[0];
        }

        // we know that there are at least two centroids now
        int n = lastUsedCell;

        // if values were stored in a sorted array, index would be the offset we are interested in
        final double index = q * totalWeight;

        // at the boundaries, we return min or max
        if (index < weight[0] / 2) {
            assert weight[0] > 0;
            return min + 2 * index / weight[0] * (mean[0] - min);
        }

        // in between we interpolate between centroids
        double weightSoFar = weight[0] / 2;
        for (int i = 0; i < n - 1; i++) {
            double dw = (weight[i] + weight[i + 1]) / 2;
            if (weightSoFar + dw > index) {
                // centroids i and i+1 bracket our current point
                double z1 = index - weightSoFar;
                double z2 = weightSoFar + dw - index;
                return (mean[i] * z2 + mean[i + 1] * z1) / dw;
            }
            weightSoFar += dw;
        }
        assert index <= totalWeight;
        assert index >= totalWeight - weight[n - 1] / 2;

        // weightSoFar = totalWeight - weight[n-1]/2 (very nearly)
        // so we interpolate out to max value ever seen
        return mean[n - 1] + (index - (totalWeight - weight[n - 1] / 2)) / (weight[n - 1] / 2) * (max - mean[n - 1]);
    }

    @Override
    public int centroidCount() {
        return lastUsedCell;
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
    @SuppressWarnings("SameParameterValue")
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

    @SuppressWarnings("WeakerAccess")
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
