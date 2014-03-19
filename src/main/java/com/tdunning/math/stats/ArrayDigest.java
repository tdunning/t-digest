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
import java.util.NoSuchElementException;

/**
 * Array based implementation of a TDigest.
 * <p/>
 * This implementation is essentially a one-level b-tree in which nodes are collected into
 * pages typically with 32 values per page.  Commonly, an ArrayDigest contains 500-3000
 * centroids.  With 32 values per page, we have about 32 values per page and about 30 pages
 * which seems to give a nice balance for speed.  Sizes from 4 to 100 are plausible, however.
 */
public class ArrayDigest extends AbstractTDigest {
    private final int pageSize;

    private List<Page> data = new ArrayList<Page>();
    private int totalWeight = 0;
    private int centroidCount = 0;
    private double compression = 100;

    public ArrayDigest(int pageSize, double compression) {
        if (pageSize > 3) {
            this.pageSize = pageSize;
            this.compression = compression;
        } else {
            throw new IllegalArgumentException("Must have page size of 4 or more");
        }
    }

    @Override
    public void add(double x, int w) {
        Index start = floor(x);
        if (start == null) {
            start = ceiling(x);
        }

        if (start == null) {
            addRaw(x, w);
        } else {
            Iterable<Index> neighbors = inclusiveTail(start);
            double minDistance = Double.MAX_VALUE;
            int lastNeighbor = 0;
            int i = headCount(start);
            for (Index neighbor : neighbors) {
                double z = Math.abs(mean(neighbor) - x);
                if (z <= minDistance) {
                    minDistance = z;
                    lastNeighbor = i;
                } else {
                    // as soon as z exceeds the minimum, we have passed the nearest neighbor and can quit
                    break;
                }
                i++;
            }

            Index closest = null;
            int sum = headSum(start);
            i = headCount(start);
            double n = 0;
            for (Index neighbor : neighbors) {
                if (i > lastNeighbor) {
                    break;
                }
                double z = Math.abs(mean(neighbor) - x);
                double q = (sum + count(neighbor) / 2.0) / totalWeight;
                double k = 4 * totalWeight * q * (1 - q) / compression;

                // this slightly clever selection method improves accuracy with lots of repeated points
                if (z == minDistance && count(neighbor) + w <= k) {
                    n++;
                    if (gen.nextDouble() < 1 / n) {
                        closest = neighbor;
                    }
                }
                sum += count(neighbor);
                i++;
            }

            if (closest == null) {
                addRaw(x, w);
            } else {
                if (n == 1) {
                    // if the nearest point was unique, centroid ordering cannot change
                    Page p = data.get(closest.page);
                    p.counts[closest.subPage] += w;
                    p.totalCount += w;
                    p.centroids[closest.subPage] += (x - p.centroids[closest.subPage]) / p.counts[closest.subPage];
                    if (p.history != null && p.history.get(closest.subPage) != null) {
                        p.history.get(closest.subPage).add(x);
                    }
                    totalWeight += w;
                } else {
                    // if the nearest point was not unique, then we may not be modifying the first copy
                    // which means that ordering can change
                    int weight = count(closest) + w;
                    double center = mean(closest);
                    center = center + (x - center) / weight;

                    if (mean(increment(closest, -1)) <= center && mean(increment(closest, 1)) >= center) {
                        // if order doesn't change, we can short-cut the process
                        Page p = data.get(closest.page);
                        p.counts[closest.subPage] = weight;
                        p.centroids[closest.subPage] = center;

                        p.totalCount += w;
                        totalWeight += w;
                        if (p.history != null && p.history.get(closest.subPage) != null) {
                            p.history.get(closest.subPage).add(x);
                        }
                    } else {
                        delete(closest);

                        List<Double> history = history(closest);
                        if (history != null) {
                            history.add(x);
                        }

                        addRaw(center, weight, history);
                    }

                }
            }

            if (centroidCount > 100 * compression) {
                // something such as sequential ordering of data points
                // has caused a pathological expansion of our summary.
                // To fight this, we simply replay the current centroids
                // in random order.

                // this causes us to forget the diagnostic recording of data points
                compress();
            }
        }
    }

    public int headSum(Index limit) {
        int r = 0;

        for (int i = 0; limit != null && i < limit.page; i++) {
            r += data.get(i).totalCount;
        }

        if (limit != null && limit.page < data.size()) {
            for (int j = 0; j < limit.subPage; j++) {
                r += data.get(limit.page).counts[j];
            }
        }

        return r;
    }

