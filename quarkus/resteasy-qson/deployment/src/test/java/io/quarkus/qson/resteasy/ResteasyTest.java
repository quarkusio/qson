package io.quarkus.qson.resteasy;

import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import static org.hamcrest.Matchers.equalTo;

import javax.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;

public class ResteasyTest {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(CustomerResource.class, Customer.class));

    @Test
    public void testUserClass() throws Exception {
        given().get("/customers/1").then()
                .statusCode(200)
                .body("name", equalTo("Bill"));
        given().get("/customers").then()
                .statusCode(200)
                .body("name[0]", equalTo("Bill"));

    }
}
