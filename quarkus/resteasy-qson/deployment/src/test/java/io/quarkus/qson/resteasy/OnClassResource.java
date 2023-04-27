package io.quarkus.qson.resteasy;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/onclass")
@Produces("application/json")
@Consumes("application/json")
public class OnClassResource {

    @POST
    public OnClass post(OnClass on) {
        return on;
    }

    @Path("/annotated")
    @POST
    public QsonAnnotated postAnnotated(QsonAnnotated an) {
        return an;
    }
}
