package io.quarkus.qson.test;

import io.quarkus.qson.GenericType;
import io.quarkus.qson.QsonValue;
import io.quarkus.qson.generator.ClassMapping;
import io.quarkus.qson.generator.Generator;
import io.quarkus.qson.generator.QsonGenerator;
import io.quarkus.qson.generator.QsonMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ValueClassTest {
    public static class MyConstructorStringValue {
        private String string;

        @QsonValue
        public MyConstructorStringValue(String str) {
            this.string = str;
        }

        @QsonValue
        public String value() {
            return string;
        }
    }
    public static class MyMethodStringValue {
        private String string;

        @QsonValue
        public void setString(String string) {
            this.string = string;
        }

        @QsonValue
        public String value() {
            return string;
        }
    }

    public static class MyStringValue {
        private String string;

        public String stringValue() {
            return string;
        }

        public void create(String string) {
            this.string = string;
        }
    }

    public static class MyConstructorIntValue {
        private int val;

        @QsonValue
        public MyConstructorIntValue(int val) {
            this.val = val;
        }

        @QsonValue
        public int getVal() {
            return val;
        }
    }
    public static class MyMethodIntValue {
        private int val;

        @QsonValue
        public void value(int val) {
            this.val = val;
        }

        @QsonValue
        public int value() {
            return val;
        }
    }

    public static class MyIntValue {
        private int val;

        public int value() {
            return val;
        }

        public void create(int val) {
            this.val = val;
        }
    }

    public static MyStringValue createMyStringValue(String string) {
        MyStringValue val = new MyStringValue();
        val.create(string);
        return val;
    }

    public static String writeMyStringValue(MyStringValue val) {
        return val.stringValue();
    }

    public static MyIntValue createMyIntValue(int val) {
        MyIntValue obj = new MyIntValue();
        obj.create(val);
        return obj;
    }

    public static int writeMyIntValue(MyIntValue val) {
        return val.value();
    }

    public static class ContainsValue {
        MyMethodStringValue methodString;
        MyConstructorStringValue constructorString;
        MyStringValue valueString;
        List<MyMethodStringValue> listMethodString;
        MyMethodIntValue methodInt;
        MyConstructorIntValue constructorInt;
        MyIntValue valueInt;
        List<MyMethodIntValue> listMethodInt;

        public MyMethodStringValue getMethodString() {
            return methodString;
        }

        public void setMethodString(MyMethodStringValue methodString) {
            this.methodString = methodString;
        }

        public MyConstructorStringValue getConstructorString() {
            return constructorString;
        }

        public void setConstructorString(MyConstructorStringValue constructorString) {
            this.constructorString = constructorString;
        }

        public List<MyMethodStringValue> getListMethodString() {
            return listMethodString;
        }

        public void setListMethodString(List<MyMethodStringValue> listMethodString) {
            this.listMethodString = listMethodString;
        }

        public MyStringValue getValueString() {
            return valueString;
        }

        public void setValueString(MyStringValue valueString) {
            this.valueString = valueString;
        }

        public MyMethodIntValue getMethodInt() {
            return methodInt;
        }

        public void setMethodInt(MyMethodIntValue methodInt) {
            this.methodInt = methodInt;
        }

        public MyConstructorIntValue getConstructorInt() {
            return constructorInt;
        }

        public void setConstructorInt(MyConstructorIntValue constructorInt) {
            this.constructorInt = constructorInt;
        }

        public MyIntValue getValueInt() {
            return valueInt;
        }

        public void setValueInt(MyIntValue valueInt) {
            this.valueInt = valueInt;
        }

        public List<MyMethodIntValue> getListMethodInt() {
            return listMethodInt;
        }

        public void setListMethodInt(List<MyMethodIntValue> listMethodInt) {
            this.listMethodInt = listMethodInt;
        }
    }

    @Test
    public void generateClass() throws Exception {
        Generator generator = new Generator();
        myStringValueMapping(generator);
        generator.deserializer(MyConstructorStringValue.class).output(new TestClassOutput()).generate();
        generator.deserializer(MyMethodStringValue.class).output(new TestClassOutput()).generate();
        generator.deserializer(MyStringValue.class).output(new TestClassOutput()).generate();
        generator.serializer(MyStringValue.class).output(new TestClassOutput()).generate();
        generator.deserializer(MyMethodIntValue.class).output(new TestClassOutput()).generate();
        generator.deserializer(ContainsValue.class).output(new TestClassOutput()).generate();
        generator.deserializer(new GenericType<List<MyMethodStringValue>>() {}).output(new TestClassOutput()).generate();
    }

    @Test
    public void staticStringTest() throws Exception {
        QsonMapper mapper = new QsonMapper();
        myStringValueMapping(mapper);
        MyStringValue val = new MyStringValue();
        val.create("hello");
        String json = mapper.writeString(val);
        Assertions.assertEquals("\"hello\"", json);
        val = mapper.read(json, MyStringValue.class);
        Assertions.assertEquals("hello", val.stringValue());
    }

    private void myStringValueMapping(QsonGenerator mapper) throws NoSuchMethodException {
        ClassMapping mapping = mapper.mappingFor(MyStringValue.class);
        mapping.valueSetter(ValueClassTest.class.getMethod("createMyStringValue", String.class));
        mapping.valueGetter(ValueClassTest.class.getMethod("writeMyStringValue", MyStringValue.class));
    }

    @Test
    public void constructorStringTest() {
        QsonMapper mapper = new QsonMapper();
        MyConstructorStringValue val = mapper.read("\"hello\"", MyConstructorStringValue.class);
        Assertions.assertEquals("hello", val.value());
        String json = mapper.writeString(val);
        Assertions.assertEquals("\"hello\"", json);

    }

    @Test
    public void methodStringTest() {
        QsonMapper mapper = new QsonMapper();
        MyMethodStringValue val = mapper.read("\"hello\"", MyMethodStringValue.class);
        Assertions.assertEquals("hello", val.value());
        String json = mapper.writeString(val);
        Assertions.assertEquals("\"hello\"", json);
    }

    @Test
    public void staticIntTest() throws Exception {
        QsonMapper mapper = new QsonMapper();
        mapMyIntValue(mapper);
        MyIntValue val = mapper.read("42", MyIntValue.class);
        Assertions.assertEquals(42, val.value());
        String json = mapper.writeString(val);
        Assertions.assertEquals("42", json);

    }

    private void mapMyIntValue(QsonGenerator mapper) throws NoSuchMethodException {
        ClassMapping mapping = mapper.mappingFor(MyIntValue.class);
        mapping.valueSetter(ValueClassTest.class.getMethod("createMyIntValue", int.class));
        mapping.valueGetter(ValueClassTest.class.getMethod("writeMyIntValue", MyIntValue.class));
    }

    @Test
    public void constructorIntTest() {
        QsonMapper mapper = new QsonMapper();
        MyConstructorIntValue val = mapper.read("42", MyConstructorIntValue.class);
        Assertions.assertEquals(42, val.getVal());
    }

    @Test
    public void methodIntTest() {
        QsonMapper mapper = new QsonMapper();
        MyMethodIntValue val = mapper.read("42", MyMethodIntValue.class);
        Assertions.assertEquals(42, val.value());
    }


    @Test
    public void containsValue() throws Exception {
        String json = "{\n" +
                "  \"methodString\": \"methodString\",\n" +
                "  \"constructorString\": \"constructorString\",\n" +
                "  \"valueString\": \"valueString\",\n" +
                "  \"listMethodString\" : [\n" +
                "    \"mOne\",\n" +
                "    \"mTwo\"\n" +
                "  ],\n" +
                "  \"methodInt\": 1,\n" +
                "  \"constructorInt\": 2,\n" +
                "  \"valueInt\": 3,\n" +
                "  \"listMethodInt\" : [\n" +
                "    4,\n" +
                "    5" +
                "  ]\n" +
                "}";
        QsonMapper mapper = new QsonMapper();
        myStringValueMapping(mapper);
        mapMyIntValue(mapper);
        ContainsValue val = mapper.read(json, ContainsValue.class);
        test(val);
        json = mapper.writeString(val);
        val = mapper.read(json, ContainsValue.class);
        test(val);


    }

    private void test(ContainsValue val) {
        Assertions.assertEquals("methodString", val.getMethodString().value());
        Assertions.assertEquals("constructorString", val.getConstructorString().value());
        Assertions.assertEquals("valueString", val.getValueString().stringValue());
        Assertions.assertEquals("mOne", val.getListMethodString().get(0).value());
        Assertions.assertEquals("mTwo", val.getListMethodString().get(1).value());

        Assertions.assertEquals(1, val.getMethodInt().value());
        Assertions.assertEquals(2, val.getConstructorInt().getVal());
        Assertions.assertEquals(3, val.getValueInt().value());
        Assertions.assertEquals(4, val.getListMethodInt().get(0).value());
        Assertions.assertEquals(5, val.getListMethodInt().get(1).value());
    }


}
