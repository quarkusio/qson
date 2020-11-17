package io.quarkus.qson.serializer;

public class GenericObjectWriter implements QsonObjectWriter {
    @Override
    public void write(JsonWriter writer, Object target) {
        writer.writeObject(target);
    }
}
