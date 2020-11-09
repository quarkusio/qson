package io.quarkus.qson.resteasy;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

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
