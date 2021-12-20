package com.tdunning.tdigest.quality;

import com.tdunning.math.stats.AVLTreeDigest;
import com.tdunning.math.stats.MergingDigest;
import com.tdunning.math.stats.TDigest;
import org.apache.mahout.math.jet.random.AbstractContinousDistribution;
import org.apache.mahout.math.jet.random.Gamma;
import org.apache.mahout.math.jet.random.Uniform;

import java.io.*;
import java.util.Random;

/**
 * Handy routings for computing cdf and quantile from a list of numbers
 */
class Util {
    enum Factory {
        MERGE {
            TDigest create(double compression) {
                MergingDigest digest = new MergingDigest(compression, (int) (10 * compression));
                digest.useAlternatingSort = true;
                digest.useTwoLevelCompression = true;
                return digest;
            }

            TDigest create(double compression, int bufferSize) {
                MergingDigest digest = new MergingDigest(compression, bufferSize);
                digest.useAlternatingSort = true;
                digest.useTwoLevelCompression = true;
                return digest;
            }
            TDigest create() {
                return create(100);
            }
        },

        MERGE_OLD_STYLE {
            TDigest create(double compression) {
                MergingDigest digest = new MergingDigest(compression, (int) (10 * compression));
                digest.useAlternatingSort = false;
                digest.useTwoLevelCompression = false;
                return digest;
            }

            TDigest create(double compression, int bufferSize) {
                MergingDigest digest = new MergingDigest(compression, bufferSize);
                digest.useAlternatingSort = false;
                digest.useTwoLevelCompression = false;
                return digest;
            }
            TDigest create() {
                return create(100);
            }
        },

        TREE {
            TDigest create(double compression) {
                return new AVLTreeDigest(compression);
            }
            TDigest create() {
                return create(20);
            }
        },

        SEEDED_AVL_TREE {
            TDigest create(double compression) {
                return new AVLTreeDigest(compression, new Random());
            }
            TDigest create() {
                return create(20);
            }
        };

        abstract TDigest create(double compression);
        abstract TDigest create();

        TDigest create(double compression, int bufferSize) {
            return create(compression);
        }
    }

    enum Distribution {
        UNIFORM {
            @Override
            public AbstractContinousDistribution create(Random gen) {
                return new Uniform(0, 1, gen);
            }
        },

        GAMMA {
            @Override
            public AbstractContinousDistribution create(Random gen) {
                return new Gamma(0.1, 0.1, gen);
            }
        };

        public abstract AbstractContinousDistribution create(Random gen);
    }
}
