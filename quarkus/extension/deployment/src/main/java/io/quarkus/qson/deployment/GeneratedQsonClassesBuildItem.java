package io.quarkus.qson.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

import java.util.Map;

public final class GeneratedQsonClassesBuildItem extends SimpleBuildItem {
    final Map<String, String> generatedParsers;
    final Map<String, String> generatedWriters;

    public GeneratedQsonClassesBuildItem(Map<String, String> generatedParsers, Map<String, String> generatedWriters) {
        this.generatedParsers = generatedParsers;
        this.generatedWriters = generatedWriters;
    }

    public Map<String, String> getGeneratedParsers() {
        return generatedParsers;
    }

    public Map<String, String> getGeneratedWriters() {
        return generatedWriters;
    }
}
