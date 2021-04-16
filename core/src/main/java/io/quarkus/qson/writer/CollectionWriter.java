package io.quarkus.qson.writer;

import java.util.Collection;

public class CollectionWriter implements QsonObjectWriter {
    private QsonObjectWriter elementWriter;

    public CollectionWriter(QsonObjectWriter elementWriter) {
        this.elementWriter = elementWriter;
    }

    @Override
    public void write(JsonWriter writer, Object target) {
        Collection list = (Collection)target;
        writer.write(list, elementWriter);
    }
}
