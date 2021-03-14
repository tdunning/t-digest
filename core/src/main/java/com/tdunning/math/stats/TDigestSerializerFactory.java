package com.tdunning.math.stats;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TDigestSerializerFactory {
    private static final String PROPERTIES_FILE_NAME = "serialization.properties";
    private static final String SERIALIZER_PROP_KEY = "tdigest.serializerClass";
    
    private Class<? extends TDigestSerializer<? extends TDigest, ? extends Object>> clazz;
    private Properties serializationProperties;
    
    @SuppressWarnings("unchecked")
    public TDigestSerializerFactory() {
        Properties props = new Properties();
        try {
            File f = new File(PROPERTIES_FILE_NAME);
            InputStream is = f.exists() ?
                new FileInputStream(f)
                : getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE_NAME);
                
            props.load(is);
            if(!props.containsKey(SERIALIZER_PROP_KEY))
                throw new IllegalStateException(PROPERTIES_FILE_NAME + " does not contain key " + SERIALIZER_PROP_KEY);
            
            clazz = (Class<? extends TDigestSerializer<? extends TDigest, ? extends Object>>) Class.forName(props.getProperty(SERIALIZER_PROP_KEY));
            serializationProperties = props;
        } catch (IOException | ClassNotFoundException e) {
            clazz = TDigestDefaultSerializer.class;
            e.printStackTrace(System.err);
        }
    }

    @SuppressWarnings("rawtypes")
    public TDigestSerializer create() throws TDigestSerializerException {
        try {
            return (TDigestSerializer)clazz.getConstructor(Properties.class).newInstance(serializationProperties);
        } catch (Exception e) {
            throw new TDigestSerializerException(e);
        }
    }

}
