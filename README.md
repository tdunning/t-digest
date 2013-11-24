t-digest
========

A new data structure for accurate on-line accumulation of rank-based statistics such as quantiles
and trimmed means.

The t-digest construction algorithm uses a variant of 1-dimensional k-means clustering to product a
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
