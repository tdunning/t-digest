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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A tree containing TDigest.Centroid.  This adds to the normal NavigableSet the
 * ability to sum up the size of elements to the left of a particular group.
 */
public class GroupTree implements Iterable<Centroid> {
    private long count;
    private int size;
    private int depth;
    private Centroid leaf;
    private GroupTree left, right;

    public GroupTree() {
        count = size = depth = 0;
        leaf = null;
        left = right = null;
    }

    public GroupTree(Centroid leaf) {
        size = depth = 1;
        this.leaf = leaf;
        count = leaf.count();
        left = right = null;
    }

    public GroupTree(GroupTree left, GroupTree right) {
        this.left = left;
        this.right = right;
        count = left.count + right.count;
        size = left.size + right.size;
        rebalance();
        leaf = this.right.first();
    }

    public void add(Centroid centroid) {
        if (size == 0) {
            leaf = centroid;
            depth = 1;
            count = centroid.count();
            size = 1;
            return;
        } else if (size == 1) {
            int order = centroid.compareTo(leaf);
            if (order < 0) {
                left = new GroupTree(centroid);
                right = new GroupTree(leaf);
            } else if (order > 0) {
                left = new GroupTree(leaf);
                right = new GroupTree(centroid);
                leaf = centroid;
            }
        } else if (centroid.compareTo(leaf) < 0) {
            left.add(centroid);
        } else {
            right.add(centroid);
        }
        count += centroid.count();
        size++;
        depth = Math.max(left.depth, right.depth) + 1;

        rebalance();
    }

    /**
     * Modify an existing value in the tree subject to the constraint that the change will not alter the
     * ordering of the tree.
     * @param x         New value to add to Centroid
     * @param count     Weight of new value
     * @param v         The value to modify
     * @param data      The recorded data
     */
    public void move(double x, int count, Centroid v, Iterable<? extends Double> data) {
        if (size <= 0) {
            throw new IllegalStateException("Cannot move element of empty tree");
        }

        if (size == 1) {
            if(leaf != v) {
                throw new IllegalStateException("Cannot move element that is not in tree");
            }
            leaf.add(x, count, data);
        } else if (v.compareTo(leaf) < 0) {
            left.move(x, count, v, data);
        } else {
            right.move(x, count, v, data);
        }
        this.count += count;
    }

    private void rebalance() {
        int l = left.depth();
        int r = right.depth();
        if (l > r + 1) {
            if (left.left.depth() > left.right.depth()) {
                rotate(left.left.left, left.left.right, left.right, right);
            } else {
                rotate(left.left, left.right.left, left.right.right, right);
            }
        } else if (r > l + 1) {
            if (right.left.depth() > right.right.depth()) {
                rotate(left, right.left.left, right.left.right, right.right);
            } else {
                rotate(left, right.left, right.right.left, right.right.right);
            }
        } else {
            depth = Math.max(left.depth(), right.depth()) + 1;
        }
    }

    private void rotate(GroupTree a, GroupTree b, GroupTree c, GroupTree d) {
        left = new GroupTree(a, b);
        right = new GroupTree(c, d);
        count = left.count + right.count;
        size = left.size + right.size;
        depth = Math.max(left.depth(), right.depth()) + 1;
        leaf = right.first();
    }

    private int depth() {
        return depth;
    }

    public int size() {
        return size;
    }

    /**
     * @return the number of items strictly before the current element
     */
    public int headCount(Centroid base) {
        if (size == 0) {
            return 0;
        } else if (left == null) {
            return leaf.compareTo(base) < 0 ? 1 : 0;
        } else {
            if (base.compareTo(leaf) < 0) {
                return left.headCount(base);
            } else {
                return left.size + right.headCount(base);
            }
        }
    }

    /**
     * @return the sum of the size() function for all elements strictly before the current element.
     */
    public long headSum(Centroid base) {
        if (size == 0) {
            return 0;
        } else if (left == null) {
            return leaf.compareTo(base) < 0 ? count : 0;
        } else {
            if (base.compareTo(leaf) <= 0) {
                return left.headSum(base);
            } else {
                return left.count + right.headSum(base);
            }
        }
    }

    /**
     * @return the first Centroid in this set
     */
    public Centroid first() {
        if(size <= 0) {
            throw new IllegalStateException("No first element of empty set");
        }
        if (left == null) {
            return leaf;
        } else {
            return left.first();
        }
    }

    /**
     * Iteratres through all groups in the tree.
     */
    public Iterator<Centroid> iterator() {
        return iterator(null);
    }

