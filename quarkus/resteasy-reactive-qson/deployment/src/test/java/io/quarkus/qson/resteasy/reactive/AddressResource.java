package io.quarkus.qson.resteasy.reactive;

import jakarta.ws.rs.Path;

@Path("/address")
public class AddressResource extends AbstractResource<Address> {
}
