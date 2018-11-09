package com.tdunning.math.stats.serde;

public enum Encoding {
  VERBOSE(1), COMPACT(2);

  private final int code;

  Encoding(int code) {
    this.code = code;
  }

  public int code() {
    return this.code;
  }
}