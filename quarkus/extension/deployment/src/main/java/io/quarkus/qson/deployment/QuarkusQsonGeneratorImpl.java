package io.quarkus.qson.deployment;

import io.quarkus.qson.GenericType;
import io.quarkus.qson.generator.Generator;
import io.quarkus.qson.runtime.QuarkusQsonGenerator;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

class QuarkusQsonGeneratorImpl extends Generator implements QuarkusQsonGenerator {
    Set<Type> parsers = new HashSet<>();
    Set<Type> writers = new HashSet<>();

    @Override
    public QuarkusQsonGenerator register(Type type, boolean parser, boolean writer) {
        if (parser) parsers.add(type);
        if (writer) writers.add(type);
        return this;
    }

    @Override
    public QuarkusQsonGenerator register(GenericType type, boolean parser, boolean writer) {
        register(type.getType(), parser, writer);
        return this;
    }

    public Set<Type> getParsers() {
        return parsers;
    }

    public Set<Type> getWriters() {
        return writers;
    }
}
