package io.quarkus.qson.resteasy;

import io.quarkus.qson.Qson;

@Qson
public class QsonAnnotated {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
