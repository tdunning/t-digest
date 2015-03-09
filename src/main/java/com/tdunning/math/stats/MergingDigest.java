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
import java.util.*;

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
    private int lastUsedCell;
    private double totalWeight;
    private double[] weight;
    private double[] mean;
    private List<List<Double>> data = null;

    private double[] mergeWeight;
    private double[] mergeMean;
    private List<List<Double>> mergeData = null;

    private double unmergedWeight = 0;

    private int tempUsed = 0;
    private double[] tempWeight;
    private double[] tempMean;
    private int[] order;

    public MergingDigest(double compression) {
        // magic formula created by regressing against known sizes for sample compression values
        this(compression, (int) (7.5 + 0.37 * compression - 2e-4 * compression * compression));
    }

    public MergingDigest(double compression, int bufferSize) {
        this.compression = compression;
        int size = (int) (Math.PI * compression + 0.5);
        weight = new double[size];
        mean = new double[size];

        mergeWeight = new double[size];
        mergeMean = new double[size];

        tempWeight = new double[bufferSize];
        tempMean = new double[bufferSize];
        order = new int[bufferSize];

        lastUsedCell = 0;
    }

    @Override
    public TDigest recordAllData() {
        super.recordAllData();
        data = new ArrayList<List<Double>>();
        mergeData = new ArrayList<List<Double>>();
        return this;
    }

    @Override
    void add(double x, int w, Centroid base) {
        add(x, w);
    }

    @Override
    public void add(double x, int w) {
        if (Double.isNaN(x)) {
            throw new IllegalArgumentException("Cannot add NaN to t-digest");
        }
        if (tempUsed >= tempWeight.length) {
            mergeNewValues();
        }
        int where = tempUsed++;
        tempWeight[where] = w;
        tempMean[where] = x;
        unmergedWeight += w;
    }

    private void mergeNewValues() {
        if (tempUsed > 0) {
            Sort.sort(order, tempMean);
        }


        double wSoFar = 0;
        double k1 = 0;
        int i = 0;
        int j = 0;
        int n = totalWeight == 0 ? 0 : lastUsedCell + 1;
        lastUsedCell = -1;
        totalWeight += unmergedWeight;

        while (i < tempUsed && j < n) {
            int ix = order[i];
            if (tempMean[ix] <= mean[j]) {
                wSoFar += tempWeight[ix];
                k1 = mergeCentroid(wSoFar, k1, tempWeight[ix], tempMean[ix], Arrays.asList(tempMean[ix]));
                i++;
            } else {
                wSoFar += weight[j];
                k1 = mergeCentroid(wSoFar, k1, weight[j], mean[j], data.get(j));
                j++;
            }
        }

        while (i < tempUsed) {
            int ix = order[i];
            wSoFar += tempWeight[ix];
            k1 = mergeCentroid(wSoFar, k1, tempWeight[ix], tempMean[ix], Arrays.asList(tempMean[ix]));
            i++;
        }

        while (j < n) {
            wSoFar += weight[j];
            k1 = mergeCentroid(wSoFar, k1, weight[j], mean[j], data.get(j));
            j++;
        }
        tempUsed = 0;
        unmergedWeight = 0;

        // swap pointers for working space and merge space
        double[] z = weight;
        weight = mergeWeight;
        mergeWeight = z;

        z = mean;
        mean = mergeMean;
        mergeMean = z;

        if (data != null) {
            data = mergeData;
            mergeData = new ArrayList<List<Double>>();
        }

        Arrays.fill(mergeWeight, 0);
    }

    private double mergeCentroid(double wSoFar, double k1, double weight, double mean, List<Double> newData) {
        double k2 = integratedLocation(wSoFar / totalWeight);
        if (k2 - k1 <= 1 && lastUsedCell >= 0) {
            double newWeight = mergeWeight[lastUsedCell] + weight;
            mergeMean[lastUsedCell] = (mergeMean[lastUsedCell] * mergeWeight[lastUsedCell] + mean * weight) / newWeight;
            mergeWeight[lastUsedCell] = newWeight;
            if (mergeData != null) {
                while (mergeData.size() <= lastUsedCell) {
                    mergeData.add(new ArrayList<Double>());
                }
                mergeData.get(lastUsedCell).addAll(newData);
            }
        } else {
            lastUsedCell++;
            mergeMean[lastUsedCell] = mean;
            mergeWeight[lastUsedCell] = weight;
            if (mergeData != null) {
                while (mergeData.size() <= lastUsedCell) {
                    mergeData.add(new ArrayList<Double>());
                }
                mergeData.get(lastUsedCell).addAll(newData);
            }

            k1 = k2;
        }
        return k1;
    }

    private double integratedLocation(double q) {
        return compression * Math.asin(q);
    }

    @Override
    public void compress() {
        mergeNewValues();
        // make an extra pass just to make sure we have compacted things as much as possible
        // seems unnecessary, but should remain until proven redundant
        mergeNewValues();
    }

    @Override
    public long size() {
        return (long) (totalWeight + unmergedWeight);
    }

    @Override
    public double cdf(double x) {
        mergeNewValues();
        if (lastUsedCell < 0) {
            return Double.NaN;
        } else if (lastUsedCell == 0) {
            return x < mean[0] ? 0 : 1;
        } else {
            double r = 0;

            // we scan a across the centroids
            int i = 0;

            // initially, we set left width equal to right width
            double left = (mean[i + 1] - mean[i]) / 2;
            double right = left;

            // scan to next to last element
            while (i < lastUsedCell) {
                if (x < mean[i] + right) {
                    return (r + weight[i] * interpolate(x, mean[i] - left, mean[i] + right)) / totalWeight;
                }
                r += weight[i];

                i++;

                left = right;
                right = (mean[i + 1] - mean[i]) / 2;
            }

            // i == lastUsedCell

            // for the last element, assume right width is same as left
            left = right;
            if (x < mean[i] + right) {
                return (r + weight[i] * interpolate(x, mean[i] - left, mean[i] + right)) / totalWeight;
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

        if (lastUsedCell < 0) {
            return Double.NaN;
        } else if (lastUsedCell == 0) {
            return mean[0];
        }

        // if values were stored in a sorted array, index would be the offset we are interested in
        final double index = q * (totalWeight - 1);

        if (index < (weight[0] - 1) / 2) {
            // special case 1: the index we are interested in is before the 1st centroid
            return quantile(index, (weight[0] - 1) / 2, weight[0] + (weight[1] - 1) / 2, mean[0], mean[1]);
        }

        double previousMean = mean[0];
        double previousIndex = 0;
        int next = 0;
        long total = 0;

        int last = weight[lastUsedCell] == 0 ? lastUsedCell - 1 : lastUsedCell;
        double lastIndex = totalWeight - 1 - weight[last] / 2;
        while (index <= lastIndex) {
            final double nextIndex = total + (weight[next] - 1.0) / 2;
            if (nextIndex >= index) {
                // common case: we found two centroids previous and next so that the desired quantile is
                // after 'previous' but before 'next'
                return quantile(index, previousIndex, nextIndex, previousMean, mean[next]);
            }
            total += weight[next];
            previousMean = mean[next];
            previousIndex = nextIndex;
            next++;
        }

        // special case 2: the index we are interested in is beyond the last centroid
        // again, assume values grow linearly between index previousIndex and (count - 1)
        // which is the highest possible index
        return quantile(index, previousIndex, lastIndex, mean[last - 1], mean[last]);
    }

    @Override
    public Collection<Centroid> centroids() {
        compress();
        List<Centroid> r = new ArrayList<Centroid>(lastUsedCell);
        for (int i = 0; i <= lastUsedCell; i++) {
            if (data != null) {
                r.add(new Centroid(mean[i], (int) weight[i], data.get(i)));
            } else {
                r.add(new Centroid(mean[i], (int) weight[i]));
            }
        }
        return r;
    }

    @Override
    public double compression() {
        return compression;
    }

    @Override
    public int byteSize() {
        compress();
        // 4 bytes of size, then two doubles per centroid
        return lastUsedCell * 16 + 4;
    }

    @Override
    public int smallByteSize() {
        compress();
        // 4 bytes of size, then two floats per centroid
        return lastUsedCell * 8 + 2;
    }

    @Override
    public void asBytes(ByteBuffer buf) {
        compress();
        buf.putInt(lastUsedCell);
        for (int i = 0; i < lastUsedCell; i++) {
            buf.putDouble(weight[i]);
            buf.putDouble(mean[i]);
        }
    }

    @Override
    public void asSmallBytes(ByteBuffer buf) {
        compress();
        buf.putShort((short) lastUsedCell);
        for (int i = 0; i < lastUsedCell; i++) {
            buf.putFloat((float) weight[i]);
            buf.putFloat((float) mean[i]);
        }
    }
}
