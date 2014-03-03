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

import com.google.common.collect.Lists;
import org.apache.mahout.common.RandomUtils;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Adaptive histogram based on something like streaming k-means crossed with Q-digest.
 * <p/>
 * The special characteristics of this algorithm are:
 * <p/>
 * a) smaller summaries than Q-digest
 * <p/>
 * b) works on doubles as well as integers.
 * <p/>
 * c) provides part per million accuracy for extreme quantiles and typically <1000 ppm accuracy for middle quantiles
 * <p/>
 * d) fast
 * <p/>
 * e) simple
 * <p/>
 * f) test coverage > 90%
 * <p/>
 * g) easy to adapt for use with map-reduce
 */
public abstract class TDigest {
    protected Random gen = RandomUtils.getRandom();
    protected boolean recordAllData = false;

    /**
     * Adds a sample to a histogram.
     *
     * @param x The value to add.
     * @param w The weight of this point.
     */
    public abstract void add(double x, int w);

    abstract void add(double x, int w, Centroid base);

    abstract void compress();

    abstract void compress(GroupTree other);

    abstract int size();

    abstract double cdf(double x);

    abstract double quantile(double q);

    abstract int centroidCount();

    abstract Iterable<? extends Centroid> centroids();

    abstract double compression();

    abstract int byteSize();

    abstract int smallByteSize();

    abstract void asBytes(ByteBuffer buf);

    abstract void asSmallBytes(ByteBuffer buf);

    /**
     * Sets up so that all centroids will record all data assigned to them.  For testing only, really.
     */
    public TDigest recordAllData() {
        recordAllData = true;
        return this;
    }

    public boolean isRecording() {
        return recordAllData;
    }

    /**
     * Adds a sample to a histogram.
     *
     * @param x The value to add.
     */
    public void add(double x) {
        add(x, 1);
    }

    public void add(TreeDigest other) {
        List<Centroid> tmp = Lists.newArrayList(other.centroids());

        Collections.shuffle(tmp, gen);
        for (Centroid centroid : tmp) {
            add(centroid.mean(), centroid.count(), centroid);
        }
    }

    protected Centroid createCentroid(double mean, int id) {
        return new Centroid(mean, id, recordAllData);
    }
}
