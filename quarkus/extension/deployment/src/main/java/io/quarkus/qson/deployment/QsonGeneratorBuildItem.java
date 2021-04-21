package io.quarkus.qson.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

public final class QsonGeneratorBuildItem  extends SimpleBuildItem {
    private final QuarkusQsonGeneratorImpl generator;

    public QsonGeneratorBuildItem(QuarkusQsonGeneratorImpl generator) {
        this.generator = generator;
    }

    public QuarkusQsonGeneratorImpl getGenerator() {
        return generator;
    }
}
