package io.quarkus.qson.test;

import io.quarkus.qson.GenericType;
import io.quarkus.qson.parser.ByteArrayParserContext;
import io.quarkus.qson.parser.QsonParser;
import io.quarkus.qson.generator.Generator;
import io.quarkus.qson.generator.QsonMapper;
import io.quarkus.qson.generator.WriterGenerator;
import io.quarkus.qson.generator.ParserGenerator;
import io.quarkus.qson.writer.ByteArrayJsonWriter;
import io.quarkus.qson.writer.JsonByteWriter;
import io.quarkus.qson.writer.QsonObjectWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class NioGeneratorTest {

    @Test
    public void testDeserializer() throws Exception {
        Generator generator = new Generator();
        generator.writer(Color.class).output(new TestClassOutput()).generate();
    }

    @Test
    public void testRawCollection() throws Exception {
        GenericType<List<Person2>> type = new GenericType<List<Person2>>() {
        };
        Generator generator = new Generator();
        ParserGenerator.Builder builder = generator.parser(type).output(new TestClassOutput());
        builder.generate();
        WriterGenerator.Builder sBuilder = generator.writer(type).output(new TestClassOutput());
        sBuilder.generate();
    }

    @Test
    public void testRawCollectionParsing() throws Exception {
        QsonMapper mapper = new QsonMapper();
        {
            Map<String, List<Person2>> map = new HashMap<>();
            List<Person2> list = new LinkedList<>();
            Person2 bb = new Person2();
            bb.setName("bill");
            list.add(bb);
            map.put("bb", list);

            ByteArrayJsonWriter jsonWriter = new ByteArrayJsonWriter();
            GenericType<Map<String, List<Person2>>> type = new GenericType<Map<String, List<Person2>>>() {
            };
            QsonObjectWriter objectWriter = mapper.writerFor(type);
            objectWriter.write(jsonWriter, map);

            byte[] bytes = jsonWriter.toByteArray();
            System.out.println(new String(bytes, JsonByteWriter.UTF8));


            QsonParser parser = mapper.parserFor(type);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            map = ctx.finish(bytes);
            Assertions.assertEquals("bill", map.get("bb").get(0).getName());
        }

        {
            List<Person2> list = new LinkedList<>();
            Person2 bb = new Person2();
            bb.setName("bill");
            list.add(bb);

            ByteArrayJsonWriter jsonWriter = new ByteArrayJsonWriter();
            GenericType<List<Person2>> type = new GenericType<List<Person2>>() {
            };
            QsonObjectWriter objectWriter = mapper.writerFor(type);
            objectWriter.write(jsonWriter, list);

            byte[] bytes = jsonWriter.toByteArray();
            System.out.println(new String(bytes, JsonByteWriter.UTF8));


            QsonParser parser = mapper.parserFor(type);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            list = ctx.finish(bytes);
            Assertions.assertEquals("bill", list.get(0).getName());
        }

        {
            List<Long> list = new LinkedList<>();
            list.add(42L);
            ByteArrayJsonWriter jsonWriter = new ByteArrayJsonWriter();
            GenericType<List<Long>> type = new GenericType<List<Long>>() {
            };
            QsonObjectWriter objectWriter = mapper.writerFor(type);
            objectWriter.write(jsonWriter, list);

            byte[] bytes = jsonWriter.toByteArray();
            System.out.println(new String(bytes, JsonByteWriter.UTF8));


            QsonParser parser = mapper.parserFor(type);
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
        QsonMapper mapper = new QsonMapper();
        QsonParser parser = mapper.parserFor(Single.class);
        ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
        Assertions.assertTrue(ctx.parse(simpleJson));
        Single single = ctx.popTarget();
        Assertions.assertEquals(1, single.getName());
    }
    @Test
    public void testSimple() throws Exception {
        QsonMapper mapper = new QsonMapper();
        QsonParser parser = mapper.parserFor(Simple.class);
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
    public void testAny() throws Exception {
        QsonMapper mapper = new QsonMapper();
        QsonParser parser = mapper.parserFor(PersonAny.class);
        ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
        PersonAny person = ctx.finish(json);
        validateAny(person);

        ByteArrayJsonWriter jsonWriter = new ByteArrayJsonWriter();
        QsonObjectWriter objectWriter = mapper.writerFor(PersonAny.class);
        objectWriter.write(jsonWriter, person);

        byte[] bytes = jsonWriter.toByteArray();
        System.out.println(new String(bytes, JsonByteWriter.UTF8));

        ctx = new ByteArrayParserContext(parser);
        person = ctx.finish(bytes);
        validateAny(person);

    }

    public void validateAny(PersonAny person) {
        validatePerson(person);
        Map<String, Object> any = person.getAny();
        List<String> junkList = (List<String>)any.get("junkList");
        Assertions.assertEquals("1", junkList.get(0));
        Assertions.assertEquals("2", junkList.get(1));
        Map<String, Object> junkMap = (Map<String, Object>)any.get("junkMap");
        Assertions.assertEquals("bar", junkMap.get("foo"));
        Assertions.assertEquals(1, (Long)junkMap.get("one"));
        Assertions.assertTrue((Boolean)any.get("junkBool"));
        Assertions.assertEquals(666, (Long)any.get("junkInt"));

    }


    @Test
    public void testPerson() throws Exception {
        QsonMapper mapper = new QsonMapper();
        QsonParser parser = mapper.parserFor(Person2.class);
        ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
        Person2 person = ctx.finish(json);
        validatePerson(person);

        // serializer

        ByteArrayJsonWriter jsonWriter = new ByteArrayJsonWriter();
        QsonObjectWriter objectWriter = mapper.writerFor(Person2.class);
        objectWriter.write(jsonWriter, person);

        byte[] bytes = jsonWriter.toByteArray();
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
        QsonMapper mapper = new QsonMapper();
        QsonParser parser = mapper.parserFor(Person2.class);
        QsonObjectWriter objectWriter = mapper.writerFor(Person2.class);

        String json = "{ \"name\": \"The \\\"Dude\\\"\" }";
        String expected = "The \"Dude\"";

        testEscapes(parser, objectWriter, json, expected);
    }

    private void testEscapes(QsonParser parser, QsonObjectWriter objectWriter, String json, String expected) {
        ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
        Person2 person = ctx.finish(json);
        Assertions.assertEquals(expected, person.getName());

        // serializer

        ByteArrayJsonWriter jsonWriter = new ByteArrayJsonWriter();
        objectWriter.write(jsonWriter, person);

        byte[] bytes = jsonWriter.toByteArray();
        System.out.println(new String(bytes, JsonByteWriter.UTF8));

        // validate serializer

        ctx = new ByteArrayParserContext(parser);
        person = ctx.finish(bytes);
        Assertions.assertEquals(expected, person.getName());
    }
}
