package com.tdunning.math.stats.serde;

import com.tdunning.math.stats.DigestModel;
import com.tdunning.math.stats.TDigest;

import java.nio.ByteBuffer;

public class DigestModelDefaultSerde {

  public static void serialize(DigestModel model, ByteBuffer buf) {
    buf.putInt(1);
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
    boolean verboseEncoding = buf.getInt() == 1;
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
