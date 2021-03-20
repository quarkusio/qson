package io.quarkus.qson.serializer;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Writer that creates a byte array as a result
 */
public class BufferedStreamJsonWriter extends JsonByteWriter {

    final byte[] buffer;
    int count;
    final OutputStream os;

    public BufferedStreamJsonWriter(byte[] buffer, OutputStream os) {
        this.buffer = buffer;
        this.os = os;
    }

    /**
     * Buffer capacity defaults to 512
     *
     * @param os
     */
    public BufferedStreamJsonWriter(OutputStream os) {
        this(new byte[512], os);
    }
    public BufferedStreamJsonWriter(OutputStream os, int bufferSize) {
        this(new byte[bufferSize], os);
    }

    @Override
    public void writeByte(int b) {
        if (count == buffer.length) {
            try {
                os.write(buffer);
                count = 0;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        buffer[count++] = (byte)b;
    }

    @Override
    public void writeBytes(byte[] bytes) {
        if (count + bytes.length > buffer.length) {
            try {
                os.write(buffer, 0, count);
                count = 0;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (bytes.length > buffer.length) {
                try {
                    os.write(bytes);
                    return;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        System.arraycopy(bytes, 0, buffer, count, bytes.length);
        count += bytes.length;
    }

    public void flush() {
        try {
            if (count > 0) {
                os.write(buffer, 0, count);
                count = 0;
            }
            os.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
