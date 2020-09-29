package io.quarkus.qson.serializer;

public interface ObjectWriter {
    void write(JsonWriter writer, Object target);
}
