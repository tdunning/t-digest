Release 3.2
===========
In release 3.2, the goal is to produce an update to the code given the large number of improvements since the previous release.

There are a few bugs that will survive this release, most notably in the AVLTreeDigest. These have to do with large nubmers of repeated data points and are not new bugs.
 
There is a also lot of work going on with serialization. I need to hear from people about  what they are doing with serialization so that we can build some test cases to allow an appropriate migration strategy to future serialization.

The paper continues to be updated. The algorithmic descriptions are getting reasonably clear, but the speed and accuracy sections need a complete revamp with current implementations.


Bugs, fixed and known
----

#### Fixed
The following important issues are fixed in this release

[Issue #90](https://github.com/tdunning/t-digest/issues/90) Serialization for MergingDigest

[Issue #92](https://github.com/tdunning/t-digest/issues/92) Serialization for AVLTreeDigest

#### Maybe fixed
This issue has substantial progress, but lacks a definitive test to determine whether it should be closed.

[Issue 78](https://github.com/tdunning/t-digest/issues/78) Stability under merging.

#### Pushed
The following issues are pushed beyond this release

[Issue #87](https://github.com/tdunning/t-digest/issues/87) Future proof and extensible serialization

[Issue #89](https://github.com/tdunning/t-digest/issues/89) Bad handling for duplicate values in AVLTreeDigest

#### All fixed issues
Here is a complete list of issues resolved in this release:

[Issue #55](https://github.com/tdunning/t-digest/issues/55) Add time
decay to t-digest

[Issue #52](https://github.com/tdunning/t-digest/issues/52) General
factory method for "fromBytes"

[Issue #90](https://github.com/tdunning/t-digest/issues/90)
Deserialization of MergingDigest BuferUnderflowException in 3.1

[Issue #92](https://github.com/tdunning/t-digest/issues/92) Error in
AVLTreeDigest.fromBytes

[Issue #93](https://github.com/tdunning/t-digest/issues/93) high
centroid frequency causes overflow - giving incorrect results

[Issue #67](https://github.com/tdunning/t-digest/issues/67) Release of
version 3.2

[Issue #81](https://github.com/tdunning/t-digest/issues/81)
AVLTreeDigest with a lot of datas : integer overflow

[Issue #75](https://github.com/tdunning/t-digest/issues/75) Adjusting
the centroid threshold values to obtain better accuracy at interesting
values

[Issue #74](https://github.com/tdunning/t-digest/issues/74) underlying
distribution : powerlaw

[Issue #72](https://github.com/tdunning/t-digest/issues/72) Inverse
quantile algorithm is non-contiguous

[Issue #65](https://github.com/tdunning/t-digest/issues/65)
totalDigest add spark dataframe column / array

[Issue #60](https://github.com/tdunning/t-digest/issues/60) Getting
IllegalArgumentException when adding digests

[Issue #53](https://github.com/tdunning/t-digest/issues/53)
smallByteSize methods are very trappy in many classes -- should be
changed or have warnings in javadocs

[Issue #82](https://github.com/tdunning/t-digest/issues/82) TDigest
class does not implement Serializable interface in last release.

[Issue #42](https://github.com/tdunning/t-digest/issues/42) Histogram

[Issue #40](https://github.com/tdunning/t-digest/issues/40) Improved
constraint on centroid sizes

[Issue #37](https://github.com/tdunning/t-digest/issues/37) Allow
arbitrary scaling laws for centroid sizes

[Issue #29](https://github.com/tdunning/t-digest/issues/29) Test
method testScaling() always adds values in ascending order

[Issue #84](https://github.com/tdunning/t-digest/issues/84) Remove
deprecated kinds of t-digest

[Issue #76](https://github.com/tdunning/t-digest/issues/76) Add
serializability

[Issue #77](https://github.com/tdunning/t-digest/issues/77) Question:
Proof of bounds on merging digest size

[Issue #71](https://github.com/tdunning/t-digest/issues/71) Simple
alternate algorithm using maxima, ranks and fixed cumulative weighting

[Issue #61](https://github.com/tdunning/t-digest/issues/61) Possible
improvement to the speed of the algorithm

[Issue #58](https://github.com/tdunning/t-digest/issues/58) jdk8
doclint incompatibility

[Issue #48](https://github.com/tdunning/t-digest/issues/48) Build is
unstable under some circumstances

[Issue #63](https://github.com/tdunning/t-digest/issues/63) Which
TDigest do you recommend?

[Issue #62](https://github.com/tdunning/t-digest/issues/62) Very slow
performance; what am I missing?

[Issue #47](https://github.com/tdunning/t-digest/issues/47) Make
TDigest serializable

[Issue #49](https://github.com/tdunning/t-digest/issues/49)
MergingDigest.centroids is wrong on an empty digest

