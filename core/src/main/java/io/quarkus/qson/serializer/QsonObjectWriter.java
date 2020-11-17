package io.quarkus.qson.serializer;

public interface QsonObjectWriter {
    void write(JsonWriter writer, Object target);
}
