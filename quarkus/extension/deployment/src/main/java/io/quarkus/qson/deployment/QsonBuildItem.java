package io.quarkus.qson.deployment;

import io.quarkus.builder.item.MultiBuildItem;

import java.lang.reflect.Type;

public final class QsonBuildItem extends MultiBuildItem {
    private final Class type;
    private final Type genericType;
    private final boolean generateParser;
    private final boolean generateWriter;

    public QsonBuildItem(Class type, Type genericType, boolean generateParser, boolean generateWriter) {
        this.type = type;
        this.genericType = genericType;
        this.generateParser = generateParser;
        this.generateWriter = generateWriter;
    }

    public Class getType() {
        return type;
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
