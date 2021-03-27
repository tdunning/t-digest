package com.tdunning.tdigest;

import com.tdunning.math.stats.Interpolator;
import org.apache.mahout.math.*;
import org.apache.mahout.math.function.Functions;
import org.apache.mahout.math.function.VectorFunction;
import org.apache.mahout.math.jet.random.Exponential;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * This set of tests explores how quantiles should be interpolated between centroids
 * with very low weights. The ideal interpolation is surprisingly (to me) non-uniform.
 * <p>
 * The reason for this is that the centroid mean and adjacent centroid gives more
 * information about the location of the samples in the mean even if we have no
 * prior information about the distribution. Assume, for instance, that we have
 * adjacent centroids with weight 1 and 2. We know that there is one sample exactly
 * at the first centroid because it has unit weight and we know that there is a second
 * sample somewhere between the first and second centroid. But where? It turns out
 * that the distance between the centroids gives us some information about the
 * average spacing between samples in this small area. That means that the location
 * distribution of this second sample given the means and weights of the centroids
 * is not uniform, but is biased away from the first centroid.
 * <p>
 * Analyzing this rigorously is complex, but is easy enough to do using numerical
 * experiments. Further, we only need to model a few situations because the distribution
 * of samples between centroids becomes as good as uniform as soon as the centroids
 * have more than a handful of samples each.
 */
public class SmallCountInterpolationTest {
    /**
     * Computes an approximation of the cumulative distribution between two centroids
     * whose means are assumed to be at 0 and 1.
     *
     * @param w1          Number of samples in left centroid
     * @param w2          Number of samples in right centroid
     * @param x           Sorted points, normally in [0,1], where we should sample the CDF
     * @param sampleCount Number of times to sample the points that make up the centroids
     * @return The CDF of the average distribution of points to the left of specified values of x
     */
    double[] resample(int w1, int w2, double[] x, int sampleCount) {
        // samples are Poisson distributed with unit rate
        Matrix samples = new DenseMatrix(sampleCount, w1 + w2);
        samples.assign(new Exponential(1, new Random()));
        for (int i = 0; i < sampleCount; i++) {
            double sum = 0;
            for (int j = 0; j < w1 + w2; j++) {
                double z = samples.get(i, j);
                samples.set(i, j, sum);
                sum += z;
            }
        }

        // now we need to normalize the left centroid to zero and the right centroid to 1
        VectorFunction vSum = new VectorFunction() {
            @Override
            public double apply(Vector vector) {
                return vector.zSum();
            }
        };
        Vector leftMean = samples.viewPart(0, sampleCount, 0, w1)
                .aggregateRows(vSum)
                .assign(Functions.div(w1));
        Vector rightMean = samples.viewPart(0, sampleCount, w1, w2)
                .aggregateRows(vSum)
                .assign(Functions.div(w2));
        for (int i = 0; i < w1 + w2; i++) {
            samples.viewColumn(i)
                    // offset everything to set leftMean to zero
                    .assign(leftMean, Functions.MINUS)
                    // and scale everything to set rightMean to 1
                    .assign(rightMean.minus(leftMean), Functions.DIV);
        }

        // verify the normalization
        double leftDeviation = samples.viewPart(0, sampleCount, 0, w1)
                .aggregateRows(vSum)
                .assign(Functions.div(w1))
                .aggregate(Functions.MAX, Functions.ABS);
        assert leftDeviation < 1e-6;

        double rightDeviation = samples.viewPart(0, sampleCount, w1, w2)
                .aggregateRows(vSum)
                .assign(Functions.div(w2))
                .aggregate(Functions.MAX, Functions.chain(Functions.ABS, Functions.minus(1)));
        assert rightDeviation < 1e-6;

        double separation = samples
                .viewColumn(w1)
                .minus(samples.viewColumn(w1 - 1))
                .aggregate(Functions.MIN, Functions.IDENTITY);
        assert separation > 0;

        if (w1 > 1) {
            assert samples.viewColumn(0).maxValue() < 0;
            assert samples.viewColumn(w1 - 1).minValue() > 0;
        } else {
            assert samples.viewColumn(0).aggregate(Functions.MAX, Functions.ABS) == 0;
        }

        if (w2 > 1) {
            assert samples.viewColumn(w1).maxValue() < 1;
            assert samples.viewColumn(w1 + w2 - 1).minValue() > 1;
        }

        // add up the cumulative number of points for each desired location
        // this should average out to be w1/2 at 0 and w1 + w2/2 at 1
        // unless w1 == 1
        double[] r = new double[x.length];
        for (int i = 0; i < sampleCount; i++) {
            int xIndex = 0;
            for (int j = 0; j < w1 + w2; j++) {
                double z = samples.get(i, j);
                while (xIndex < r.length && x[xIndex] < z) {
                    r[xIndex] += j;
                    xIndex++;
                }
                while (xIndex < r.length && x[xIndex] == z) {
                    r[xIndex] += j + 0.5;
                    xIndex++;
                }
            }
            while (xIndex < r.length) {
                r[xIndex] += w1 + w2;
                xIndex++;
            }
        }
        // convert to means
        for (int k = 0; k < r.length; k++) {
            r[k] = r[k] / ((w1 + w2) * (double) sampleCount);
        }
        return r;
    }

