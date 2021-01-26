package io.quarkus.qson.resteasy.reactive;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;

public abstract class AbstractResource<T> {


    @POST
    @Produces("application/json")
    @Consumes("application/json")
    public T post(T t) {
        return t;
    }
}
