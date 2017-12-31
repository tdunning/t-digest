package com.tdunning.math.stats;

public class DigestModel {
  private final double compression;
  private final double min;
  private final double max;
  private final int centroidCount;
  private final double[] centroidPositions;
  private final double[] centroidWeights;

  //For compact encoding of MergingDigest
  private Integer mainBufferSize = null;
  private Integer tempBufferSize = null;

  private boolean compactEncoding = false;

  public DigestModel(double compression, double min, double max, int centroidCount, double[] centroidPositions, double[] centroidWeights) {
    this.compression = compression;
    this.min = min;
    this.max = max;
    this.centroidCount = centroidCount;
    this.centroidPositions = centroidPositions;
    this.centroidWeights = centroidWeights;
  }

  public DigestModel(double compression, double min, double max, int centroidCount, double[] centroidPositions, double[] centroidWeights, int mainBufferSize, int tempBufferSize) {
    this(compression, min, max, centroidCount, centroidPositions, centroidWeights);
    this.mainBufferSize = mainBufferSize;
    this.tempBufferSize = tempBufferSize;
  }

  public void setCompactEncoding(boolean compactEncoding) {
    this.compactEncoding = compactEncoding;
  }

  public double compression() {
    return compression;
  }

  public double min() {
    return min;
  }

  public double max() {
    return max;
  }

  public int centroidCount() {
    return centroidCount;
  }

  public double[] centroidPositions() {
    return centroidPositions;
  }

  public double[] centroidWeights() {
    return centroidWeights;
  }

  public boolean compactEncoding() {
    return compactEncoding;
  }

  public Integer mainBufferSize() {
    return mainBufferSize;
  }

  public Integer tempBufferSize() {
    return tempBufferSize;
  }
}
