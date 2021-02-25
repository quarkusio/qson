package io.quarkus.qson.test;

import io.quarkus.qson.QsonIgnore;
import io.quarkus.qson.QsonIgnoreRead;
import io.quarkus.qson.QsonIgnoreWrite;
import io.quarkus.qson.QsonProperty;
import io.quarkus.qson.generator.Deserializer;
import io.quarkus.qson.generator.PropertyReference;
import io.quarkus.qson.generator.QsonMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class PropertyAnnotationTest {
    public static class Pojo {
        private int renamedGetter;
        private int renamedSetter;

        @QsonProperty("field")
        private int renamedField;

        private int ignoredGetter;
        private int ignoredSetter;

        @QsonIgnore
        private int ignoredField;

        @QsonProperty("getter")
        public int getRenamedGetter() {
            return renamedGetter;
        }

        public void setRenamedGetter(int renamedGetter) {
            this.renamedGetter = renamedGetter;
        }

        public int getRenamedSetter() {
            return renamedSetter;
        }

        @QsonProperty("setter")
        public void setRenamedSetter(int renamedSetter) {
            this.renamedSetter = renamedSetter;
        }

        public int getRenamedField() {
            return renamedField;
        }

        public void setRenamedField(int renamedField) {
            this.renamedField = renamedField;
        }

        @QsonIgnore
        public int getIgnoredGetter() {
            return ignoredGetter;
        }

        public void setIgnoredGetter(int ignoredGetter) {
            this.ignoredGetter = ignoredGetter;
        }

        public int getIgnoredSetter() {
            return ignoredSetter;
        }

        @QsonIgnore
        public void setIgnoredSetter(int ignoredSetter) {
            this.ignoredSetter = ignoredSetter;
        }

        public int getIgnoredField() {
            return ignoredField;
        }

        public void setIgnoredField(int ignoredField) {
            this.ignoredField = ignoredField;
        }
    }

    //@Test
    public void testGenerate() {
        Deserializer.create(Pojo.class).output(new TestClassOutput()).generate();

    }

    @Test
    public void testMappingAndIgnore() throws Exception {
        List<PropertyReference> properties = PropertyReference.getProperties(Pojo.class);

        QsonMapper mapper = new QsonMapper();
        String json = "{\n" +
                "  \"getter\": 42,\n" +
                "  \"setter\": 42,\n" +
                "  \"field\": 42,\n" +
                "  \"ignoredField\": -1,\n" +
                "  \"ignoredGetter\": -1,\n" +
                "  \"ignoredSetter\": -1\n" +
                "}";

        Pojo pojo = mapper.read(json, Pojo.class);
        Assertions.assertEquals(42, pojo.getRenamedGetter());
        Assertions.assertEquals(42, pojo.getRenamedSetter());
        Assertions.assertEquals(42, pojo.getRenamedField());
        Assertions.assertEquals(0, pojo.getIgnoredField());
        Assertions.assertEquals(0, pojo.getIgnoredGetter());
        Assertions.assertEquals(0, pojo.getIgnoredSetter());

        json = mapper.writeString(pojo);
        System.out.println(json);
        Assertions.assertFalse(json.contains("ignoredField"));
        Assertions.assertFalse(json.contains("ignoredGetter"));
        Assertions.assertFalse(json.contains("ignoredSetter"));

        pojo = mapper.read(json, Pojo.class);
        Assertions.assertEquals(42, pojo.getRenamedGetter());
        Assertions.assertEquals(42, pojo.getRenamedSetter());
        Assertions.assertEquals(42, pojo.getRenamedField());
        Assertions.assertEquals(0, pojo.getIgnoredField());
        Assertions.assertEquals(0, pojo.getIgnoredGetter());
        Assertions.assertEquals(0, pojo.getIgnoredSetter());
    }

    public static class Pojo2 {
        int deserializedOnly;
        int serializedOnly;
        int setterDeserializedOnly;
        int setterSerializedOnly;
        int getterDeserializedOnly;
        int getterSerializedOnly;

        @QsonIgnoreWrite
        int fieldDeserializedOnly;
        @QsonIgnoreRead
        int fieldSerializedOnly;

        public void setDeserializedOnly(int deserializedOnly) {
            this.deserializedOnly = deserializedOnly;
        }

        @QsonIgnoreWrite
        public void setSetterDeserializedOnly(int setterDeserializedOnly) {
            this.setterDeserializedOnly = setterDeserializedOnly;
        }

        @QsonIgnoreRead
        public void setSetterSerializedOnly(int setterSerializedOnly) {
            this.setterSerializedOnly = setterSerializedOnly;
        }

        public void setGetterDeserializedOnly(int getterDeserializedOnly) {
            this.getterDeserializedOnly = getterDeserializedOnly;
        }

        public void setGetterSerializedOnly(int getterSerializedOnly) {
            this.getterSerializedOnly = getterSerializedOnly;
        }

        public void setFieldDeserializedOnly(int fieldDeserializedOnly) {
            this.fieldDeserializedOnly = fieldDeserializedOnly;
        }

        public void setFieldSerializedOnly(int fieldSerializedOnly) {
            this.fieldSerializedOnly = fieldSerializedOnly;
        }

        public int getSerializedOnly() {
            return serializedOnly;
        }

        public int getSetterDeserializedOnly() {
            return setterDeserializedOnly;
        }

        public int getSetterSerializedOnly() {
            return setterSerializedOnly;
        }

        @QsonIgnoreWrite
        public int getGetterDeserializedOnly() {
            return getterDeserializedOnly;
        }

        @QsonIgnoreRead
        public int getGetterSerializedOnly() {
            return getterSerializedOnly;
        }

        public int getFieldDeserializedOnly() {
            return fieldDeserializedOnly;
        }

        public int getFieldSerializedOnly() {
            return fieldSerializedOnly;
        }
    }

    @Test
    public void testSerialization() {
        QsonMapper mapper = new QsonMapper();
        String json = "{\n" +
                "  \"deserializedOnly\": 42,\n" +
                "  \"serializedOnly\": -1,\n" +
                "  \"setterDeserializedOnly\": 42,\n" +
                "  \"setterSerializedOnly\": -1,\n" +
                "  \"getterDeserializedOnly\": 42,\n" +
                "  \"getterSerializedOnly\": -1,\n" +
                "  \"fieldDeserializedOnly\": 42,\n" +
                "  \"fieldSerializedOnly\": -1\n" +
                "}";
        Pojo2 pojo = mapper.read(json, Pojo2.class);
        Assertions.assertEquals(42, pojo.deserializedOnly);
        Assertions.assertEquals(42, pojo.setterDeserializedOnly);
        Assertions.assertEquals(42, pojo.getterDeserializedOnly);
        Assertions.assertEquals(42, pojo.fieldDeserializedOnly);
        Assertions.assertEquals(0, pojo.serializedOnly);
        Assertions.assertEquals(0, pojo.setterSerializedOnly);
        Assertions.assertEquals(0, pojo.getterSerializedOnly);
        Assertions.assertEquals(0, pojo.fieldSerializedOnly);

        json = mapper.writeString(pojo);
        System.out.println(json);
        Assertions.assertFalse(json.contains("\"deserializedOnly\""));
        Assertions.assertFalse(json.contains("\"setterDeserializedOnly\""));
        Assertions.assertFalse(json.contains("\"getterDeserializedOnly\""));
        Assertions.assertFalse(json.contains("\"fieldDeserializedOnly\""));
    }
}
