package com.tdunning.math.stats.serde;

import com.tdunning.math.stats.DigestModel;

import java.nio.ByteBuffer;

public class DigestModelDefaultSerde {

  public static int byteSize(int centroids) {
    return (centroids * 16) + 32;
  }

  public static void serialize(DigestModel model, ByteBuffer buf) {
    buf.putInt(Encoding.VERBOSE.code());
    buf.putDouble(model.min());
    buf.putDouble(model.max());
    buf.putDouble(model.compression());
    buf.putInt(model.centroidCount());
    double[] position = model.centroidPositions();
    double[] weight = model.centroidWeights();
    for (int i = 0; i < model.centroidCount(); i++) {
      buf.putDouble(position[i]);
      buf.putDouble(weight[i]);
    }
  }

  public static DigestModel deserialize(ByteBuffer buf) {
    boolean verboseEncoding = buf.getInt() == Encoding.VERBOSE.code();
    if (!verboseEncoding) {
      throw new IllegalArgumentException("Serialization was not done using verbose encoding, cannot deserialize");
    }

    double min = buf.getDouble();
    double max = buf.getDouble();
    double compression = buf.getDouble();
    int centroidCount = buf.getInt();
    double[] position = new double[centroidCount];
    double[] weight = new double[centroidCount];
    for (int i = 0; i < centroidCount; i++) {
      position[i] = buf.getDouble();
      weight[i] = buf.getDouble();
    }

    return new DigestModel(compression, min, max, centroidCount, position, weight);
  }

}
