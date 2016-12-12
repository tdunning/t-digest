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

import org.junit.Ignore;
import org.junit.runner.RunWith;

import com.carrotsearch.randomizedtesting.JUnit3MethodProvider;
import com.carrotsearch.randomizedtesting.JUnit4MethodProvider;
import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.Listeners;
import com.carrotsearch.randomizedtesting.annotations.TestMethodProviders;

@Ignore
@Listeners({
        ReproduceInfoPrinterRunListener.class
})
@TestMethodProviders({
        JUnit3MethodProvider.class, // test names starting with test*
        JUnit4MethodProvider.class  // test methods annotated with @Test
})
@RunWith(value = com.carrotsearch.randomizedtesting.RandomizedRunner.class)
/**
 * Base test case, all other test cases must inherit this one.
 */
public abstract class AbstractTest extends RandomizedTest {

}