    /**
     * Returns the number of centroids strictly before the limit.
     */
    private int headCount(Index limit) {
        int r = 0;

        for (int i = 0; i < limit.page; i++) {
            r += data.get(i).active;
        }

        if (limit.page < data.size()) {
            for (int j = 0; j < limit.subPage; j++) {
                r++;
            }
        }

        return r;
    }

    public double mean(Index index) {
        return data.get(index.page).centroids[index.subPage];
    }

    public int count(Index index) {
        return data.get(index.page).counts[index.subPage];
    }

    @Override
    public void compress() {
        ArrayDigest reduced = new ArrayDigest(pageSize, compression);
        if (recordAllData) {
            reduced.recordAllData();
        }
        List<Index> tmp = new ArrayList<Index>();
        Iterator<Index> ix = this.iterator(0, 0);
        while (ix.hasNext()) {
            tmp.add(ix.next());
        }

        Collections.shuffle(tmp, gen);
        for (Index index : tmp) {
            reduced.add(mean(index), count(index));
        }

        data = reduced.data;
        centroidCount = reduced.centroidCount;
    }

    @Override
    public int size() {
        return totalWeight;
    }

    @Override
    public int centroidCount() {
        return centroidCount;
    }

    @Override
    public Iterable<Centroid> centroids() {
        List<Centroid> r = new ArrayList<Centroid>();
        Iterator<Index> ix = iterator(0, 0);
        while (ix.hasNext()) {
            Index index = ix.next();
            Page current = data.get(index.page);
            Centroid centroid = new Centroid(current.centroids[index.subPage], current.counts[index.subPage]);
            if (current.history != null) {
                for (double x : current.history.get(index.subPage)) {
                    centroid.insertData(x);
                }
            }
            r.add(centroid);
        }
        return r;
    }

    public Iterator<Index> allAfter(double x) {
        if (data.size() == 0) {
            return iterator(0, 0);
        } else {

            for (int i = 1; i < data.size(); i++) {
                if (data.get(i).centroids[0] >= x) {
                    Page previous = data.get(i - 1);
                    for (int j = 0; j < previous.active; j++) {
                        if (previous.centroids[j] > x) {
                            return iterator(i - 1, j);
                        }
                    }
                    return iterator(i, 0);
                }
            }

            Page last = data.get(data.size() - 1);
            for (int j = 0; j < last.active; j++) {
                if (last.centroids[j] > x) {
                    return iterator(data.size() - 1, j);
                }
            }
            return iterator(data.size(), 0);
        }
    }

    /**
     * Returns a cursor pointing to the first element <= x.  Exposed only for testing.
     * @param x The value used to find the cursor.
     * @return The cursor.
     */
    public Index floor(double x) {
        Iterator<Index> rx = allBefore(x);
        if (!rx.hasNext()) {
            return null;
        }
        Index r = rx.next();
        Index z = r;
        while (rx.hasNext() && mean(z) == x) {
            r = z;
            z = rx.next();
        }
        return r;
    }

    public Index ceiling(double x) {
        Iterator<Index> r = allAfter(x);
        return r.hasNext() ? r.next() : null;
    }

    /**
     * Returns an iterator which will give each element <= to x in non-increasing order.
     *
     * @param x The upper bound of all returned elements
     * @return An iterator that returns elements in non-increasing order.
     */
    public Iterator<Index> allBefore(double x) {
        if (data.size() == 0) {
            return iterator(0, 0);
        } else {
            for (int i = 1; i < data.size(); i++) {
                if (data.get(i).centroids[0] > x) {
                    Page previous = data.get(i - 1);
                    for (int j = 0; j < previous.active; j++) {
                        if (previous.centroids[j] > x) {
                            return reverse(i - 1, j - 1);
                        }
                    }
                    return reverse(i, -1);
                }
            }
            Page last = data.get(data.size() - 1);
            for (int j = 0; j < last.active; j++) {
                if (last.centroids[j] > x) {
                    return reverse(data.size() - 1, j - 1);
                }
            }
            return reverse(data.size(), -1);
        }
    }

