Release 3.2
===========
In release 3.2, the goal is to produce an update to the code given the large number of improvements since the previous release.

There are a few bugs that will survive this release, most notably in the AVLTreeDigest. These have to do with large nubmers of repeated data points and are not new bugs.
 
There is a also lot of work going on with serialization. I need to hear from people about  what they are doing with serialization so that we can build some test cases to allow an appropriate migration strategy to future serialization.

The paper continues to be updated. The algorithmic descriptions are getting reasonably clear, but the speed and accuracy sections need a complete revamp with current implementations.


Bugs, fixed and known
----

The following important issues are fixed in this release

[Issue #90](https://github.com/tdunning/t-digest/issues/90) Serialization for MergingDigest

[Issue #92](https://github.com/tdunning/t-digest/issues/92) Serialization for AVLTreeDigest

This issue has substantial progress, but lacks a definitive test to determine whether it should be closed.

[Issue 78](https://github.com/tdunning/t-digest/issues/78) Stability under merging.

The following issues are pushed beyond this release

[Issue #87](https://github.com/tdunning/t-digest/issues/87) Future proof and extensible serialization

[Issue #89](https://github.com/tdunning/t-digest/issues/89) Bad handling for duplicate values in AVLTreeDigest

Here is a complete list of issues resolved in this release:

#55
#52
#90
#92
#93
#67
#81
#75
#74
#72
#65
#60
#53
#82
#42
#40
#37
#29
#84
#76
#77
#71
#61
#58
#48
#63
#62
#47
#49

