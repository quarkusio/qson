package io.quarkus.qson.test;

import io.quarkus.qson.QsonTransformer;
import io.quarkus.qson.generator.QsonMapper;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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



}
