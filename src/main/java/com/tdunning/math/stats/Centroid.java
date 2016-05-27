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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A single centroid which represents a number of data points.
 */
public class Centroid implements Comparable<Centroid>, Serializable {
    private static final AtomicInteger uniqueCount = new AtomicInteger(1);

    // Generate a random base for the ID. This will (with high probability) prevent collisions
    // in the event of Centroids from two different JVM instances being merged (e.g. in a
    // distributed system or across serialization/deserialization).
    private static final long ID_BASE = ((long) new java.util.Random().nextInt()) << 32;

    private double centroid = 0;
    private int count = 0;
    private long id;

    private List<Double> actualData = null;

    Centroid(boolean record) {
        id = ID_BASE + uniqueCount.getAndIncrement();
        if (record) {
            actualData = new ArrayList<Double>();
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

    Centroid(double x, int w, List<Double> data) {
        this(x, w);
        actualData = data;
    }

    private void start(double x, int w, int id) {
        this.id = ID_BASE + id;
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
        return ((int) (id & 0xFFFFFFFFL));
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
        return id();
    }

    @Override
    public int compareTo(Centroid o) {
        int r = Double.compare(centroid, o.centroid);
        if (r == 0) {
            long idDiff = id - o.id;
            if (idDiff < 0L) {
                r = -1;
            } else if (idDiff > 0L) {
                r = 1;
            } else {
                r = 0;
            }
        }
        return r;
    }

    public List<Double> data() {
        return actualData;
    }

    public void insertData(double x) {
        if (actualData == null) {
            actualData = new ArrayList<Double>();
        }
        actualData.add(x);
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
        centroid = AbstractTDigest.weightedAverage(centroid, count, x, w);
        count += w;
    }
}
