package com.tdunning.math.stats;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
* A single centroid which represents a number of data points.
*/
public class Centroid implements Comparable<Centroid> {
    private static final AtomicInteger uniqueCount = new AtomicInteger(1);

    private double centroid = 0;
    private int count = 0;
    private int id;

    private List<Double> actualData = null;

    Centroid(boolean record) {
        id = uniqueCount.incrementAndGet();
        if (record) {
            actualData = Lists.newArrayList();
        }
    }

    public Centroid(double x) {
        this(false);
        start(x, 1, uniqueCount.getAndIncrement());
    }

    public Centroid(double x, int w) {
        this(false);
        start(x, w, uniqueCount.getAndIncrement());
    }

    public Centroid(double x, int w, int id) {
        this(false);
        start(x, w, id);
    }

    public Centroid(double x, int id, boolean record) {
        this(record);
        start(x, 1, id);
    }

    public Centroid(double x, int id, int w, boolean record) {
        this(record);
        start(x, 1, id);
        count = w;
    }

    private void start(double x, int w, int id) {
        this.id = id;
        add(x, w);
    }

    public void add(double x, int w) {
        if (actualData != null) {
            actualData.add(x);
        }
        count += w;
        centroid += w * (x - centroid) / count;
    }

    public double mean() {
        return centroid;
    }

    public int count() {
        return count;
    }

    public int id() {
        return id;
    }

    @Override
    public String toString() {
        return "Centroid{" +
                "centroid=" + centroid +
                ", count=" + count +
                '}';
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public int compareTo(Centroid o) {
        int r = Double.compare(centroid, o.centroid);
        if (r == 0) {
            r = id - o.id;
        }
        return r;
    }

    public Iterable<? extends Double> data() {
        return actualData;
    }

    public static Centroid createWeighted(double x, int w, Iterable<? extends Double> data) {
        Centroid r = new Centroid(data != null);
        r.add(x, w, data);
        return r;
    }

    public void add(double x, int w, Iterable<? extends Double> data) {
        if (actualData != null) {
            if (data != null) {
                for (Double old : data) {
                    actualData.add(old);
                }
            } else {
                actualData.add(x);
            }
        }
        count += w;
        centroid += w * (x - centroid) / count;
    }
}
