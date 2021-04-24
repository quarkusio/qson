package io.quarkus.qson.test;

import io.quarkus.qson.QsonDate;
import io.quarkus.qson.runtime.QuarkusQsonGenerator;
import io.quarkus.qson.runtime.QuarkusQsonInitializer;
import io.quarkus.qson.runtime.QuarkusQsonMapper;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import java.util.Date;

public class InitializerTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Mydate.class));


    public static class Mydate {
        Date date;

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        @QuarkusQsonInitializer
        public static void initDate(QuarkusQsonGenerator gen) {
            gen.dateFormat(QsonDate.Format.MILLISECONDS);
            gen.register(Mydate.class, true, true);
        }
    }

    @Inject
    QuarkusQsonMapper mapper;

    @Test
    public void testDefaultPropertyMillis() throws Exception {
        Date now = new Date();
        String json = "{ \"date\": " + now.toInstant().toEpochMilli() + "}";

        Mydate date = mapper.parserFor(Mydate.class.getName()).read(json);
        Assertions.assertEquals(now, date.getDate());
        json = mapper.writerFor(Mydate.class.getName()).writeString(date);
        date = mapper.parserFor(Mydate.class.getName()).read(json);
        Assertions.assertEquals(now, date.getDate());
    }
}
