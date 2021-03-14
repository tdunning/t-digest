package com.tdunning.math.stats;

public interface TDigestSerializer<T extends TDigest, O extends Object> {

    /**
     * Serializes a TDigest object
     * 
     * @param tDigest
     * @return
     * @throws TDigestSerializerException
     */
    public O serialize(T tDigest) throws TDigestSerializerException;

    /**
     * De-serializes an Object into a TDigest
     * 
     * @param object
     * @return
     * @throws TDigestSerializerException
     */
    public T deserialize(O object) throws TDigestSerializerException;

}