    public Index increment(Index x, int delta) {
        int i = x.page;
        int j = x.subPage + delta;

        while (i < data.size() && j >= data.get(i).active) {
            j -= data.get(i).active;
            i++;
        }

        while (i > 0 && j < 0) {
            i--;
            j += data.get(i).active;
        }
        return new Index(i, j);
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
        return 4 + 8 + 8 + centroidCount * 12;
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

    /**
     * Outputs a histogram as bytes using a particularly cheesy encoding.
     */
    @Override
    public void asBytes(ByteBuffer buf) {
        buf.putInt(VERBOSE_ARRAY_DIGEST);
        buf.putDouble(compression());
        buf.putInt(pageSize);
        buf.putInt(centroidCount);
        for (Page page : data) {
            for (int i = 0; i < page.active; i++) {
                buf.putDouble(page.centroids[i]);
            }
        }
        for (Page page : data) {
            for (int i = 0; i < page.active; i++) {
                buf.putInt(page.counts[i]);
            }
        }
    }

    @Override
    public void asSmallBytes(ByteBuffer buf) {
        buf.putInt(SMALL_ARRAY_DIGEST);
        buf.putDouble(compression());
        buf.putInt(pageSize);
        buf.putInt(centroidCount);

        double x = 0;
        for (Page page : data) {
            for (int i = 0; i < page.active; i++) {
                double mean = page.centroids[i];
                double delta = mean - x;
                x = mean;
                buf.putFloat((float) delta);
            }
        }
        for (Page page : data) {
            for (int i = 0; i < page.active; i++) {
                int n = page.counts[i];
                encode(buf, n);
            }
        }
    }

    /**
     * Reads a histogram from a byte buffer
     *
     * @return The new histogram structure
     */
    public static ArrayDigest fromBytes(ByteBuffer buf) {
        int encoding = buf.getInt();
        if (encoding == VERBOSE_ENCODING || encoding == VERBOSE_ARRAY_DIGEST) {
            double compression = buf.getDouble();
            int pageSize = 32;
            if (encoding == VERBOSE_ARRAY_DIGEST) {
                pageSize = buf.getInt();
            }
            ArrayDigest r = new ArrayDigest(pageSize, compression);
            int n = buf.getInt();
            double[] means = new double[n];
            for (int i = 0; i < n; i++) {
                means[i] = buf.getDouble();
            }
            for (int i = 0; i < n; i++) {
                r.add(means[i], buf.getInt());
            }
            return r;
        } else if (encoding == SMALL_ENCODING || encoding == SMALL_ARRAY_DIGEST) {
            double compression = buf.getDouble();
            int pageSize = 32;
            if (encoding == SMALL_ARRAY_DIGEST) {
                pageSize = buf.getInt();
            }
            ArrayDigest r = new ArrayDigest(pageSize, compression);
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

    private List<Double> history(Index index) {
        List<List<Double>> h = data.get(index.page).history;
        return h == null ? null : h.get(index.subPage);
    }

    private void delete(Index index) {
        // don't want to delete empty pages here because other indexes would be screwed up.
        // this should almost never happen anyway since deletes only cause small ordering
        // changes
        totalWeight -= count(index);
        centroidCount--;
        data.get(index.page).delete(index.subPage);
    }

    private Iterable<Index> inclusiveTail(final Index start) {
        return new Iterable<Index>() {
            @Override
            public Iterator<Index> iterator() {
                return ArrayDigest.this.iterator(start.page, start.subPage);
            }
        };
    }

    void addRaw(double x, int w) {
        List<Double> tmp = new ArrayList<Double>();
        tmp.add(x);
        addRaw(x, w, recordAllData ? tmp : null);
    }

    void addRaw(double x, int w, List<Double> history) {
        if (centroidCount == 0) {
            Page page = new Page(pageSize, recordAllData);
            page.add(x, w, history);
            totalWeight += w;
            centroidCount++;
            data.add(page);
        } else {
            for (int i = 1; i < data.size(); i++) {
                if (data.get(i).centroids[0] > x) {
                    Page newPage = data.get(i - 1).add(x, w, history);
                    totalWeight += w;
                    centroidCount++;
                    if (newPage != null) {
                        data.add(i, newPage);
                    }
                    return;
                }
            }
            Page newPage = data.get(data.size() - 1).add(x, w, history);
            totalWeight += w;
            centroidCount++;
            if (newPage != null) {
                data.add(data.size(), newPage);
            }
        }
    }

    @Override
    protected void add(double x, int w, Centroid base) {
        addRaw(x, w, base.data());
    }

    private Iterator<Index> iterator(final int startPage, final int startSubPage) {
        return new Iterator<Index>() {
            int page = startPage;
            int subPage = startSubPage;
            Index end = new Index(-1, -1);
            Index next = null;

            @Override
            public boolean hasNext() {
                if (next == null) {
                    next = computeNext();
                }
                return next != end;
            }

            @Override
            public Index next() {
                if (hasNext()) {
                    Index r = next;
                    next = null;
                    return r;
                } else {
                    throw new NoSuchElementException("Can't iterate past end of data");
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Default operation");
            }

            protected Index computeNext() {
                if (page >= data.size()) {
                    return end;
                } else {
                    Page current = data.get(page);
                    if (subPage >= current.active) {
                        subPage = 0;
                        page++;
                        return computeNext();
                    } else {
                        Index r = new Index(page, subPage);
                        subPage++;
                        return r;
                    }
                }
            }
        };
    }

    private Iterator<Index> reverse(final int startPage, final int startSubPage) {
        return new Iterator<Index>() {
            int page = startPage;
            int subPage = startSubPage;

            Index end = new Index(-1, -1);
            Index next = null;

            @Override
            public boolean hasNext() {
                if (next == null) {
                    next = computeNext();
                }
                return next != end;
            }

            @Override
            public Index next() {
                if (hasNext()) {
                    Index r = next;
                    next = null;
                    return r;
                } else {
                    throw new NoSuchElementException("Can't reverse iterate before beginning of data");
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Default operation");
            }

            protected Index computeNext() {
                if (page < 0) {
                    return end;
                } else {
                    if (subPage < 0) {
                        page--;
                        if (page >= 0) {
                            subPage = data.get(page).active - 1;
                        }
                        return computeNext();
                    } else {
                        Index r = new Index(page, subPage);
                        subPage--;
                        return r;
                    }
                }
            }
        };
    }

    public final static int VERBOSE_ENCODING = 1;
    public final static int SMALL_ENCODING = 2;
    public final static int VERBOSE_ARRAY_DIGEST = 3;
    public final static int SMALL_ARRAY_DIGEST = 4;

    class Index {
        final int page, subPage;

        private Index(int page, int subPage) {
            this.page = page;
            this.subPage = subPage;
        }

        double mean() {
            return data.get(page).centroids[subPage];
        }

        int count() {
            return data.get(page).counts[subPage];
        }
    }

    private static class Page {
        private final boolean recordAllData;
        private final int pageSize;

        int totalCount;
        int active;
        double[] centroids;
        int[] counts;
        List<List<Double>> history;

        private Page(int pageSize, boolean recordAllData) {
            this.pageSize = pageSize;
            this.recordAllData = recordAllData;
            centroids = new double[this.pageSize];
            counts = new int[this.pageSize];
            history = this.recordAllData ? new ArrayList<List<Double>>() : null;
        }

        public Page add(double x, int w, List<Double> history) {
            for (int i = 0; i < active; i++) {
                if (centroids[i] >= x) {
                    // insert at i
                    if (active >= pageSize) {
                        // split page
                        Page newPage = split();
                        if (i < pageSize / 2) {
                            addAt(i, x, w, history);
                        } else {
                            newPage.addAt(i - pageSize / 2, x, w, history);
                        }
                        return newPage;
                    } else {
                        addAt(i, x, w, history);
                        return null;
                    }
                }
            }

            // insert at end
            if (active >= pageSize) {
                // split page
                Page newPage = split();
                newPage.addAt(pageSize / 2, x, w, history);
                return newPage;
            } else {
                addAt(active, x, w, history);
                return null;
            }
        }

        private void addAt(int i, double x, int w, List<Double> history) {
            if (i < active) {
                // shift data to make room
                System.arraycopy(centroids, i, centroids, i + 1, active - i);
                System.arraycopy(counts, i, counts, i + 1, active - i);
                if (this.history != null) {
                    this.history.add(i, history);
                }
                centroids[i] = x;
                counts[i] = w;
            } else {
                centroids[active] = x;
                counts[active] = w;
                if (this.history != null) {
                    this.history.add(history);
                }
            }
            active++;
            totalCount += w;
        }

        private Page split() {
            assert active == pageSize;
            final int half = pageSize / 2;
            Page newPage = new Page(pageSize, recordAllData);
            System.arraycopy(centroids, half, newPage.centroids, 0, pageSize - half);
            System.arraycopy(counts, half, newPage.counts, 0, pageSize - half);
            if (history != null) {
                newPage.history = new ArrayList<List<Double>>();
                newPage.history.addAll(history.subList(half, pageSize));

                List<List<Double>> tmp = new ArrayList<List<Double>>();
                tmp.addAll(history.subList(0, half));
                history = tmp;
            }
            active = half;
            newPage.active = pageSize - half;

            newPage.totalCount = totalCount;
            totalCount = 0;
            for (int i = 0; i < half; i++) {
                totalCount += counts[i];
                newPage.totalCount -= counts[i];
            }

            return newPage;
        }

        public void delete(int i) {
            int w = counts[i];
            if (i != active - 1) {
                System.arraycopy(centroids, i + 1, centroids, i, active - i - 1);
                System.arraycopy(counts, i + 1, counts, i, active - i - 1);
                if (history != null) {
                    history.remove(i);
                }
            }
            active--;
            totalCount -= w;
        }
    }
}
