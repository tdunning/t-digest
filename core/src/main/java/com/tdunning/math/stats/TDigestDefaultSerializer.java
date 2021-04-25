package com.tdunning.math.stats;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Properties;

public class TDigestDefaultSerializer extends AbstractTDigestSerializer<TDigest, byte[]> {
    
    public TDigestDefaultSerializer(Properties properties) {
        super(properties);
    }

    @Override
    public byte[] serialize(TDigest tDigest) throws TDigestSerializerException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(5120);
        try (ObjectOutputStream out = new ObjectOutputStream(baos)) {
            out.writeObject(tDigest);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new TDigestSerializerException(e);
        } 
    }

    @Override
    public TDigest deserialize(byte[] object) throws TDigestSerializerException {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(object))) {
            return (TDigest) in.readObject();
        } catch (ClassCastException | ClassNotFoundException | IOException e) {
            throw new TDigestSerializerException(e);
        }
    }

}
