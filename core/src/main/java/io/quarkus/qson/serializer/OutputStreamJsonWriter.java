package io.quarkus.qson.serializer;


import io.quarkus.qson.QsonException;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Writer for an OutputStream
 *
 */
public class OutputStreamJsonWriter extends JsonByteWriter {
    private final OutputStream stream;

    public OutputStreamJsonWriter(OutputStream stream) {
        this.stream = stream;
    }

    @Override
    public void writeByte(int b) {
        try {
            stream.write(b);
        } catch (IOException e) {
            throw new QsonException(e);
        }
    }

    @Override
    public void writeBytes(byte[] bytes) {
        try {
            stream.write(bytes);
        } catch (IOException e) {
            throw new QsonException(e);
        }
    }
}
