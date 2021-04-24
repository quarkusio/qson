package io.quarkus.funqy.test;

import io.quarkus.funqy.Funq;

import java.util.Map;

public class QueryFunction {

    @Funq
    public Simple simple(Simple simple) {
        return simple;
    }

    @Funq
    public Nested nested(Nested nested) {
        return nested;
    }

    @Funq
    public NestedCollection nestedCollection(NestedCollection nested) {
        return nested;
    }

    @Funq
    public Map<String, String> map(Map<String, String> nested) {
        return nested;
    }
}
