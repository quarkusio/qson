package io.quarkus.qson.serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ByteArrayByteWriter implements ByteWriter {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    @Override
    public void write(int b) {
        baos.write(b);
    }

    @Override
    public void write(byte[] bytes) {
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
