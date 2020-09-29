package io.quarkus.qson.test;

public class Single {
    private int name;
    public int getName() {
        return name;
    }

    public Single setName(int name) {
        this.name = name;
        return this;
    }
}
