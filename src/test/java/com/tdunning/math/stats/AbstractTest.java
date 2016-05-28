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