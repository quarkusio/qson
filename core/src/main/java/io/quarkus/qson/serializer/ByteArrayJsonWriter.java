package io.quarkus.qson.serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ByteArrayJsonWriter extends JsonByteWriter {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    @Override
    public void writeByte(int b) {
        baos.write(b);
    }

    @Override
    public void writeBytes(byte[] bytes) {
        try {
            baos.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getBytes() {
        return baos.toByteArray();
    }
}
