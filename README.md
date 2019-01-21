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
* is very fast (~ 140 ns per add)
* is very simple (~ 5000 lines of code total, <1000 for the most advanced implementation alone)
* has a reference implementation that has > 90% test coverage
* can be used with map-reduce very easily because digests can be merged
* recent implementations are simple, yet still very fast, and require no dynamic allocation after initial creation
* has no runtime dependencies

Recent News
-----------
Lots has happened in t-digest lately. The basic gist of all the changes are that the
core algorithms have been made much more rigorous and the associated papers have been 
updated to match the reality of the most advanced implementations. The general areas of improvement
include substantial speedups, a new framework for dealing with scale functions, real proofs of size 
bounds and invariants for all current scale functions, much improved interpolation algorithms, better 
accuracy testing and splitting the entire distribution into parts for the core algorithms, quality 
testing, benchmarking and documentation.

I am working on a 4.0 release that incorporates all of these improvements. The remaining punch list for 
the release is roughly:

* verify all tests are clean and not disabled
* integrate all scale functions into `AVLTreeDigest`
* describe accuracy using the quality suite
* extend benchmarks to include `AVLTreeDigest` as first-class alternative

I am also converging the main paper for submission and will be preparing a more implementation-oriented paper 
intended for submission to the Journal of Statistical Software. Potential co-authors would could accelerate these 
submissions are enouraged to speak up!
 
### Scale Functions
The idea of scale functions is the heart of the t-digest. But things don't quite work the way that
we originally thought. Originally, it was presumed that accuracy should be proportional to the 
square of the size of a cluster. That isn't true in practice. That means that scale functions need
to be much more aggressive about controlling cluster sizes near the tails. We now have 4 scale functions 
supported for the `MergingDigest` to all different trade-offs in terms of accuracy.

These scale functions now have associated proofs that they all preserve the key invariants necessary
to build an accurate digest and that they all give tight bounds on the size of a digest. This means that we 
can get much better tail accuracy than before without losing much in terms of median accuracy. It also means
that insertion into a `MergingDigest` is much faster than before since we
have been able to eliminate all fancy functions like sqrt, log or sin.

The `AVLTreeDigest` does not support any scale functions beyond what it historically has supported. This means 
that digests will be much larger than would be produced by `MergingDigest` for the same compression. Work is 
(slowly) ongoing to support all scale functions in the `AVLTreeDigest`.
 
### Better Interpolation
The better accuracy achieved by the new scale functions partly comes from the fact that the most extreme clusters
near q=0 or q=1 are limited to only a single sample. Handling these singletons well makes a huge difference in the 
accuracy of tail estimates. Handling the transition to non-singletons is also very important.
  
Both cases are handled much better than before.

So far, the better interpolation has been fully integrated and tested in the `MergingDigest` with very good improvements in accuracy, but applying these improvements has exposed bugs in the way that `AVLTreeDigest` handles data with many repeated points.
  
### Two-level Merging
We now have a trick for the `MergingDigest` that uses a higher value of the compression parameter (delta) 
while we are accumulating a t-digest and a lower value when we are about to store or display a t-digest. 
This two-level merging has a small (negative) effect on speed, but a substantial (positive) effect on accuracy 
because clusters are ordered more strongly. This better ordering of clusters means that the effects of the 
improved interpolation are much easier to observe.

Extending this to `AVLTreeDigest` is theoretically possible, but it isn't clear the effect it will have, 
particularly if done before proper scale functions are added to the tree digest.
 
### Repo Reorg
The t-digest repository is now split into different functional areas. This is important because it simplifies
the code used in production by extracting the (slow) code that generates data for accuracy testing, but also 
because it lets us avoid any dependencies on GPL code (notable the jmh benchmarking tools) in the released artifacts.
 
The major areas are
 
 * core - this is where the t-digest and unit tests live
 * docs - the main paper and auxillary proofs live here
 * benchmarks - this is the code that tests the speed of the digest algos
 * quality - this is the code that generates and analyzes accuracy information

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
 - C++: [CPP TDigest](https://github.com/gpichot/cpp-tdigest), [FB's Folly Implementation (high performance)](https://github.com/facebook/folly/blob/master/folly/stats/TDigest.h)
 - Scala: need link!

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
      
