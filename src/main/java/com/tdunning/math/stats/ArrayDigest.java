package com.tdunning.math.stats;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Array based implementation of a TDigest.
 * <p/>
 * This implementation is essentially a one-level b-tree in which nodes are collected into
 * pages with up to 32 values.
 */
public class ArrayDigest extends TDigest {
    private final int pageSize;

    private List<Page> data = Lists.newArrayList();
    private int totalWeight = 0;
    private int centroidCount = 0;
    private double compression = 100;

    public ArrayDigest(int pageSize, double compression) {
        Preconditions.checkArgument(pageSize > 3, "Must have page size of 4 or more");
        this.pageSize = pageSize;
        this.compression = compression;
    }

    Index floor(double x) {
        Iterator<Index> r = before(x);
        return r.hasNext() ? r.next() : null;
    }

    private Index ceiling(double x) {
        Iterator<Index> r = after(x);
        return r.hasNext() ? r.next() : null;
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
                        p.history.get(closest.subPage).add(new Point(x, w));
                    }
                    totalWeight += w;
                } else {
                    // if the nearest point was not unique, then we may not be modifying the first copy
                    // which means that ordering can change
                    double center = mean(closest);
                    int weight = count(closest);
                    List<Point> history = history(closest);

                    delete(closest);

                    if (history != null) {
                        history.add(new Point(x, w));
                    }
                    weight += w;
                    center = center + (x - center) / weight;

                    addRaw(center, weight, history);
                }
            }
