package com.tdunning.math.stats;

import java.io.*;
import java.net.URL;
import java.util.Arrays;

/**
 * Supports piece-wise linear interpolation with special juju to refine cases where
 * centroids have small counts.
 */
public class Interpolator {
    double[] cuts;
    double[][] quantile;
    double[][] cdf;

    int stride;

    public Interpolator() {
        this("/small-count-coefficients.csv");
    }

    public Interpolator(String resourceName) {
        readInterpolationData(resourceName);
    }

    /**
     * Interpolates values using a piece-wise linear approximation implemented using
     * rectifying units. Each rectifying unit has a value of max(0, x-cut) the
     * interpolated value is the dot product of the vector (1, ... max(0, x-cut[i-1]) ...)
     * and the coefficients.
     *
     * @param cuts The cut points for the rectifiers. There should be n-1 of these.
     * @param a    The coefficients for combining rectifier outputs. There should be n of these.
     * @param x    The value to be interpolated.
     * @return The interpolated value.
     */
    public static double interpolate(double[] cuts, double[] a, double x) {
        assert a.length == cuts.length + 1;
        double sum = a[0];
        for (int i = 1; i < a.length; i++) {
            sum += a[i] * Math.max(0, x - cuts[i - 1]);
        }
        return sum;
    }

    /**
     * Translates a function value offset from the left centroid into a
     * rank offset from the mid-point of the left centroid. If we have
     * some special knowledge about how to handle the particular weights
     * given, then we will do that. Otherwise, we will just do linear
     * interpolation.
     *
     * @param w1 The mass of the centroid to the left
     * @param w2 The mass of the centroid to the right
     * @param x  The function value minus the mean of the centroid to the left normalized by left to right distance
     * @return The interpolated value of q * totalWeight offset from the mid-point of the left centroid
     */
    public double cdf(double w1, double w2, double x) {
        if (x < 0) {
            return 0;
        }
        if (x > 1) {
            return 1;
        }
        if (w1 == 1 && w2 == 1) {
            if (x < 0.5) {
                return 0;
            } else {
                return 1;
            }
        }

        if (w1 > w2) {
            // we only keep special cases for w1 < w2
            return w1 + w2 - cdf(w2, w1, 1 - x);
        }

        int i1 = (int) w1;
        int i2 = (int) w2;
        int index = (i1 - 1) * stride + (i2 - 1);
        if (w1 < 100 && w2 < 100 && index < quantile.length && quantile[index] != null) {
            // we have some magic info to use
            return interpolate(cuts, cdf[index], x);
        } else {
            // assume linear interpolation
            return x;
        }
    }

    /**
     * Interpolates a denormalized value of q to get a fraction of the
     * distance between centroid means. If we have some special juju
     * about how this interpolation should work for small w1 and w2, we
     * will apply that, but others will interpolated between the two
     * centroids keeping in mind that half the weight of each centroid
     * is outside this interval.
     *
     * @param w1 Mass of left centroid
     * @param w2 Mass of right centroid
     * @param q  Normalized quantile offset from center of left centroid.
     * @return Interpolated fraction of distance from left to right
     */
    public double quantile(double w1, double w2, double q) {
        if (q < 0) {
            return 0;
        } else if (q > (w1 + w2) / 2) {
            return 1;
        }
        if (w1 == 1 && w2 == 1) {
            return 0.5;
        }
        if (w1 > w2) {
            return 1 - quantile(w2, w1, (w1 + w2) / 2 - q);
        } else {
            int i1 = (int) w1;
            int i2 = (int) w2;
            int index = (i1 - 1) * stride + (i2 - 1);
            if (w1 < 100 && w2 < 100 && index < cdf.length && quantile[index] != null) {
                // we have some parameters to use
                return interpolate(cuts, quantile[index], q);
            } else {
                // assume linear interpolation
                return q;
            }
        }
    }

    private synchronized void readInterpolationData(String resourceName) {
        InputStream dataResource = this.getClass().getResourceAsStream(resourceName);
        if (dataResource == null) {
            throw new IllegalStateException("Can't open resource for interpolation curves");
        }
        try (BufferedReader polys = new BufferedReader(new InputStreamReader(dataResource))) {
            // skip headers and comment lines
            String headers = polys.readLine();
            while (headers != null && headers.startsWith("#")) {
                headers = polys.readLine();
            }
            String[] cutValues = polys.readLine().split(",");
            assert "0".equals(cutValues[0]);
            assert "0".equals(cutValues[1]);
            assert "cuts".equals(cutValues[2]);
            assert "0".equals(cutValues[3]);
            cuts = parseValues(cutValues, 4);
            cdf = new double[0][];
            quantile = new double[0][];
            stride = 0;

            String line = polys.readLine();
            while (line != null) {
                String[] parts = line.split(",");
                if (parts.length != cuts.length + 4) {
                    throw new RuntimeException(String.format("Invalid number of fields: %d", parts.length));
                }
                int w1 = Integer.parseInt(parts[0]);
                int w2 = Integer.parseInt(parts[1]);
                String dir = parts[2];
                extend(w1, w2);
                int index = (w1 - 1) * stride + (w2 - 1);
                double[] values = parseValues(parts, 3);
                switch (dir) {
                    case "cdf":
                        cdf[index] = values;
                        break;
                    case "quantile":
                        quantile[index] = values;
                        break;
                    default:
                        throw new RuntimeException(String.format("Invalid parameter code encountered: %s", dir));
                }
                line = polys.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void extend(int w1, int w2) {
        if (w2 > stride) {
            int newStride = w2 + 5;
            double[][] newCdf = new double[w1 * newStride][];
            double[][] newQuantile = new double[w1 * newStride][];
            if (stride > 0) {
                for (int i = 0; i < cdf.length / stride; i++) {
                    for (int j = 0; j < stride; j++) {
                        if (cdf[i * stride + j] != null) {
                            newCdf[i * newStride + j] = cdf[i * stride + j];
                        }
                        if (quantile[i * stride + j] != null) {
                            newQuantile[i * newStride + j] = quantile[i * stride + j];
                        }
                    }
                }
            }
            stride = newStride;
            cdf = newCdf;
            quantile = newQuantile;
        }
        if (w1 * stride > cdf.length) {
            int newLength = (w1 + 5) * stride;
            cdf = Arrays.copyOf(cdf, newLength);
            quantile = Arrays.copyOf(quantile, newLength);
        }
    }

    private double[] parseValues(String[] values, int offset) {
        double[] r = new double[values.length - offset];
        for (int i = 0; i < r.length; i++) {
            r[i] = Double.parseDouble(values[i + offset]);
        }
        return r;
    }
}
