package io.quarkus.qson.serializer;

import java.util.Arrays;

/**
 * Writer that creates a byte array as a result
 */
public class ByteArrayJsonWriter extends JsonByteWriter {

    // NOTE:  ByteArrayOutputStream.write is synchronized and hurt microbenchmark numbers
    byte[] buffer;
    int count;

    /**
     * Uses 512 bytes as an initial capacity for its buffer
     *
     */
    public ByteArrayJsonWriter() {
        this(512);
    }

    /**
     *
     * @param initialCapacity of the buffer
     */
    public ByteArrayJsonWriter(int initialCapacity) {
        buffer = new byte[initialCapacity];
    }

    @Override
    public void writeByte(int b) {
        if (count == buffer.length) {
            buffer = Arrays.copyOf(buffer, buffer.length + buffer.length / 2);
        }
        buffer[count++] = (byte)b;
    }

    @Override
    public void writeBytes(byte[] bytes) {
        if (count + bytes.length > buffer.length) {
            buffer = Arrays.copyOf(buffer, buffer.length + buffer.length / 2 + bytes.length);
        }
        System.arraycopy(bytes, 0, buffer, count, bytes.length);
        count += bytes.length;
    }

    /**
     * Gets a copy of written buffer
     *
     * @return
     */
    public byte[] toByteArray() {
        return Arrays.copyOf(buffer, count);
    }

    /**
     * Get the underlying buffer (no copy)
     * This byte array may be padded with unwritten bytes.  Get the size() to
     * get the actual length of bytes written
     *
     * @return
     */
    public byte[] getBuffer() {
        return buffer;
    }

    /**
     * Size of bytes written to buffer.
     *
     * @return
     */
    public int size() {
        return count;
    }
}
