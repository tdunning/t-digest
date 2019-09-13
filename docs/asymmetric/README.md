This directory contains some empirical results of constructing t-digests with asymmetric scale functions,
which may be of particular interest for distributions with substantial skew. Suitable asymmetric scale functions
can be obtained by gluing the familiar k<sub>1</sub>, k<sub>2</sub>, k<sub>3</sub> to their tangent lines at some point p in (0, 1).
A theoretical justification for this construction (namely, the proofs that the modified scale functions produce
mergeable data structures that can operate online), together with these results, will be submitted for publication shortly.

The main goal here is to compare the usual scale functions with their asymmetric counterparts (using the point p=1/2 as a gluing location),
both in terms of accuracy and memory consumption (via number of centroids). A quadratic scale function is considered as well.

The gist of the results is that for each k<sub>i</sub> mentioned above, the asymmetric (glued) variant has the
error profile of k<sub>i</sub> for quantiles greater than 1/2 (i.e., above the median) and that of k<sub>0</sub>
(a linear function producing equal-sized bins) for quantiles on the other side.

The data and summarizing plots can be produced in two steps.

### Generate data

In `TDigestTests.java`, run `writeUniformResultsWithCompression` with the `ALVTreeDigest` implementation, i.e., run
`AVLTreeDigestTest.writeUniformResultsWithCompression`.

In `MergingDigestTest.java`, run `writeAsymmetricScaleFunctionResults`.

These will write data files to subdirectories of the `data` directory of `asymmetric`.
The data so generated is already present.


### Generate plots

Now run the script `generate_plots.py` located in this directory.

This script expects to be present the results of running the tests as above.
It will write plots (as PNG files) to subdirectories of the `plots` directory.
The figures so generated are already present.

