package io.quarkus.qson.resteasy.reactive;

import io.quarkus.qson.runtime.QuarkusQsonMapper;
import org.junit.jupiter.api.Assertions;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Path("/customers")
public class CustomerResource {

    @Inject
    QuarkusQsonMapper mapper;

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Customer getCustomer() {
        Assertions.assertNotNull(mapper);
        Customer c = new Customer();
        c.setName("Bill");
        return c;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Customer> getCustomers() {
        Assertions.assertNotNull(mapper);
        List<Customer> list = new ArrayList<>();
        list.add(getCustomer());
        return list;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void createCustomers(List<Customer> c) {
        Assertions.assertNotNull(mapper);
        Assertions.assertEquals("Bill", c.get(0).getName());
    }
    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateCustomer(Customer c) {
        Assertions.assertNotNull(mapper);
        Assertions.assertEquals("Bill", c.getName());
    }
}
