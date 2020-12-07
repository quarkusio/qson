package io.quarkus.qson.deserializer;

public interface AnySetter {
    void setAny(Object target, String key, Object value);
}
