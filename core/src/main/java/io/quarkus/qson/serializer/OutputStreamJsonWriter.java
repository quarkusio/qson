package io.quarkus.qson.serializer;


import java.io.IOException;
import java.io.OutputStream;

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
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeBytes(byte[] bytes) {
        try {
            stream.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
