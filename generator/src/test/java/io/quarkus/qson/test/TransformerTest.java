package io.quarkus.qson.test;

import io.quarkus.qson.QsonTransformer;
import io.quarkus.qson.generator.QsonMapper;
import io.quarkus.qson.writer.JsonWriter;
import io.quarkus.qson.writer.QsonObjectWriter;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

public class TransformerTest {
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

    @Test
    public void testTransformer() throws Exception {
        String json = "{ \"x\": 42 }";
        QsonMapper mapper = new QsonMapper();
        mapper.overrideMappingFor(Thirdparty.class).transformer(Transformer.class);
        Thirdparty t = mapper.read(json, Thirdparty.class);
        Assertions.assertEquals(42, t.getX());
    }

    public static class Custom implements QsonObjectWriter {
        @Override
        public void write(JsonWriter writer, Object target) {
            Assertions.assertTrue(target instanceof Thirdparty);
            writer.writeBytes("{ \"foobar\": 12 }".getBytes());
        }
    }

    public static QsonObjectWriter writer = new Custom();

    @Test
    public void testCustomWriter() throws Exception {
        QsonMapper mapper = new QsonMapper();
        mapper.mappingFor(Thirdparty.class).customWriter(Custom.class);
        String json = mapper.writeString(new Thirdparty(-1));
        Assertions.assertEquals("{ \"foobar\": 12 }", json);
    }

    @Test
    public void testCustomWriterField() throws Exception {
        QsonMapper mapper = new QsonMapper();
        Field field = TransformerTest.class.getField("writer");
        mapper.mappingFor(Thirdparty.class).customWriter(field);
        String json = mapper.writeString(new Thirdparty(-1));
        Assertions.assertEquals("{ \"foobar\": 12 }", json);
    }



}
