package com.tdunning.tdigest.quality;

import com.tdunning.math.stats.AVLTreeDigest;
import com.tdunning.math.stats.MergingDigest;
import com.tdunning.math.stats.TDigest;
import org.apache.mahout.math.jet.random.AbstractContinousDistribution;
import org.apache.mahout.math.jet.random.Gamma;
import org.apache.mahout.math.jet.random.Uniform;

import java.io.*;
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
            n2 += (v == x) ? 1 : 0;
        }
        return (n1 + n2 / 2.0) / data.length;
    }

    public static double quantile(final double q, double[] data) {
        int n = data.length;
        if (n == 0) {
            return Double.NaN;
        }
        if (q == 1 || n == 1) {
            return data[n - 1];
        }
        double index = q * n;
        if (index < 0.5) {
            return data[0];
        } else if (n - index < 0.5) {
            return data[n - 1];
        } else {
            index -= 0.5;
            final int intIndex = (int) index;
            return data[intIndex + 1] * (index - intIndex) + data[intIndex] * (intIndex + 1 - index);
        }
    }

    public static boolean isGitClean() {
        try {
            return new ProcessBuilder("git", "diff-index", "--quiet", "HEAD", "--")
                    .redirectOutput(new File("/dev/null"))
                    .start()
                    .exitValue() == 0;
        } catch (IOException e) {
            return false;
        }
    }

    public static String getHash(boolean force) throws IOException {
        if (force || isGitClean()) {
            Process p = new ProcessBuilder("git", "log", "-1")
                    .start();
            BufferedReader stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
            // output should look like "commit 01ea144ca865361be6786fd502bb554c75105e3c"
            return stdout.readLine().substring(7);
        } else {
            throw new IOException("Source directory has changes that need to be committed");
        }
    }

    enum Factory {
        MERGE {
            TDigest create(double compression) {
                return new MergingDigest(compression, (int) (10 * compression));
            }

            TDigest create(double compression, int bufferSize) {
                return new MergingDigest(compression, bufferSize);
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
