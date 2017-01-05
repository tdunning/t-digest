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
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * A tree of t-digest centroids.
 */
final class AVLGroupTree extends AbstractCollection<Centroid> implements Serializable {

    /* For insertions into the tree */
    private double centroid;
    private int count;
    private List<Double> data;

    private double[] centroids;
    private int[] counts;
    private List<Double>[] datas;
    private long[] aggregatedCounts;
    private final IntAVLTree tree;

    AVLGroupTree() {
        this(false);
    }

    AVLGroupTree(final boolean record) {
        tree = new IntAVLTree() {

            @Override
            protected void resize(int newCapacity) {
                super.resize(newCapacity);
                centroids = Arrays.copyOf(centroids, newCapacity);
                counts = Arrays.copyOf(counts, newCapacity);
                aggregatedCounts = Arrays.copyOf(aggregatedCounts, newCapacity);
                if (datas != null) {
                    datas = Arrays.copyOf(datas, newCapacity);
                }
            }

            @Override
            protected void merge(int node) {
                // two nodes are never considered equal
                throw new UnsupportedOperationException();
            }

            @Override
            protected void copy(int node) {
                centroids[node] = centroid;
                counts[node] = count;
                if (datas != null) {
                    if (data == null) {
                        if (count != 1) {
                            throw new IllegalStateException();
                        }
                        data = new ArrayList<Double>();
                        data.add(centroid);
                    }
                    datas[node] = data;
                }
            }

            @Override
            protected int compare(int node) {
                if (centroid < centroids[node]) {
                    return -1;
                } else {
                    // upon equality, the newly added node is considered greater
                    return 1;
                }
            }

            @Override
            protected void fixAggregates(int node) {
                super.fixAggregates(node);
                aggregatedCounts[node] = counts[node] + aggregatedCounts[left(node)] + aggregatedCounts[right(node)];
            }

        };
        centroids = new double[tree.capacity()];
        counts = new int[tree.capacity()];
        aggregatedCounts = new long[tree.capacity()];
        if (record) {
            @SuppressWarnings("unchecked")
            final List<Double>[] datas = new List[tree.capacity()];
            this.datas = datas;
        }
    }

    /**
     * Return the number of centroids in the tree.
     */
    @Override
    public int size() {
        return tree.size();
    }

    /**
     * Return the previous node.
     */
    public int prev(int node) {
        return tree.prev(node);
    }

    /**
     * Return the next node.
     */
    public int next(int node) {
        return tree.next(node);
    }

    /**
     * Return the mean for the provided node.
     */
    public double mean(int node) {
        return centroids[node];
    }

    /**
     * Return the count for the provided node.
     */
    public int count(int node) {
        return counts[node];
    }

    /**
     * Return the data for the provided node.
     */
    public List<Double> data(int node) {
        return datas == null ? null : datas[node];
    }

    /**
     * Add the provided centroid to the tree.
     */
    public void add(double centroid, int count, List<Double> data) {
        this.centroid = centroid;
        this.count = count;
        this.data = data;
        tree.add();
    }

    @Override
    public boolean add(Centroid centroid) {
        add(centroid.mean(), centroid.count(), centroid.data());
        return true;
    }

    /**
     * Update values associated with a node, readjusting the tree if necessary.
     */
    public void update(int node, double centroid, int count, List<Double> data) {
        this.centroid = centroid;
        this.count = count;
        this.data = data;
        tree.update(node);
    }

    /**
     * Return the last node whose centroid is less than <code>centroid</code>.
     */
    public int floor(double centroid) {
        int floor = IntAVLTree.NIL;
        for (int node = tree.root(); node != IntAVLTree.NIL; ) {
            final int cmp = Double.compare(centroid, mean(node));
            if (cmp <= 0) {
                node = tree.left(node);
            } else {
                floor = node;
                node = tree.right(node);
            }
        }
        return floor;
    }

    /**
     * Return the last node so that the sum of counts of nodes that are before
     * it is less than or equal to <code>sum</code>.
     */
    public int floorSum(long sum) {
        int floor = IntAVLTree.NIL;
        for (int node = tree.root(); node != IntAVLTree.NIL; ) {
            final int left = tree.left(node);
            final long leftCount = aggregatedCounts[left];
            if (leftCount <= sum) {
                floor = node;
                sum -= leftCount + count(node);
                node = tree.right(node);
            } else {
                node = tree.left(node);
            }
        }
        return floor;
    }

    /**
     * Return the least node in the tree.
     */
    public int first() {
        return tree.first(tree.root());
    }

    /**
     * Compute the number of elements and sum of counts for every entry that
     * is strictly before <code>node</code>.
     */
    public long headSum(int node) {
        final int left = tree.left(node);
        long sum = aggregatedCounts[left];
        for (int n = node, p = tree.parent(node); p != IntAVLTree.NIL; n = p, p = tree.parent(n)) {
            if (n == tree.right(p)) {
                final int leftP = tree.left(p);
                sum += counts[p] + aggregatedCounts[leftP];
            }
        }
        return sum;
    }

    @Override
    public Iterator<Centroid> iterator() {
        return iterator(first());
    }

    private Iterator<Centroid> iterator(final int startNode) {
        return new Iterator<Centroid>() {

            int nextNode = startNode;

            @Override
            public boolean hasNext() {
                return nextNode != IntAVLTree.NIL;
            }

            @Override
            public Centroid next() {
                final Centroid next = new Centroid(mean(nextNode), count(nextNode));
                final List<Double> data = data(nextNode);
                if (data != null) {
                    for (Double x : data) {
                        next.insertData(x);
                    }
                }
                nextNode = tree.next(nextNode);
                return next;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Read-only iterator");
            }

        };
    }

    /**
     * Return the total count of points that have been added to the tree.
     */
    public long sum() {
        return aggregatedCounts[tree.root()];
    }

    void checkBalance() {
        tree.checkBalance(tree.root());
    }

    void checkAggregates() {
        checkAggregates(tree.root());
    }

    private void checkAggregates(int node) {
        assert aggregatedCounts[node] == counts[node] + aggregatedCounts[tree.left(node)] + aggregatedCounts[tree.right(node)];
        if (node != IntAVLTree.NIL) {
            checkAggregates(tree.left(node));
            checkAggregates(tree.right(node));
        }
    }

}
