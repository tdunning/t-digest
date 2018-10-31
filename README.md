t-digest
========

A new data structure for accurate on-line accumulation of rank-based statistics such as quantiles
and trimmed means.  The t-digest algorithm is also very parallel friendly making it useful in
map-reduce and parallel streaming applications.

The t-digest construction algorithm uses a variant of 1-dimensional k-means clustering to produce a
data structure that is related to the Q-digest.  This t-digest data structure can be used to estimate
quantiles or compute other rank statistics.  The advantage of the t-digest over the Q-digest is that
the t-digest can handle floating point values while the Q-digest is limited to integers.  With small
changes, the t-digest can handle any values from any ordered set that has something akin to a mean.
The accuracy of quantile estimates produced by t-digests can be orders of magnitude more accurate than
those produced by Q-digests in spite of the fact that t-digests are more compact when stored on disk.

In summary, the particularly interesting characteristics of the t-digest are that it

* has smaller summaries than Q-digest
* works on doubles as well as integers.
* provides part per million accuracy for extreme quantiles and typically <1000 ppm accuracy for middle quantiles
* is fast
* is very simple
* has a reference implementation that has > 90% test coverage
* can be used with map-reduce very easily because digests can be merged
* recent versions are simple, very fast, and require no dynamic allocation after creation

FloatHistogram
--------------

This package also has an implementation of `FloatHistogram` which is another way to look at distributions where all measurements are positive and where you want relative accuracy in the measurement space instead of accuracy defined in quantiles. This `FloatHistogram` makes use of the floating point hardware to implement variable width bins so that adding data is very fast (5ns/data point in benchmarks) and the resulting sketch is small for reasonable accuracy levels. For instance, if you require dynamic range of a million and are OK with about bins being about ±10%, then you only need 80 counters.

Since the bins for `FloatHistogram`'s are static rather than adaptive, they can be combined very easily. Thus you can store a histogram for short periods of time and combined them at query time if you are looking at metrics for your system. You can also reweight histograms to avoid errors due to structured
omission.



Compile and Test
================

You have to have Java 1.7 to compile and run this code.  You will also need maven (3+ preferred)
to compile and test this software.  In order to build the images that go into the theory paper, you will need R.
In order to format the paper, you will need latex.  A pre-built pdf version of the paper is provided.

On Ubuntu, you can get the necessary pre-requisites with the following:

    sudo apt-get install  openjdk-7-jdk git maven

Once you have these installed, use this to build and test the software:

    mvn test

Testing Accuracy and Comparing to Q-digest
================

The normal test suite produces a number of diagnostics that describe the scaling and accuracy characteristics of
t-digests.  In order to produce nice visualizations of these properties, you need to have more samples.  To get
this enhanced view, use this command:

    mvn test -DrunSlowTests=true

This will enable a slow scaling test and extend the number of iterations on a number of other tests.  Threading
is used extensively in these tests and all tests run in parallel so running this on a multi-core machine is
indicated. On an 8-core EC2 instance, these tests take about 20 minutes to complete.

The data from these tests are stored in a variety of data files in the root directly.  Some of these files are
quite large.  To visualize the contents of these files, copy all of them into the t-digest-paper directory so
that they are accessible to the R scripts there:

    cp *.?sv docs/t-digest-paper/

At this point you can run the R analysis scripts:

    cd docs/t-digest-paper/
    for i in *.r; do (R --slave -f $i; echo $i complete) & echo $i started; done

Most of these scripts will complete almost instantaneously; one or two will take a few tens of seconds.

The output of these scripts are a collection of PNG and/or PDF files that can be viewed with any suitable viewer
such as Preview on a Mac.  Many of these images are used as figures in the paper in the same directory with
the R scripts.

Implementations in Other Languages
=================
The t-digest algorithm has been ported to other languages:
 - Python: [tdigest](https://github.com/CamDavidsonPilon/tdigest)
 - Go: [github.com/spenczar/tdigest](https://github.com/spenczar/tdigest) [https://github.com/influxdata/tdigest](https://github.com/influxdata/tdigest)
 - Javascript: [tdigest](https://github.com/welch/tdigest)
 - C++: [CPP TDigest](https://github.com/gpichot/cpp-tdigest), [FB's Folly Implementation (high performance)](https://github.com/facebook/folly/blob/master/folly/stats/QuantileEstimator.h)
 - Scala: need link!
 - C: [tdigestc (w/ bindings to Go, Java, Python, JS via wasm)](https://githb.com/ajwerner/tdigestc)

Continuous Integration
=================

The t-digest project makes use of Travis integration with Github for testing whenever a change is made.

You can see the reports at:

    https://travis-ci.org/tdunning/t-digest

travis update

Installation
===============

The t-Digest library Jars are released via [Maven Central Repository] (http://repo1.maven.org/maven2/com/tdunning/).
The current version is 3.2.

 ```xml
      <dependency>
          <groupId>com.tdunning</groupId>
          <artifactId>t-digest</artifactId>
          <version>3.1</version>
      </dependency>
 ```     
      