    /**
     * Returns an array of evenly spaced values with a specified length.
     *
     * @param low   The first value
     * @param high  The last value
     * @param steps The number of equal steps to be taken
     * @return A newly allocated array of values
     */
    double[] sequence(double low, double high, int steps) {
        assert steps > 1;
        double dx = (high - low) / (steps - 1);
        return sequence(low, high, dx);
    }

    double[] sequence(double low, double high, double by) {
        assert high > low;
        assert by > 0;
        int steps = (int) Math.rint((high - low) / by) + 1;
        double[] r = new double[steps];
        double x = low;
        for (int i = 0; i < steps; i++) {
            r[i] = x;
            x += by;
        }
        // sledge hammer any round-off issues
        r[r.length - 1] = high;
        return r;
    }

    @Test
    public void testUnitWeight() {
        // two singletons must be exactly at the centroid values
        double[] r = resample(1, 1, new double[]{-1e-8, 0, 1e-8, 0.5, 1 - 1e-8, 1, 1 + 1e-8}, 8);
        assertEquals(0, r[0], 0);
        assertEquals(0.25, r[1], 0);
        assertEquals(0.5, r[2], 0);
        assertEquals(0.5, r[3], 0);
        assertEquals(0.5, r[4], 0);
        assertEquals(0.75, r[5], 0);
        assertEquals(1.0, r[6], 0);
    }

    @Test
    public void testEndPoints() {
        // verify that half of the left and right centroids respectively are at the left and right
        // end points respectively
        for (int w1 : new int[]{1, 2, 3, 5, 10}) {
            for (int w2 : new int[]{1, 2, 3, 5, 10, 20, 30}) {
                double[] r = resample(w1, w2, new double[]{0, 1}, 10000);
                assertEquals(w1 / 2.0 / (w1 + w2), r[0], 0.02);
                assertEquals((w1 + w2 / 2.0) / (w1 + w2), r[1], 0.02);
            }
        }
    }

    @Test
    public void generateApproximations() throws FileNotFoundException {
        // these are the cut-points for the piece-wise linear approximations
        // for interpolating from sample domain to quantiles or reverse
        double[] xCuts = {0, 0.05, 0.1, 0.2, 0.3, 0.5, 0.7, 0.8, 0.9};



        // we write out two files. One is a summary of how things work, the other has the
        // coefficients for the approximations themselves. This coefficients file gets moved
        // into the source code itself when we are happy with it to be read on startup
        try (PrintStream raw = new PrintStream("small-count-correction.csv");
             PrintStream polys = new PrintStream("small-count-coefficients.csv")) {
            raw.println("w1,w2,x,y,dy,est");
            polys.println("# This file contains information for interpolating the distribution");
            polys.println("# of mass between two centroids. It is used by the class Interpolator.");
            polys.print("w1,w2,dir,c0");
            for (int i = 0; i < xCuts.length; i++) {
                polys.printf(",c%d", i + 1);
            }
            polys.println();

            // this first line of data is how we communicate the cut-points.
            polys.printf("0,0,cuts,0");
            for (double xCut : xCuts) {
                polys.printf(",%.4f", xCut);
            }
            polys.println();
            for (int w1 = 1; w1 < 10; w1++) {
                for (int w2 = Math.max(2, w1); w2 < 20; w2++) {
                    double[] x = sequence(0.001, 0.999, 101);
                    double[] r = resample(w1, w2, x, 100000);

                    // normalize r to be from mid-point to mid-point (except for singleton on left)
                    double y0 = w1 > 1 ? (double) w1 / 2.0 / (w1 + w2) : (double) w1 / (w1 + w2);
                    double y1 = (w1 + w2 / 2.0) / (w1 + w2);
                    for (int i = 0; i < r.length; i++) {
                        r[i] = (r[i] - y0) / (y1 - y0);
                    }

                    Vector rx = piecewiseApproximate(xCuts, x, r);
                    polys.printf("%d,%d,cdf", w1, w2);
                    for (int i = 0; i < rx.size(); i++) {
                        polys.printf(",%.7f", rx.get(i));
                    }
                    polys.println();

                    double[] r0 = Arrays.copyOf(r, r.length);
                    Vector rev = piecewiseApproximate(xCuts, r0, x);
                    polys.printf("%d,%d,quantile", w1, w2);
                    for (int i = 0; i < rx.size(); i++) {
                        polys.printf(",%.7f", rev.get(i));
                    }
                    polys.println();

                    double gain = 0;
                    Vector v = new DenseVector(xCuts.length + 1);
                    for (int i = 0; i < r.length; i++) {
                        if (x[i] <= 0) {
                            continue;
                        }
                        double yLinear = y0 * (1 - x[i]) + y1 * x[i];
                        double dy = r[i] - yLinear;
                        gain = Math.max(gain, Math.abs(dy));
                        setupRow(v, xCuts, x[i]);
                        double est = v.dot(rx);
                        raw.printf("%d,%d,%.7f,%.7f,%.7f,%.7f\n", w1, w2, x[i], r[i], dy, est);
                    }

                    // verify our interpolation works
                    double absError = piecewiseEvaluate(xCuts, rx, x, r);

                    // and do a round trip to verify that the reverse works as well
                    double[] r1 = sequence(0, 1, x.length);
                    double[] x1 = new double[x.length];
                    double[] revx = new double[rev.size()];
                    for (int i = 0; i < rev.size(); i++) {
                        revx[i] = rev.get(i);
                    }
                    for (int i = 0; i < r1.length; i++) {
                        x1[i] = Interpolator.interpolate(xCuts, revx, r1[i]);
                    }
                    double revError = piecewiseEvaluate(xCuts, rx, x1, r1);
                    assertEquals(String.format("Excess error w1=%d, w2=%d", w1, w2), 0, absError, 0.01);
                    assertEquals(String.format("Excess reverse error w1=%d, w2=%d", w1, w2), 0, revError, 0.01);
                    System.out.printf("%d, %d, error = %.5f, gain = %.5f, ratio = %.1f, rev = %.5f\n",
                            w1, w2, absError, gain, gain / absError, revError);

                }
            }
        }
    }

