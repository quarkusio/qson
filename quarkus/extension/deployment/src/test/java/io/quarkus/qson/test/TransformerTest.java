package io.quarkus.qson.test;

import io.quarkus.qson.QsonCustomWriter;
import io.quarkus.qson.QsonTransformer;
import io.quarkus.qson.runtime.QuarkusQsonMapper;
import io.quarkus.qson.writer.JsonWriter;
import io.quarkus.qson.writer.QsonObjectWriter;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import jakarta.inject.Inject;

public class TransformerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Thirdparty.class, Transformer.class, Custom.class));

    public static class Thirdparty {
        int x;

        public Thirdparty(int x) {
            this.x = x;
        }

        public int getX() {
            return x;
        }
    }
    public static class Transformer {
        int x;

        @QsonTransformer
        public Thirdparty getThirdparty() {
            return new Thirdparty(x);
        }

        public void setX(int x) {
            this.x = x;
        }
    }

    @Inject
    QuarkusQsonMapper mapper;

    @Test
    public void testTransformer() throws Exception {
        String json = "{ \"x\": 42 }";
        Thirdparty t = mapper.parserFor(Thirdparty.class.getName()).read(json);
        Assertions.assertEquals(42, t.getX());
    }

    @QsonCustomWriter(Thirdparty.class)
    public static class Custom implements QsonObjectWriter {
        @Override
        public void write(JsonWriter writer, Object target) {
            Assertions.assertTrue(target instanceof Thirdparty);
            writer.writeBytes("{ \"foobar\": 12 }".getBytes());
        }
    }

    @Test
    public void testCustomWriter() throws Exception {
        String json = mapper.writerFor(Thirdparty.class.getName()).writeString(new Thirdparty(-1));
        Assertions.assertEquals("{ \"foobar\": 12 }", json);
    }
}
