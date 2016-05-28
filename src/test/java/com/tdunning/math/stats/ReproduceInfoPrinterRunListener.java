package com.tdunning.math.stats;

import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import com.carrotsearch.randomizedtesting.RandomizedContext;

public final class ReproduceInfoPrinterRunListener extends RunListener {

    private boolean failed = false;

    @Override
    public void testFailure(Failure failure) throws Exception {
        failed = true;
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        if (failed) {
            printReproLine();
        }
        failed = false;
    }

    private void printReproLine() {
        final StringBuilder b = new StringBuilder();
        b.append("NOTE: reproduce with: mvn test -Dtests.seed=").append(RandomizedContext.current().getRunnerSeedAsString());
        if (System.getProperty("runSlowTests") != null) {
            b.append(" -DrunSlowTests=").append(System.getProperty("runSlowTests"));
        }
        b.append(" -Dtests.class=").append(RandomizedContext.current().getTargetClass().getName());
        System.out.println(b.toString());
    }

}