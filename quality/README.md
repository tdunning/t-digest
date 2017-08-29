Quality Testing
===============

This module contains a number of programs that assess the accuracy of t-digest implementations. 
In the process, bounds can be set on the quality of the t-digest idea itself. 

The implementation of a t-digest can have a variety of subtle flaws that do not 
affect operation except to compromise accuracy or to increase the size of
a digest. The tests in this module aim to highlight how and where an
implementation may be going wrong.

Basic Accuracy
--------------

Accuracy versus Size
-----
```python
for each algorithm:
  for compression in [10,20,50,100,200,500,1000]:
    for distribution in ["gamma", "flip-gamma", "uniform"]:
      add data
      for q in [0.0001, 0.001,0.01,0.1,0.5,0.9,0.99,0.999,0.9999]:
        record algorithm, compression, distribution, q, x_data, x_digest, q_data
```
Bin Distribution
-----
```python
for each algorithm:
  compression = 100
  for distribution in ["gamma", "uniform"]:
      add data
      for bin in [0.0001, 0.001,0.01,0.5]:
        for sample in bin:
           record algorithm, compression, distribution, q_bin, x
```

General Considerations
=======

Input Distribution
-------

Compression Factor
-------

Data Size
-------


