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

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Collection;

/**
 * Adaptive histogram based on something like streaming k-means crossed with Q-digest.
 *
 * The special characteristics of this algorithm are:
 *
 * - smaller summaries than Q-digest
 *
 * - works on doubles as well as integers.
 *
 * - provides part per million accuracy for extreme quantiles and typically <1000 ppm accuracy for middle quantiles
 *
 * - fast
 *
 * - simple
 *
 * - test coverage roughly at 90%
 *
 * - easy to adapt for use with map-reduce
 */
public abstract class TDigest implements Serializable {
    /**
     * Creates an ArrayDigest with default page size.
     *
     * @param compression The compression parameter.  100 is a common value for normal uses.  1000 is extremely large.
     *                    The number of centroids retained will be a smallish (usually less than 10) multiple of this number.
     * @return the ArrayDigest
     */
    public static ArrayDigest createArrayDigest(double compression) {
        return new ArrayDigest(32, compression);
    }

    /**
     * Creates an ArrayDigest with specified page size.
     *
     * @param pageSize    The internal page size to use.  This should be about sqrt(10*compression)
     * @param compression The compression parameter.  100 is a common value for normal uses.  1000 is extremely large.
     *                    The number of centroids retained will be a smallish (usually less than 10) multiple of this number.
     * @return the ArrayDigest
     */
    public static ArrayDigest createArrayDigest(int pageSize, double compression) {
        return new ArrayDigest(pageSize, compression);
    }

    /**
     * Creates a TreeDigest.  Going forward, AVLTreeDigest should be preferred to the TreeDigest since they are
     * uniformly faster and require less memory while producing nearly identical results.
     *
     * @param compression The compression parameter.  100 is a common value for normal uses.  1000 is extremely large.
     *                    The number of centroids retained will be a smallish (usually less than 10) multiple of this number.
     * @return the TreeDigest
     */
    public static TDigest createTreeDigest(double compression) {
        return new TreeDigest(compression);
    }

    /**
     * Creates an AVLTreeDigest.  AVLTreeDigest is generally the best known implementation right now.
     *
     * @param compression The compression parameter.  100 is a common value for normal uses.  1000 is extremely large.
     *                    The number of centroids retained will be a smallish (usually less than 10) multiple of this number.
     * @return the TreeDigest
     */
    public static TDigest createAvlTreeDigest(double compression) {
        return new AVLTreeDigest(compression);
    }

    /**
     * Creates a TreeDigest of whichever type is the currently recommended type.  AVLTreeDigest is generally the best
     * known implementation right now.
     *
     * @param compression The compression parameter.  100 is a common value for normal uses.  1000 is extremely large.
     *                    The number of centroids retained will be a smallish (usually less than 10) multiple of this number.
     * @return the TreeDigest
     */
    public static TDigest createDigest(double compression) {
        return createAvlTreeDigest(compression);
    }

    /**
     * Adds a sample to a histogram.
     *
     * @param x The value to add.
     * @param w The weight of this point.
     */
    public abstract void add(double x, int w);

    protected final void checkValue(double x) {
        if (Double.isNaN(x)) {
            throw new IllegalArgumentException("Cannot add NaN");
        }
    }

    /**
     * Re-examines a t-digest to determine whether some centroids are redundant.  If your data are
     * perversely ordered, this may be a good idea.  Even if not, this may save 20% or so in space.
     *
     * The cost is roughly the same as adding as many data points as there are centroids.  This
     * is typically < 10 * compression, but could be as high as 100 * compression.
     *
     * This is a destructive operation that is not thread-safe.
     */
    public abstract void compress();

    /**
     * Returns the number of points that have been added to this TDigest.
     *
     * @return The sum of the weights on all centroids.
     */
    public abstract long size();

    /**
     * Returns the fraction of all points added which are <= x.
     */
    public abstract double cdf(double x);

    /**
     * Returns an estimate of the cutoff such that a specified fraction of the data
     * added to this TDigest would be less than or equal to the cutoff.
     *
     * @param q The desired fraction
     * @return The value x such that cdf(x) == q
     */
    public abstract double quantile(double q);

    /**
     * A {@link Collection} that lets you go through the centroids in ascending order by mean.  Centroids
     * returned will not be re-used, but may or may not share storage with this TDigest.
     *
     * @return The centroids in the form of a Collection.
     */
    public abstract Collection<Centroid> centroids();

    /**
     * Returns the current compression factor.
     *
     * @return The compression factor originally used to set up the TDigest.
     */
    public abstract double compression();

    /**
     * Returns the number of bytes required to encode this TDigest using #asBytes().
     *
     * @return The number of bytes required.
     */
    public abstract int byteSize();

    /**
     * Returns the number of bytes required to encode this TDigest using #asSmallBytes().
     *
     * @return The number of bytes required.
     */
    public abstract int smallByteSize();

    /**
     * Serialize this TDigest into a byte buffer.  Note that the serialization used is
     * very straightforward and is considerably larger than strictly necessary.
     *
     * @param buf The byte buffer into which the TDigest should be serialized.
     */
    public abstract void asBytes(ByteBuffer buf);

    /**
     * Serialize this TDigest into a byte buffer.  Some simple compression is used
     * such as using variable byte representation to store the centroid weights and
     * using delta-encoding on the centroid means so that floats can be reasonably
     * used to store the centroid means.
     *
     * @param buf The byte buffer into which the TDigest should be serialized.
     */
    public abstract void asSmallBytes(ByteBuffer buf);

    /**
     * Tell this TDigest to record the original data as much as possible for test
     * purposes.
     *
     * @return This TDigest so that configurations can be done in fluent style.
     */
    public abstract TDigest recordAllData();

    public abstract boolean isRecording();

    /**
     * Add a sample to this TDigest.
     *
     * @param x The data value to add
     */
    public abstract void add(double x);

    /**
     * Add all of the centroids of another TDigest to this one.
     *
     * @param other The other TDigest
     */
    public abstract void add(TDigest other);
}
