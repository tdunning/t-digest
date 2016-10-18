package com.tdunning.math.stats;

import java.nio.ByteBuffer;
import java.util.AbstractList;
import java.util.Collection;
import java.util.List;


public class BatchTDigest extends AbstractTDigest {

    private final double compression;
    private Histogram buffer = new Histogram(recordAllData);
    private Histogram main = new Histogram(recordAllData);
    private Histogram spare = new Histogram(recordAllData);

    public BatchTDigest(double compression) {
        this.compression = compression;
    }
    
    @Override
    public TDigest recordAllData() {
        if (buffer.length() + main.length() != 0) {
            throw new IllegalStateException("Can only ask to record added data on an empty summary");
        }
        super.recordAllData();
        buffer = new Histogram(recordAllData);
        main = new Histogram(recordAllData);
        spare = new Histogram(recordAllData);
        return this;
    }

    @Override
    void add(double x, int w, Centroid base) {
        if (x != base.mean() || w != base.count()) {
            throw new IllegalArgumentException();
        }
        add(x, w, base.data());
    }

    @Override
    public void add(double x, int w) {
        add(x, w, (List<Double>) null);
    }
    
    private void merge() {
        if (buffer.length() > 0) {
            buffer.sort();
            Histogram.merge(buffer, main, spare);
            buffer.reset();

            main.reset();
            Histogram.compact(spare, main, compression);
            spare.reset();
        }
    }
    
    public void add(double x, int w, List<Double> data) {
        checkValue(x);
        buffer.append(x, w, data);
        if (buffer.length() >= main.length()) {
            merge();

            if (main.length() > 10 * compression) {
                compress();
            }
        }
    }

    @Override
    public void compress() {
        Histogram.compact(main, spare, compression);
        main.reset();
        Histogram.compact(spare, main, compression);
        spare.reset();
    }

    @Override
    public long size() {
        return main.totalCount() + buffer.totalCount();
    }

    @Override
    public Collection<Centroid> centroids() {
        merge();
        return new AbstractList<Centroid>() {
            @Override
            public Centroid get(int index) {
                Centroid centroid = new Centroid(main.mean(index), main.count(index), index);
                final List<Double> data = main.data(index);
                if (data != null) {
                    for (double d : data) {
                        centroid.insertData(d);
                    }
                }
                return centroid;
            }

            @Override
            public int size() {
                return main.length();
            }
        };
    }

    @Override
    public double compression() {
        return compression;
    }

    @Override
    public int byteSize() {
        merge();
        return 4 + 8 + 4 + main.length() * 12;
    }

    @Override
    public int smallByteSize() {
        int bound = byteSize();
        ByteBuffer buf = ByteBuffer.allocate(bound);
        asSmallBytes(buf);
        return buf.position();
    }

    public final static int VERBOSE_ENCODING = 1;
    public final static int SMALL_ENCODING = 2;

    /**
     * Outputs a histogram as bytes using a particularly cheesy encoding.
     */
    @Override
    public void asBytes(ByteBuffer buf) {
        merge();
        buf.putInt(VERBOSE_ENCODING);
        buf.putDouble(compression());
        buf.putInt(centroids().size());
        for (Centroid centroid : centroids()) {
            buf.putDouble(centroid.mean());
        }

        for (Centroid centroid : centroids()) {
            buf.putInt(centroid.count());
        }
    }

    @Override
    public void asSmallBytes(ByteBuffer buf) {
        buf.putInt(SMALL_ENCODING);
        buf.putDouble(compression());
        buf.putInt(centroids().size());

        double x = 0;
        for (Centroid centroid : centroids()) {
            double delta = centroid.mean() - x;
            x = centroid.mean();
            buf.putFloat((float) delta);
        }

        for (Centroid centroid : centroids()) {
            int n = centroid.count();
            encode(buf, n);
        }
    }

    /**
     * Reads a histogram from a byte buffer
     *
     * @return The new histogram structure
     */
    public static BatchTDigest fromBytes(ByteBuffer buf) {
        int encoding = buf.getInt();
        if (encoding == VERBOSE_ENCODING) {
            double compression = buf.getDouble();
            BatchTDigest r = new BatchTDigest(compression);
            int n = buf.getInt();
            double[] means = new double[n];
            for (int i = 0; i < n; i++) {
                means[i] = buf.getDouble();
            }
            for (int i = 0; i < n; i++) {
                r.main.append(means[i], buf.getInt(), null);
            }
            return r;
        } else if (encoding == SMALL_ENCODING) {
            double compression = buf.getDouble();
            BatchTDigest r = new BatchTDigest(compression);
            int n = buf.getInt();
            double[] means = new double[n];
            double x = 0;
            for (int i = 0; i < n; i++) {
                double delta = buf.getFloat();
                x += delta;
                means[i] = x;
            }

            for (int i = 0; i < n; i++) {
                int z = decode(buf);
                r.main.append(means[i], z, null);
            }
            return r;
        } else {
            throw new IllegalStateException("Invalid format for serialized histogram");
        }
    }
}