    /**
     * Iterates through all of the Groups in this tree in ascending order of means
     * @param start  The place to start this subset.  Remember that Groups are ordered by mean *and* id.
     * @return An iterator that goes through the groups in order of mean and id starting at or after the
     * specified Centroid.
     */
    private Iterator<Centroid> iterator(final Centroid start) {
        return new Iterator<Centroid>() {
            {
                stack = new ArrayDeque<GroupTree>();
                push(GroupTree.this, start);
            }

            Centroid end = new Centroid(0, 0, -1);
            Centroid next = null;

            Deque<GroupTree> stack;

            @Override
            public boolean hasNext() {
                if (next == null) {
                    next = computeNext();
                }
                return next != null && next != end;
            }

            @Override
            public Centroid next() {
                if (hasNext()) {
                    Centroid r = next;
                    next = null;
                    return r;
                } else {
                    throw new NoSuchElementException("Can't iterate past end of data");
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Default operation");
            }

            // recurses down to the leaf that is >= start
            // pending right hand branches on the way are put on the stack
            private void push(GroupTree z, Centroid start) {
                while (z.left != null) {
                    if (start == null || start.compareTo(z.leaf) < 0) {
                        // remember we will have to process the right hand branch later
                        stack.push(z.right);
                        // note that there is no guarantee that z.left has any good data
                        z = z.left;
                    } else {
                        // if the left hand branch doesn't contain start, then no push
                        z = z.right;
                    }
                }
                // put the leaf value on the stack if it is valid
                if (start == null || z.leaf.compareTo(start) >= 0) {
                    stack.push(z);
                }
            }

            protected Centroid computeNext() {
                GroupTree r = stack.poll();
                while (r != null && r.left != null) {
                    // unpack r onto the stack
                    push(r, start);
                    r = stack.poll();
                }

                // at this point, r == null or r.left == null
                // if r == null, stack is empty and we are done
                // if r != null, then r.left != null and we have a result
                if (r != null) {
                    return r.leaf;
                }
                return end;
            }
        };
    }

    public void remove(Centroid base) {
        if(size <= 0) {
            throw new IllegalStateException("Cannot remove from empty set");
        }
        if (size == 1) {
            if(base.compareTo(leaf) != 0) {
                throw new IllegalStateException(String.format("Element %s not found", base));
            }
            count = size = 0;
            leaf = null;
        } else {
            if (base.compareTo(leaf) < 0) {
                if (left.size > 1) {
                    left.remove(base);
                    count -= base.count();
                    size--;
                    rebalance();
                } else {
                    size = right.size;
                    count = right.count;
                    depth = right.depth;
                    leaf = right.leaf;
                    left = right.left;
                    right = right.right;
                }
            } else {
                if (right.size > 1) {
                    right.remove(base);
                    leaf = right.first();
                    count -= base.count();
                    size--;
                    rebalance();
                } else {
                    size = left.size;
                    count = left.count;
                    depth = left.depth;
                    leaf = left.leaf;
                    right = left.right;
                    left = left.left;
                }
            }
        }
    }

    /**
     * @return the largest element less than or equal to base
     */
    public Centroid floor(Centroid base) {
        if (size == 0) {
            return null;
        } else {
            if (size == 1) {
                return base.compareTo(leaf) >= 0 ? leaf : null;
            } else {
                if (base.compareTo(leaf) < 0) {
                    return left.floor(base);
                } else {
                    Centroid floor = right.floor(base);
                    if (floor == null) {
                        floor = left.last();
                    }
                    return floor;
                }
            }
        }
    }

    public Centroid last() {
        if(size <= 0) {
            throw new IllegalStateException("Cannot find last element of empty set");
        }
        if (size == 1) {
            return leaf;
        } else {
            return right.last();
        }
    }

    /**
     * @return the smallest element greater than or equal to base.
     */
    public Centroid ceiling(Centroid base) {
        if (size == 0) {
            return null;
        } else if (size == 1) {
            return base.compareTo(leaf) <= 0 ? leaf : null;
        } else {
            if (base.compareTo(leaf) < 0) {
                Centroid r = left.ceiling(base);
                if (r == null) {
                    r = right.first();
                }
                return r;
            } else {
                return right.ceiling(base);
            }
        }
    }

    /**
     * @return the subset of elements equal to or greater than base.
     */
    public Iterable<Centroid> tailSet(final Centroid start) {
        return new Iterable<Centroid>() {
            @Override
            public Iterator<Centroid> iterator() {
                return GroupTree.this.iterator(start);
            }
        };
    }

    public long sum() {
        return count;
    }

    public void checkBalance() {
        if (left != null) {
            if(Math.abs(left.depth() - right.depth()) >= 2) {
                throw new IllegalStateException("Imbalanced");
            }
            int l = left.depth();
            int r = right.depth();
            if(depth != Math.max(l, r) + 1){
                throw new IllegalStateException( "Depth doesn't match children");
            }
            if(size != left.size + right.size){
                throw new IllegalStateException( "Sizes don't match children");
            }
            if(count != left.count + right.count){
                throw new IllegalStateException( "Counts don't match children");
            }
            if(leaf.compareTo(right.first()) != 0){
                throw new IllegalStateException(String.format( "Split is wrong %.5f != %.5f or %d != %d", leaf.mean(), right.first().mean(), leaf.id(), right.first().id()));
            }
            left.checkBalance();
            right.checkBalance();
        }
    }

    public void print(int depth) {
        for (int i = 0; i < depth; i++) {
            System.out.printf("| ");
        }
        int imbalance = Math.abs((left != null ? left.depth : 1) - (right != null ? right.depth : 1));
        System.out.printf("%s%s, %d, %d, %d\n", (imbalance > 1 ? "* " : "") + (right != null && leaf.compareTo(right.first()) != 0 ? "+ " : ""), leaf, size, count, this.depth);
        if (left != null) {
            left.print(depth + 1);
            right.print(depth + 1);
        }
    }
}
