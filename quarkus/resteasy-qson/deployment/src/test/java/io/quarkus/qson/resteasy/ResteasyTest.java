package io.quarkus.qson.resteasy;

import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class ResteasyTest {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(CustomerResource.class, Customer.class,
                            AbstractResource.class, Address.class, AddressResource.class
                    , OnClass.class, OnClassResource.class, QsonAnnotated.class));

    @Test
    public void testUserClass() throws Exception {
        given().get("/customers/1").then()
                .statusCode(200)
                .body("name", equalTo("Bill"));
        given().get("/customers").then()
                .statusCode(200)
                .body("name[0]", equalTo("Bill"));
        given().contentType("application/json")
                .body("{ \"name\": \"Bill\"}")
                .put("/customers/1")
                .then().statusCode(204);
        given().contentType("application/json")
                .body("[{ \"name\": \"Bill\"}]")
                .post("/customers")
                .then().statusCode(204);
    }

    @Test
    public void testTypeVariable() throws Exception {
        given().contentType("application/json")
                .body("{ \"city\": \"Boston\"}")
                .post("/address")
                .then().statusCode(200)
                .body("city", equalTo("Boston"));

    }
    @Test
    public void testOnClass() throws Exception {
        given().contentType("application/json")
                .body("{ \"name\": \"Bill\"}")
                .post("/onclass")
                .then().statusCode(200)
                .body("name", equalTo("Bill"));

        given().contentType("application/json")
                .body("{ \"name\": \"Bill\"}")
                .post("/onclass/annotated")
                .then().statusCode(200)
                .body("name", equalTo("Bill"));

    }
}
