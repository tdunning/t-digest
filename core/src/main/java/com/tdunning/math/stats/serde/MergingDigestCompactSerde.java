package com.tdunning.math.stats.serde;

import com.tdunning.math.stats.DigestModel;
import com.tdunning.math.stats.MergingDigest;

import java.nio.ByteBuffer;


public class MergingDigestCompactSerde {

  public static void serialize(MergingDigest digest, ByteBuffer buf) {
    DigestModel model = digest.toModel();
    buf.putInt(2);
    buf.putDouble(model.min());                          // + 8
    buf.putDouble(model.max());                          // + 8
    buf.putFloat((float) model.compression());           // + 4
    buf.putShort((short) model.mainBufferSize().intValue());           // + 2
    buf.putShort((short) model.tempBufferSize().intValue());       // + 2
    buf.putShort((short) model.centroidCount());          // + 2 = 30

    double[] position = model.centroidPositions();
    double[] weight = model.centroidWeights();
    for (int i = 0; i < model.centroidCount(); i++) {
      buf.putFloat((float) position[i]);
      buf.putFloat((float) weight[i]);
    }
  }

  public static MergingDigest deserialize(ByteBuffer buf) {
    boolean compactEncoding = buf.getInt() == 2;
    if (!compactEncoding) {
      throw new IllegalArgumentException("Serialization was not done using compact encoding, cannot deserialize");
    }

    double min = buf.getDouble();
    double max = buf.getDouble();
    double compression = buf.getFloat();
    int mainBufferSize = buf.getShort();
    int tempBufferSize = buf.getShort();
    int centroidCount = buf.getShort();

    double[] position = new double[centroidCount];
    double[] weight = new double[centroidCount];
    for (int i = 0; i < centroidCount; i++) {
      position[i] = buf.getFloat();
      weight[i] = buf.getFloat();
    }

    DigestModel model = new DigestModel(compression, min, max, centroidCount, position, weight, mainBufferSize, tempBufferSize);
    model.setCompactEncoding(true);
    return MergingDigest.fromModel(model);
  }

}
