package io.quarkus.qson.serializer;

import io.quarkus.qson.QsonException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Writer that creates a byte array as a result
 *
 */
public class ByteArrayJsonWriter extends JsonByteWriter {
    ByteArrayOutputStream baos;

    /**
     * Uses 1024 bytes as an initial capacity for its buffer
     *
     */
    public ByteArrayJsonWriter() {
        baos = new ByteArrayOutputStream(1024);
    }

    /**
     *
     * @param initialCapacity of the buffer
     */
    public ByteArrayJsonWriter(int initialCapacity) {
        baos = new ByteArrayOutputStream(initialCapacity);
    }

    @Override
    public void writeByte(int b) {
        baos.write(b);
    }

    @Override
    public void writeBytes(byte[] bytes) {
        try {
            baos.write(bytes);
        } catch (IOException e) {
            throw new QsonException(e);
        }
    }

    /**
     * Gets a copy of written buffer
     *
     * @return
     */
    public byte[] getBytes() {
        return baos.toByteArray();
    }
}
