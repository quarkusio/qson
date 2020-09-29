package io.quarkus.qson.serializer;

public interface ByteWriter {
    void write(int b);
    void write(byte[] bytes);
}
