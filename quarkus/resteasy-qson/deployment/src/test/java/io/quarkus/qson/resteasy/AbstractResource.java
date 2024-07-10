package io.quarkus.qson.resteasy;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;

public abstract class AbstractResource<T> {


    @POST
    @Produces("application/json")
    @Consumes("application/json")
    public T post(T t) {
        return t;
    }
}
