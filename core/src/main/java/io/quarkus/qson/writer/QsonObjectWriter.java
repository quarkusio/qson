package io.quarkus.qson.writer;

import java.io.OutputStream;

public interface QsonObjectWriter {
    void write(JsonWriter writer, Object target);

    /**
     * Helper method
     *
     * @param target
     * @return
     */
    default byte[] writeBytes(Object target) {
        ByteArrayJsonWriter writer = new ByteArrayJsonWriter(1024);
        write(writer, target);
        return writer.toByteArray();
    }

    /**
     * Helper method.
     *
     * Uses a BufferedStreamJsonWriter by default.
     *
     * @param os
     * @param target
     */
    default void writeStream(OutputStream os, Object target) {
        BufferedStreamJsonWriter writer = new BufferedStreamJsonWriter(os);
        write(writer, target);
        writer.flush();
    }

    /**
     * Helper method
     *
     * @param target
     * @return
     */
    default String writeString(Object target) {
        ByteArrayJsonWriter writer = new ByteArrayJsonWriter();
        write(writer, target);
        return new String(writer.getBuffer(), 0, writer.size(), JsonByteWriter.UTF8);
    }
}
