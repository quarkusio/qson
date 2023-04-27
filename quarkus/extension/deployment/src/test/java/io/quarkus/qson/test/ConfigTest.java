package io.quarkus.qson.test;

import io.quarkus.qson.Qson;
import io.quarkus.qson.runtime.QuarkusQsonMapper;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import jakarta.inject.Inject;
import java.util.Date;

public class ConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyDate.class)
                    .addAsResource("config-test-application.properties", "application.properties")
            );


    @Qson
    public static class MyDate {
        Date date;

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

    }
    @Inject
    QuarkusQsonMapper mapper;

    @Test
    public void testDefaultPropertyMillis() throws Exception {
        Date now = new Date();
        String json = "{ \"date\": " + now.toInstant().toEpochMilli() + "}";

        MyDate date = mapper.parserFor(MyDate.class).read(json);
        Assertions.assertEquals(now, date.getDate());
        json = mapper.writerFor(MyDate.class).writeString(date);
        date = mapper.parserFor(MyDate.class).read(json);
        Assertions.assertEquals(now, date.getDate());
    }
}
