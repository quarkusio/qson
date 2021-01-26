package org.sample;


import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Writer that creates a byte array as a result
 *
 */
public class NosyncByteArrayOutputStream extends OutputStream {

    byte[] buffer;
    int count;

    /**
     * Uses 512 bytes as an initial capacity for its buffer
     *
     */
    public NosyncByteArrayOutputStream() {
        this(512);
    }

    /**
     *
     * @param initialCapacity of the buffer
     */
    public NosyncByteArrayOutputStream(int initialCapacity) {
        buffer = new byte[initialCapacity];
    }

    @Override
    public void write(int i) throws IOException {
        if (count == buffer.length) {
            buffer = Arrays.copyOf(buffer, buffer.length + buffer.length / 2);
        }
        buffer[count++] = (byte)i;
    }

    @Override
    public void write(byte[] bytes, int off, int len) throws IOException {
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
