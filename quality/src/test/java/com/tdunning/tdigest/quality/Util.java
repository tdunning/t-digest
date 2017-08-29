package com.tdunning.tdigest.quality;

import com.tdunning.math.stats.AVLTreeDigest;
import com.tdunning.math.stats.MergingDigest;
import com.tdunning.math.stats.TDigest;
import org.apache.mahout.math.jet.random.AbstractContinousDistribution;
import org.apache.mahout.math.jet.random.Gamma;
import org.apache.mahout.math.jet.random.Uniform;

import java.util.List;
import java.util.Random;

/**
 * Handy routings for computing cdf and quantile from a list of numbers
 */
public class Util {
    public static double cdf(final double x, double[] data) {
        int n1 = 0;
        int n2 = 0;
        for (Double v : data) {
            n1 += (v < x) ? 1 : 0;
            n2 += (v <= x) ? 1 : 0;
        }
        return (n1 + n2) / 2.0 / data.length;
    }

    public static double quantile(final double q, List<Double> data) {
        if (data.size() == 0) {
            return Double.NaN;
        }
        if (q == 1 || data.size() == 1) {
            return data.get(data.size() - 1);
        }
        double index = q * data.size();
        if (index < 0.5) {
            return data.get(0);
        } else if (data.size() - index < 0.5) {
            return data.get(data.size() - 1);
        } else {
            index -= 0.5;
            final int intIndex = (int) index;
            return data.get(intIndex + 1) * (index - intIndex) + data.get(intIndex) * (intIndex + 1 - index);
        }
    }

    enum Factory {
        MERGE {
            TDigest create(double compression) {
                return new MergingDigest(compression, (int) (20 * compression));
            }
        },

        TREE {
            TDigest create(double compression) {
                return new AVLTreeDigest(compression);
            }
        };

        abstract TDigest create(double compression);
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
