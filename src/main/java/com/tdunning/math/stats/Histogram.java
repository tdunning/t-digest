package com.tdunning.math.stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

final class Histogram implements Iterable<Centroid> {

    /**
     * Merge sort src1 and src2 into dest.
     */
    public static void merge(Histogram src1, Histogram src2, Histogram dest) {
        assert dest.length() == 0;
        final int len1 = src1.length();
        final int len2 = src2.length();
        int i = 0;
        int j = 0;
        while (i < len1 && j < len2) {
            final double mean1 = src1.mean(i);
            final double mean2 = src2.mean(j);
            if (src1.mean(i) < src2.mean(j)) {
                dest.append(mean1, src1.count(i), src1.data(i));
                i += 1;
            } else {
                dest.append(mean2, src2.count(j), src2.data(j));
                j += 1;
            }
        }
        while (i < len1) {
            dest.append(src1.mean(i), src1.count(i), src1.data(i));
            i += 1;
        }
        while (j < len2) {
            dest.append(src2.mean(j), src2.count(j), src2.data(j));
            j += 1;
        }
    }

    private static double weightedAverage(double d1, int w1, double d2, int w2) {
        assert d1 <= d2;
        final double average = (d1 * w1 + d2 * w2) / (w1 + w2);
        // the below line exists because of floating-point rounding errors, for
        // the compact method we need the order to be maintained!
        return Math.max(d1, Math.min(average, d2));
    }

    /** Compress <code>src</code> into <code>dest</code>. */
    public static void compact(Histogram src, Histogram dest, double compression) {
        // The key here is that this method is guaranteed to maintain order since
        // we only merge adjacent keys and the histogram is sorted
        assert dest.length() == 0;
        assert src.sorted();
        final long totalCount = src.totalCount();
        double currentMean = Double.NaN;
        int currentCount = 0;
        final boolean recordPoints = src.data != null;
        List<Double> currentPoints = null;
        for (int srcIdx = 0, length = src.length(); srcIdx < length; ++srcIdx) {
            final double mean = src.mean(srcIdx);
            final int count = src.count(srcIdx);
            final List<Double> points = src.data(srcIdx);

            if (Double.isNaN(currentMean)) {
                currentMean = mean;
                currentCount = count;
                currentPoints = points;
            } else if (srcIdx == length - 1 || mean - currentMean <= src.mean(srcIdx + 1) - mean) {
                // the current mean is the closest mean, try to merge
                final double q = totalCount == 1 ? 0.5 : (dest.totalCount() + (currentCount + count - 1) / 2.0) / (totalCount - 1);
                final double k = 4 * totalCount * q * (1 - q) / compression;
                if (count + currentCount <= k) {
                    // count is ok => merge
                    currentMean = weightedAverage(currentMean, currentCount, mean, count);
                    currentCount += count;
                    if (recordPoints) {
                        currentPoints.addAll(points);
                    }
                } else {
                    // count is too high => don't merge
                    dest.append(currentMean, currentCount, currentPoints);
                    dest.append(mean, count, points);
                    currentMean = Double.NaN;
                }
            } else {
                // the next mean is closer, flush
                dest.append(currentMean, currentCount, currentPoints);
                currentMean = mean;
                currentCount = count;
                currentPoints = points;
            }
        }
        if (Double.isNaN(currentMean) == false) {
            dest.append(currentMean, currentCount, currentPoints);
        }
    }

    private double[] means;
    private int[] counts;
    private List<Double>[] data;
    private long totalCount;
    private int length;

    Histogram(boolean record) {
        means = new double[8];
        counts = new int[8];
        if (record) {
            data = new List[8];
        } else {
            data = null;
        }
    }

    void append(double centroid, int count, List<Double> points) {
        if (length == means.length) {
            // grow by 1/4
            final int newCapacity = length + (length >>> 2);
            means = Arrays.copyOf(means, newCapacity);
            counts = Arrays.copyOf(counts, newCapacity);
            if (data != null) {
                data = Arrays.copyOf(data, newCapacity);
            }
        }
        means[length] = centroid;
        counts[length] = count;

        if (data != null) {
            if (points == null) {
                if (count == 1) {
                    points = Collections.singletonList(centroid);
                } else {
                    throw new IllegalStateException();
                }
            }
            if (data[length] == null) {
                data[length] = new ArrayList<Double>();
            }
            data[length].addAll(points);
        }

        length += 1;
        totalCount += count;
    }

    long totalCount() {
        return totalCount;
    }

    int length() {
        return length;
    }

    double mean(int i) {
        return means[i];
    }

    List<Double> data(int i) {
        return data == null ? null : data[i];
    }

    int count(int i) {
        return counts[i];
    }

    void reset() {
        length = 0;
        totalCount = 0;
        if (data != null) {
            // release memmory
            Arrays.fill(data, null);
        }
    }

    boolean sorted() {
        for (int i = 0; i < length - 1; ++i) {
            if (means[i] > means[i + 1]) {
                return false;
            }
        }
        return true;
    }

    void sort() {
        quickSort(0, length, ThreadLocalRandom.current());
    }

    private void swap(int i, int j) {
        final double tmpMean = means[i];
        means[i] = means[j];
        means[j] = tmpMean;
        final int tmpCount = counts[i];
        counts[i] = counts[j];
        counts[j] = tmpCount;
        if (data != null) {
            final List<Double> tmpData = data[i];
            data[i] = data[j];
            data[j] = tmpData;
        }
    }

    private void quickSort(int from, int to, Random random) {
        if (to - from <= 1) {
            // sorted by definition
            return;
        }
        final int p = partition(from, to, random);
        quickSort(from, p, random);
        quickSort(p + 1, to, random);
    }

    private int partition(int from, int to, Random random) {
        final int pivotIndex = from + random.nextInt(to - from);
        final double pivotValue = means[pivotIndex];
        swap(pivotIndex, to - 1);
        int p = from;
        for (int i = from; i < to - 1; ++i) {
            if (means[i] < pivotValue) {
                swap(i, p++);
            }
        }
        swap(p, to - 1);
        return p;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder().append("{ ");
        for (int i = 0; i < length(); ++i) {
            s.append(mean(i)).append(":").append(count(i)).append(", ");
        }
        if (length() > 1) {
            s.setLength(s.length() - 2);
        }
        return s.append(" }").toString();
    }

    @Override
    public Iterator<Centroid> iterator() {
        return new Iterator<Centroid>() {

            int i = 0;
            
            @Override
            public boolean hasNext() {
                return i < length;
            }

            @Override
            public Centroid next() {
                final Centroid next = new Centroid(mean(i), count(i));
                final List<Double> data = data(i);
                if (data != null) {
                    for (Double x : data) {
                        next.insertData(x);
                    }
                }
                i += 1;
                return next;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
