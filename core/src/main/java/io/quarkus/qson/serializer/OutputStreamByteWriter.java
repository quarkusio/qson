package io.quarkus.qson.serializer;


import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamByteWriter implements ByteWriter {
    private final OutputStream stream;

    public OutputStreamByteWriter(OutputStream stream) {
        this.stream = stream;
    }

    @Override
    public void write(int b) {
        try {
            stream.write(b);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void write(byte[] bytes) {
        try {
            stream.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
