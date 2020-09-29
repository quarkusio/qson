package io.quarkus.qson.serializer;

import java.util.Collection;

public class CollectionWriter implements ObjectWriter {
    private ObjectWriter elementWriter;

    public CollectionWriter(ObjectWriter elementWriter) {
        this.elementWriter = elementWriter;
    }

    @Override
    public void write(JsonWriter writer, Object target) {
        Collection list = (Collection)target;
        writer.write(list, elementWriter);
    }
}
