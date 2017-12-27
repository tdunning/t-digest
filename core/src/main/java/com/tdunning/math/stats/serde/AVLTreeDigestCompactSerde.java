package com.tdunning.math.stats.serde;

import com.tdunning.math.stats.AVLTreeDigest;
import com.tdunning.math.stats.DigestModel;

import java.nio.ByteBuffer;

public class AVLTreeDigestCompactSerde {

  public static void serialize(AVLTreeDigest digest, ByteBuffer buf) {
    DigestModel model = digest.toModel();
    buf.putInt(2);
    buf.putDouble(model.min());
    buf.putDouble(model.max());
    buf.putDouble(model.compression());
    buf.putInt(model.centroidCount());

    double[] position = model.centroidPositions();
    double[] weight = model.centroidWeights();
    double x = 0;
    for(int i = 0;i < model.centroidCount(); i++) {
      double delta = position[i] - x;
      x = position[i];
      buf.putFloat((float) delta);
      encodeInt(buf, (int) weight[i]);
    }
  }

  public static AVLTreeDigest deserialize(ByteBuffer buf) {
    boolean compactEncoding = buf.getInt() == 2;
    if (!compactEncoding) {
      throw new IllegalArgumentException("Serialization was not done using compact encoding, cannot deserialize");
    }

    double min = buf.getDouble();
    double max = buf.getDouble();
    double compression = buf.getDouble();
    int centroidCount = buf.getInt();

    double[] position = new double[centroidCount];
    double[] weight = new double[centroidCount];
    double x = 0;
    for (int i = 0; i < centroidCount; i++) {
      double delta = buf.getFloat();
      x += delta;
      position[i] = x;
      weight[i] = decodeInt(buf);
    }

    DigestModel model = new DigestModel(compression, min, max, centroidCount, position, weight);
    model.setCompactEncoding(true);
    return AVLTreeDigest.fromModel(model);
  }

  public static void encodeInt(ByteBuffer buf, int n) {
      int k = 0;
      while (n < 0 || n > 0x7f) {
          byte b = (byte) (0x80 | (0x7f & n));
          buf.put(b);
          n = n >>> 7;
          k++;
          if (k >= 6) {
              throw new IllegalStateException("Size is implausibly large");
          }
      }
      buf.put((byte) n);
  }

  public static int decodeInt(ByteBuffer buf) {
      int v = buf.get();
      int z = 0x7f & v;
      int shift = 7;
      while ((v & 0x80) != 0) {
          if (shift > 28) {
              throw new IllegalStateException("Shift too large in decode");
          }
          v = buf.get();
          z += (v & 0x7f) << shift;
          shift += 7;
      }
      return z;
  }
}
