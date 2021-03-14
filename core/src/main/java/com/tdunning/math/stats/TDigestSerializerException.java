package com.tdunning.math.stats;

public class TDigestSerializerException extends Exception {
    
    public TDigestSerializerException(String error) {
        super(error);
    }
    
    public TDigestSerializerException(String error, Exception e) {
        super(error, e);
    }
    
    public TDigestSerializerException(Exception e) {
        super(e);
    }
    
}
