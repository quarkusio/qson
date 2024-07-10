package io.quarkus.qson.resteasy;

import io.quarkus.qson.runtime.QuarkusQsonMapper;
import org.junit.jupiter.api.Assertions;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
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