//            totalWeight += w;

            if (data.size() > 100 * compression) {
                // something such as sequential ordering of data points
                // has caused a pathological expansion of our summary.
                // To fight this, we simply replay the current centroids
                // in random order.

                // this causes us to forget the diagnostic recording of data points
                compress();
            }
        }
    }

    private List<Point> history(Index index) {
        return data.get(index.page).history.get(index.subPage);
    }

    private void delete(Index index) {
        // don't want to delete empty pages here because other indexes would be screwed
        // this should almost never happen anyway since deletes only cause small ordering
        // changes
        totalWeight -= count(index);
        centroidCount--;
        data.get(index.page).delete(index.subPage);
    }

    private int count(Index index) {
        return data.get(index.page).counts[index.subPage];
    }

    int headSum(Index limit) {
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

    double mean(Index index) {
        return data.get(index.page).centroids[index.subPage];
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

    private Iterable<Index> inclusiveTail(final Index start) {
        return new Iterable<Index>() {
            @Override
            public Iterator<Index> iterator() {
                return ArrayDigest.this.iterator(start.page, start.subPage);
            }
        };
    }

    public void addRaw(double x, int w) {
        addRaw(x, w, recordAllData ? new ArrayList<Point>() : null);
    }

    public void addRaw(double x, int w, List<Point> history) {
        if (data.size() == 0) {
            Page page = new Page();
            page.add(x, w, history);
            data.add(page);
            totalWeight += w;
            centroidCount++;
        } else {
            for (int i = 1; i < data.size(); i++) {
                if (data.get(i).centroids[0] > x) {
                    Page newPage = data.get(i - 1).add(x, w, history);
                    if (newPage != null) {
                        data.add(i, newPage);
                    }
                    totalWeight += w;
                    centroidCount++;
                    return;
                }
            }
            Page newPage = data.get(data.size() - 1).add(x, w, history);
            if (newPage != null) {
                data.add(data.size(), newPage);
            }
            totalWeight += w;
            centroidCount++;
        }
    }

    @Override
    void add(double x, int w, Centroid base) {
        throw new UnsupportedOperationException("Default operation");
    }

    @Override
    public void compress() {
        ArrayDigest reduced = new ArrayDigest(pageSize, compression);
        if (recordAllData) {
            reduced.recordAllData();
        }
        List<Index> tmp = Lists.newArrayList(this.iterator(0, 0));
        Collections.shuffle(tmp, gen);
        for (Index index : tmp) {
            reduced.add(mean(index), count(index));
        }

        data = reduced.data;
        centroidCount = reduced.centroidCount;
    }

    @Override
    public void compress(GroupTree other) {
        throw new UnsupportedOperationException("Default operation");
    }

    @Override
    public int size() {
        return totalWeight;
    }

    @Override
    public double cdf(double x) {
        throw new UnsupportedOperationException("Default operation");
    }

    @Override
    public double quantile(double q) {
        throw new UnsupportedOperationException("Default operation");
    }

    @Override
    public int centroidCount() {
        return centroidCount;
    }

    @Override
    public Iterable<? extends Centroid> centroids() {
        return new Iterable<Centroid>() {
            @Override
            public Iterator<Centroid> iterator() {
                return Iterators.transform(ArrayDigest.this.iterator(0, 0),
                        new Function<Index, Centroid>() {
                            @Override
                            public Centroid apply(Index index) {
                                Page current = data.get(index.page);
                                return new Centroid(current.centroids[index.subPage], current.counts[index.subPage]);
                            }
                        });
            }
        };
    }

    public Iterator<Index> after(double x) {
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

    private Iterator<Index> iterator(final int startPage, final int startSubPage) {
        return new AbstractIterator<Index>() {
            int page = startPage;
            int subPage = startSubPage;

            @Override
            protected Index computeNext() {
                if (page >= data.size()) {
                    return endOfData();
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

    /**
     * Returns an iterator which will give each element <= to x in non-increasing order.
     *
     * @param x The upper bound of all returned elements
     * @return An iterator that returns elements in non-increasing order.
     */
    public Iterator<Index> before(double x) {
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

    private Iterator<Index> reverse(final int startPage, final int startSubPage) {
        return new AbstractIterator<Index>() {
            int page = startPage;
            int subPage = startSubPage;

            @Override
            protected Index computeNext() {
                if (page < 0) {
                    return endOfData();
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

    @Override
    public double compression() {
        throw new UnsupportedOperationException("Default operation");
    }

    @Override
    public int byteSize() {
        throw new UnsupportedOperationException("Default operation");
    }

    @Override
    public int smallByteSize() {
        throw new UnsupportedOperationException("Default operation");
    }

    @Override
    public void asBytes(ByteBuffer buf) {
        throw new UnsupportedOperationException("Default operation");
    }

    @Override
    public void asSmallBytes(ByteBuffer buf) {
        throw new UnsupportedOperationException("Default operation");
    }

    class Index {
        final int page, subPage;

        private Index(int page, int subPage) {
            this.page = page;
            this.subPage = subPage;
        }
    }

    private static class Point {
        final double x;
        final int w;

        public Point(double x, int w) {
            this.x = x;
            this.w = w;
        }
    }

    private class Page {
        int totalCount;
        int active;
        double[] centroids = new double[pageSize];
        int[] counts = new int[pageSize];
        List<List<Point>> history = recordAllData ? new ArrayList<List<Point>>() : null;

        public Page add(double x, int w, List<Point> history) {
            for (int i = 0; i < pageSize; i++) {
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

        private void addAt(int i, double x, int w, List<Point> history) {
            if (i < active) {
                // shift data to make room
                System.arraycopy(centroids, i, centroids, i + 1, active - i);
                System.arraycopy(counts, i, counts, i + 1, active - i);
                if (this.history != null) {
                    this.history.add(i, history);
                }
                centroids[i] = x;
                counts[i] = w;
                active++;
                totalCount += w;
            } else {
                centroids[active] = x;
                counts[active] = w;
                if (this.history != null) {
                    this.history.add(history);
                }
                active++;
                totalCount += w;
            }
        }

        private Page split() {
            Page newPage = new Page();
            System.arraycopy(centroids, 16, newPage.centroids, 0, pageSize / 2);
            System.arraycopy(counts, 16, newPage.counts, 0, pageSize / 2);
            if (history != null) {
                newPage.history = Lists.newArrayList(history.subList(pageSize / 2, pageSize));
                history = Lists.newArrayList(history.subList(0, pageSize / 2));
            }
            active = 16;
            newPage.active = 16;

            newPage.totalCount = totalCount;
            totalCount = 0;
            for (int i = 0; i < 16; i++) {
                totalCount += counts[i];
                newPage.totalCount -= counts[i];
            }

            return newPage;
        }

        public void delete(int i) {
            int w = counts[i];
            if (i != active - 1) {
                System.arraycopy(centroids, i + 1, centroids, i, active - i);
                System.arraycopy(counts, i + 1, counts, i, active - i);
                history.remove(i);
            }
            active--;
            totalCount -= w;
        }
    }
}
