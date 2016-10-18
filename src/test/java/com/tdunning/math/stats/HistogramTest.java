package com.tdunning.math.stats;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;

public class HistogramTest {

    @Test
    public void testSort() {
        for (int i = 0; i < 10; ++i) {
            Random r = new Random(i);
            Histogram histogram = new Histogram(false);
            final int length = r.nextInt(10000);
            for (int j = 0; j < length; ++j) {
                histogram.append(r.nextDouble(), 1 + r.nextInt(5), null);
            }
            histogram.sort();
            assertTrue(histogram.sorted());
        }
    }

    @Test
    public void testMerge() {
        for (int i = 0; i < 10; ++i) {
            Random r = new Random(i);
            final int len1 = r.nextInt(10000);
            Histogram h1 = new Histogram(false);
            double current = 0;
            for (int j = 0; j < len1; ++j) {
                current += Double.MIN_VALUE + r.nextDouble();
                h1.append(current, 1 + r.nextInt(5), null);
            }
            final int len2 = r.nextInt(10000);
            Histogram h2 = new Histogram(false);
            current = 0;
            for (int j = 0; j < len2; ++j) {
                current += Double.MIN_VALUE + r.nextDouble();
                h2.append(current, 1 + r.nextInt(5), null);
            }
            Histogram h3 = new Histogram(false);
            Histogram.merge(h1, h2, h3);
            assertTrue(h3.sorted());
            assertEquals(h3.length(), h1.length() + h2.length());
            assertEquals(h3.totalCount(), h1.totalCount() + h2.totalCount());
        }
    }

    @Test
    public void testCompact() {
        for (int i = 0; i < 10; ++i) {
            Random r = new Random(i);
            final int len1 = r.nextInt(10000);
            Histogram h = new Histogram(false);
            double current = 0;
            for (int j = 0; j < len1; ++j) {
                current += Double.MIN_VALUE + r.nextDouble();
                h.append(current, 1 + r.nextInt(5), null);
            }
            for (int j = 0; j < 10; ++j) {
                Histogram h2 = new Histogram(false);
                Histogram.compact(h, h2, r.nextDouble() * 10);
                assertEquals(h2.totalCount(), h.totalCount());
            }
        }
    }
}