    @Test
    public void testBuiltinCurves() {
        // this verifies basic functionality of the interpolator
        try {
            new Interpolator("xyzzy");
            fail("Should have failed with non-existent resource");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("interpolation"));
            assertTrue(e.getMessage().contains("Can't open"));
        }
        Interpolator ix = new Interpolator();
        for (int w1 = 1; w1 < 20; w1++) {
            for (int w2 = Math.max(2, w1); w2 < 20; w2++) {
                double[] x = sequence(0.001, 0.999, 101);
                double[] r = resample(w1, w2, x, 100000);

                // normalize r to be from mid-point to mid-point (except for singleton on left)
                double y0 = w1 > 1 ? (double) w1 / 2.0 / (w1 + w2) : (double) w1 / (w1 + w2);
                double y1 = (w1 + w2 / 2.0) / (w1 + w2);
                for (int i = 0; i < r.length; i++) {
                    r[i] = (r[i] - y0) / (y1 - y0);
                }

                for (int i = 0; i < x.length; i++) {
                    double x1 = ix.quantile(w1, w2, r[i]);
                    double q1 = ix.cdf(w1, w2, x[i]);
                    assertEquals(x[i], x1, 0.02);
                    assertEquals(r[i], q1, 0.02);
                }
            }
        }
    }

    private Vector piecewiseApproximate(double[] cuts, double[] x, double[] r) {
        Matrix a = setupA(cuts, x);
        Matrix b = setupB(r);
        Matrix ata = a.transpose().times(a);
        Matrix atb = a.transpose().times(b);
        QRDecomposition qr = new QRDecomposition(ata);
        return qr.solve(atb).viewColumn(0);
    }

    private double piecewiseEvaluate(double[] cuts, Vector rx, double[] x, double[] r) {
        Matrix a = setupA(cuts, x);
        Matrix b = setupB(r);
        return a.times(rx).minus(b.viewColumn(0)).aggregate(Functions.MAX, Functions.ABS);
    }

    private Matrix setupA(double[] cuts, double[] x) {
        Matrix a = new DenseMatrix(x.length, cuts.length + 1);
        for (int i = 0; i < x.length; i++) {
            setupRow(a.viewRow(i), cuts, x[i]);
        }
        return a;
    }

    private Matrix setupB(double[] r) {
        Matrix b = new DenseMatrix(r.length, 1);
        for (int i = 0; i < r.length; i++) {
            b.set(i, 0, r[i]);
        }
        return b;
    }

    void setupRow(Vector v, double[] cuts, double x) {
        v.set(0, 1);
        for (int i = 0; i < cuts.length; i++) {
            v.set(i + 1, Math.max(0, x - cuts[i]));
        }
    }

}
