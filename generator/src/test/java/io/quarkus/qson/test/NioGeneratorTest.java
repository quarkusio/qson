package io.quarkus.qson.test;

import io.quarkus.qson.GenericType;
import io.quarkus.qson.desserializer.ByteArrayParserContext;
import io.quarkus.qson.desserializer.JsonParser;
import io.quarkus.qson.generator.JsonMapper;
import io.quarkus.qson.generator.Serializer;
import io.quarkus.qson.generator.Deserializer;
import io.quarkus.qson.serializer.ByteArrayByteWriter;
import io.quarkus.qson.serializer.JsonByteWriter;
import io.quarkus.qson.serializer.ObjectWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class NioGeneratorTest {

    @Test
    public void testDeserializer() throws Exception {
        Deserializer.create(Simple.class).output(new TestClassOutput()).generate();
        Deserializer.create(Single.class).output(new TestClassOutput()).generate();
        Deserializer.create(Person2.class).output(new TestClassOutput()).generate();
        Serializer.create(Single.class).output(new TestClassOutput()).generate();
        Serializer.create(Person2.class).output(new TestClassOutput()).generate();
    }

    @Test
    public void testRawCollection() throws Exception {
        GenericType<List<Person2>> type = new GenericType<>() {
        };
        Deserializer.Builder builder = Deserializer.create(type.getRawType(), type.getType()).output(new TestClassOutput());
        builder.generate();
        Serializer.Builder sBuilder = Serializer.create(type.getRawType(), type.getType()).output(new TestClassOutput());
        sBuilder.generate();
    }

    @Test
    public void testRawCollectionParsing() throws Exception {
        JsonMapper mapper = new JsonMapper();
        {
            Map<String, List<Person2>> map = new HashMap<>();
            List<Person2> list = new LinkedList<>();
            Person2 bb = new Person2();
            bb.setName("bill");
            list.add(bb);
            map.put("bb", list);

            ByteArrayByteWriter writer = new ByteArrayByteWriter();
            JsonByteWriter jsonWriter = new JsonByteWriter(writer);
            GenericType<Map<String, List<Person2>>> type = new GenericType<>() {
            };
            ObjectWriter objectWriter = mapper.writerFor(type);
            objectWriter.write(jsonWriter, map);

            byte[] bytes = writer.getBytes();
            System.out.println(new String(bytes, JsonByteWriter.UTF8));


            JsonParser parser = mapper.parserFor(type);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            map = ctx.finish(bytes);
            Assertions.assertEquals("bill", map.get("bb").get(0).getName());
        }

        {
            List<Person2> list = new LinkedList<>();
            Person2 bb = new Person2();
            bb.setName("bill");
            list.add(bb);

            ByteArrayByteWriter writer = new ByteArrayByteWriter();
            JsonByteWriter jsonWriter = new JsonByteWriter(writer);
            GenericType<List<Person2>> type = new GenericType<>() {
            };
            ObjectWriter objectWriter = mapper.writerFor(type);
            objectWriter.write(jsonWriter, list);

            byte[] bytes = writer.getBytes();
            System.out.println(new String(bytes, JsonByteWriter.UTF8));


            JsonParser parser = mapper.parserFor(type);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            list = ctx.finish(bytes);
            Assertions.assertEquals("bill", list.get(0).getName());
        }

        {
            List<Long> list = new LinkedList<>();
            list.add(42L);
            ByteArrayByteWriter writer = new ByteArrayByteWriter();
            JsonByteWriter jsonWriter = new JsonByteWriter(writer);
            GenericType<List<Long>> type = new GenericType<>() {
            };
            ObjectWriter objectWriter = mapper.writerFor(type);
            objectWriter.write(jsonWriter, list);

            byte[] bytes = writer.getBytes();
            System.out.println(new String(bytes, JsonByteWriter.UTF8));


            JsonParser parser = mapper.parserFor(type);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            list = ctx.finish(bytes);
            Assertions.assertEquals(42L, ((Long)list.get(0)).longValue());
        }


    }

    static String simpleJson = "{\n" +
            "  \"name\": 1,\n" +
            "  \"age\" : 2,\n" +
            "  \"money\": 3,\n" +
            "  \"married\": 4,\n" +
            "  \"q\": 5,\n" +
            "  \"qq\": 6,\n" +
            "  \"qqq\": 7\n" +
            "}\n";
    @Test
    public void testSingle() throws Exception {
        JsonMapper mapper = new JsonMapper();
        JsonParser parser = mapper.parserFor(Single.class);
        ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
        Assertions.assertTrue(ctx.parse(simpleJson));
        Single single = ctx.popTarget();
        Assertions.assertEquals(1, single.getName());
    }
    @Test
    public void testSimple() throws Exception {
        JsonMapper mapper = new JsonMapper();
        JsonParser parser = mapper.parserFor(Simple.class);
        ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
        Assertions.assertTrue(ctx.parse(simpleJson));
        Simple simple = ctx.popTarget();
        Assertions.assertEquals(1, simple.getName());
        Assertions.assertEquals(2, simple.getAge());
        Assertions.assertEquals(3, simple.getMoney());
        Assertions.assertEquals(4, simple.getMarried());
        Assertions.assertEquals(5, simple.getQ());
        Assertions.assertEquals(6, simple.getQq());
        Assertions.assertEquals(7, simple.getQqq());
    }

    static String json = "{\n" +
            "  \"intMap\": {\n" +
            "    \"one\": 1,\n" +
            "    \"two\": 2\n" +
            "  },\n" +
            "  \"genericMap\": {\n" +
            "    \"three\": 3,\n" +
            "    \"four\": 4\n" +
            "  },\n" +
            "  \"genericList\": [\n" +
            "    \"a\",\n" +
            "    \"b\"\n" +
            "  ],\n" +
            "  \"name\": \"Bill\",\n" +
            " \"nested\": {\n" +
            "  \"one\": [\n" +
            "    {\n" +
            "      \"name\": \"Ritchie\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Joani\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"two\": [\n" +
            "    {\n" +
            "      \"name\": \"Fonzi\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Potsi\"\n" +
            "    }\n" +
            "  ]\n" +
            "},\n" +
            "  \"age\": 50,\n" +
            "  \"money\": 123.23,\n" +
            "  \"married\": true,\n" +
            "  \"junkInt\": 666,\n" +
            "  \"pets\": [ \"itchy\", \"scratchy\"],\n" +
            "  \"junkFloat\": 6.66,\n" +
            "  \"kids\": {\n" +
            "    \"Sammy\": {\n" +
            "      \"name\": \"Sammy\",\n" +
            "      \"age\": 6\n" +
            "    },\n" +
            "    \"Suzi\": {\n" +
            "      \"name\": \"Suzi\",\n" +
            "      \"age\": 7\n" +
            "    }\n" +
            "  },\n" +
            "  \"siblings\": [\n" +
            "    {\n" +
            "      \"name\": \"Ritchie\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Joani\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"junkList\": [\"1\", \"2\"]," +
            "  \"junkBool\": true,\n" +
            "  \"junkMap\": {\n" +
            "    \"foo\": \"bar\",\n" +
            "    \"one\": 1,\n" +
            "    \"list\": [1, 2, 3, 4]\n" +
            "  },\n" +
            "  \"dad\": {\n" +
            "    \"name\": \"John\",\n" +
            "    \"married\": true\n" +
            "  }\n" +
            "}";


    @Test
    public void testPerson() throws Exception {
        JsonMapper mapper = new JsonMapper();
        JsonParser parser = mapper.parserFor(Person2.class);
        ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
        Person2 person = ctx.finish(json);
        validatePerson(person);

        // serializer

        ByteArrayByteWriter writer = new ByteArrayByteWriter();
        JsonByteWriter jsonWriter = new JsonByteWriter(writer);
        ObjectWriter objectWriter = mapper.writerFor(Person2.class);
        objectWriter.write(jsonWriter, person);

        byte[] bytes = writer.getBytes();
        System.out.println(new String(bytes, JsonByteWriter.UTF8));

        // validate serializer

        ctx = new ByteArrayParserContext(parser);
        person = ctx.finish(bytes);
        validatePerson(person);


    }

    public void validatePerson(Person2 person) {
        Assertions.assertEquals("Bill", person.getName());
        Assertions.assertEquals(50, person.getAge());
        Assertions.assertTrue(person.isMarried());
        Assertions.assertEquals(123.23F, person.getMoney());
        Assertions.assertEquals(1, person.getIntMap().get("one"));
        Assertions.assertEquals(2, person.getIntMap().get("two"));
        Assertions.assertEquals("a", person.getGenericList().get(0));
        Assertions.assertEquals("b", person.getGenericList().get(1));
        Assertions.assertEquals(3l, person.getGenericMap().get("three"));
        Assertions.assertEquals(4l, person.getGenericMap().get("four"));
        Assertions.assertEquals("Ritchie", person.getNested().get("one").get(0).getName());
        Assertions.assertEquals("Joani", person.getNested().get("one").get(1).getName());
        Assertions.assertEquals("Fonzi", person.getNested().get("two").get(0).getName());
        Assertions.assertEquals("Potsi", person.getNested().get("two").get(1).getName());
        Assertions.assertEquals("John", person.getDad().getName());
        Assertions.assertTrue(person.getDad().isMarried());
        Assertions.assertEquals("Sammy", person.getKids().get("Sammy").getName());
        Assertions.assertEquals(6, person.getKids().get("Sammy").getAge());
        Assertions.assertEquals("Suzi", person.getKids().get("Suzi").getName());
        Assertions.assertEquals(7, person.getKids().get("Suzi").getAge());
        Assertions.assertEquals("Ritchie", person.getSiblings().get(0).getName());
        Assertions.assertEquals("Joani", person.getSiblings().get(1).getName());
        Assertions.assertTrue(person.getPets().contains("itchy"));
        Assertions.assertTrue(person.getPets().contains("scratchy"));
    }

    @Test
    public void testEscapes() throws Exception {
        JsonMapper mapper = new JsonMapper();
        JsonParser parser = mapper.parserFor(Person2.class);
        ObjectWriter objectWriter = mapper.writerFor(Person2.class);

        String json = "{ \"name\": \"The \\\"Dude\\\"\" }";
        String expected = "The \"Dude\"";

        testEscapes(parser, objectWriter, json, expected);
    }

    private void testEscapes(JsonParser parser, ObjectWriter objectWriter, String json, String expected) {
        ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
        Person2 person = ctx.finish(json);
        Assertions.assertEquals(expected, person.getName());

        // serializer

        ByteArrayByteWriter writer = new ByteArrayByteWriter();
        JsonByteWriter jsonWriter = new JsonByteWriter(writer);
        objectWriter.write(jsonWriter, person);

        byte[] bytes = writer.getBytes();
        System.out.println(new String(bytes, JsonByteWriter.UTF8));

        // validate serializer

        ctx = new ByteArrayParserContext(parser);
        person = ctx.finish(bytes);
        Assertions.assertEquals(expected, person.getName());
    }
}
