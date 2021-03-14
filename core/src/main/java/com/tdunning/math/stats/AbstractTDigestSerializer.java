package com.tdunning.math.stats;

import java.util.Properties;

public abstract class AbstractTDigestSerializer<T extends TDigest, O extends Object> implements TDigestSerializer<T, O> {
    private Properties properties;
    
    public AbstractTDigestSerializer(Properties properties) {
        this.properties = properties;
    }    
    
    public Properties getProperties() {
        return properties;
    }

}
