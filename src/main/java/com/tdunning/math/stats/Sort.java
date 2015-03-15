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

/**
 * Static sorting methods
 */
public class Sort {
    /**
     * Quick sort using an index array.  On return, the
     * values[order[i]] is in order as i goes 0..values.length
     *
     * @param order  Indexes into values
     * @param values The values to sort.
     */
    public static void sort(int[] order, double[] values) {
        sort(order, values, values.length);
    }

    /**
     * Quick sort using an index array.  On return, the
     * values[order[i]] is in order as i goes 0..values.length
     *
     * @param order  Indexes into values
     * @param values The values to sort.
     * @param n      The number of values to sort
     */
    public static void sort(int[] order, double[] values, int n) {
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        quickSort(order, values, 0, n, 8);
        insertionSort(order, values, n, 8);
    }

    /**
     * Standard quick sort except that sorting is done on an index array rather than the values themselves
     *
     * @param order  The pre-allocated index array
     * @param values The values to sort
     * @param start  The beginning of the values to sort
     * @param end    The value after the last value to sort
     * @param limit  The minimum size to recurse down to.
     */
    private static void quickSort(int[] order, double[] values, int start, int end, int limit) {
        // the while loop implements tail-recursion to avoid excessive stack calls on nasty cases
        while (end - start > limit) {

            // median of three values for the pivot
            int a = start;
            int b = (start + end) / 2;
            int c = end - 1;

            int pivotIndex;
            double pivotValue;
            double va = values[order[a]];
            double vb = values[order[b]];
            double vc = values[order[c]];
            if (va > vb) {
                if (vc > va) {
                    // vc > va > vb
                    pivotIndex = a;
                    pivotValue = va;
                } else {
                    // va > vb, va >= vc
                    if (vc < vb) {
                        // va > vb > vc
                        pivotIndex = b;
                        pivotValue = vb;
                    } else {
                        // va >= vc >= vb
                        pivotIndex = c;
                        pivotValue = vc;
                    }
                }
            } else {
                // vb >= va
                if (vc > vb) {
                    // vc > vb >= va
                    pivotIndex = b;
                    pivotValue = vb;
                } else {
                    // vb >= va, vb >= vc
                    if (vc < va) {
                        // vb >= va > vc
                        pivotIndex = a;
                        pivotValue = va;
                    } else {
                        // vb >= vc >= va
                        pivotIndex = c;
                        pivotValue = vc;
                    }
                }
            }

            // move pivot to beginning of array
            swap(order, start, pivotIndex);

            // we use a three way partition because many duplicate values is an important case

            int low = start + 1;   // low points to first value not known to be equal to pivotValue
            int high = end;        // high points to first value > pivotValue
            int i = low;           // i scans the array
            while (i < high) {
                // invariant:  values[order[k]] == pivotValue for k in [0..low)
                // invariant:  values[order[k]] < pivotValue for k in [low..i)
                // invariant:  values[order[k]] > pivotValue for k in [high..end)
                // in-loop:  i < high
                // in-loop:  low < high
                // in-loop:  i >= low
                double vi = values[order[i]];
                if (vi == pivotValue) {
                    if (low != i) {
                        swap(order, low, i);
                    } else {
                        i++;
                    }
                    low++;
                } else if (vi > pivotValue) {
                    high--;
                    swap(order, i, high);
                } else {
                    // vi < pivotValue
                    i++;
                }
            }
            // invariant:  values[order[k]] == pivotValue for k in [0..low)
            // invariant:  values[order[k]] < pivotValue for k in [low..i)
            // invariant:  values[order[k]] > pivotValue for k in [high..end)
            // assert i == high || low == high therefore, we are done with partition

            // at this point, i==high, from [start,low) are == pivot, [low,high) are < and [high,end) are >
            // we have to move the values equal to the pivot into the middle.  To do this, we swap pivot
            // values into the top end of the [low,high) range stopping when we run out of destinations
            // or when we run out of values to copy
            int from = start;
            int to = high - 1;
            for (i = 0; from < low && to >= low; i++) {
                swap(order, from++, to--);
            }
            if (from == low) {
                // ran out of things to copy.  This means that the the last destination is the boundary
                low = to + 1;
            } else {
                // ran out of places to copy to.  This means that there are uncopied pivots and the
                // boundary is at the beginning of those
                low = from;
            }

//            checkPartition(order, values, pivotValue, start, low, high, end);

            // now recurse, but arrange it so we handle the longer limit by tail recursion
            if (low - start < end - high) {
                quickSort(order, values, start, low, limit);

                // this is really a way to do
                //    quickSort(order, values, high, end, limit);
                start = high;
            } else {
                quickSort(order, values, high, end, limit);
                // this is really a way to do
                //    quickSort(order, values, start, low, limit);
                end = low;
            }
        }
    }

    private static void swap(int[] order, int i, int j) {
        int t = order[i];
        order[i] = order[j];
        order[j] = t;
    }

    /**
     * Check that a partition step was done correctly.  For debugging and testing.
     */
    @SuppressWarnings("UnusedDeclaration")
    public static void checkPartition(int[] order, double[] values, double pivotValue, int start, int low, int high, int end) {
        if (order.length != values.length) {
            throw new IllegalArgumentException("Arguments must be same size");
        }

        if (!(start >= 0 && low >= start && high >= low && end >= high)) {
            throw new IllegalArgumentException(String.format("Invalid indices %d, %d, %d, %d", start, low, high, end));
        }

        for (int i = 0; i < low; i++) {
            double v = values[order[i]];
            if (v >= pivotValue) {
                throw new IllegalArgumentException(String.format("Value greater than pivot at %d", i));
            }
        }

        for (int i = low; i < high; i++) {
            if (values[order[i]] != pivotValue) {
                throw new IllegalArgumentException(String.format("Non-pivot at %d", i));
            }
        }

        for (int i = high; i < end; i++) {
            double v = values[order[i]];
            if (v <= pivotValue) {
                throw new IllegalArgumentException(String.format("Value less than pivot at %d", i));
            }
        }
    }

    /**
     * Limited range insertion sort.  We assume that no element has to move more than limit steps
     * because quick sort has done its thing.
     *
     * @param order  The permutation index
     * @param values The values we are sorting
     * @param limit  The largest amount of disorder
     */
    private static void insertionSort(int[] order, double[] values, int n, int limit) {
        for (int i = 1; i < n; i++) {
            int t = order[i];
            double v = values[order[i]];
            int m = Math.max(i - limit, 0);
            for (int j = i; j >= m; j--) {
                if (j == 0 || values[order[j - 1]] <= v) {
                    if (j < i) {
                        System.arraycopy(order, j, order, j + 1, i - j);
                        order[j] = t;
                    }
                    break;
                }
            }
        }
    }
}
