/*
 * Licensed to Ted Dunning under one or more
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
 * Reference implementations for cdf and quantile if we have all data.
 */
public class Dist {
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
        double index = q * n;
        if (index < 0) {
            index = 0;
        }
        if (index > n - 1) {
            index = n - 1;
        }
        return data[(int) Math.floor(index)];
    }
}
