package io.quarkus.qson.deployment;

import io.quarkus.builder.item.MultiBuildItem;

import java.lang.reflect.Type;

public final class QsonBuildItem extends MultiBuildItem {
    private final Type genericType;
    private final boolean generateParser;
    private final boolean generateWriter;

    public QsonBuildItem(Type genericType, boolean generateParser, boolean generateWriter) {
        this.genericType = genericType;
        this.generateParser = generateParser;
        this.generateWriter = generateWriter;
    }

    public Type getGenericType() {
        return genericType;
    }

    public boolean isGenerateParser() {
        return generateParser;
    }

    public boolean isGenerateWriter() {
        return generateWriter;
    }
}
