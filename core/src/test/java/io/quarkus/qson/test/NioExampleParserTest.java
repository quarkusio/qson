package io.quarkus.qson.test;

import io.quarkus.qson.deserializer.ByteArrayParserContext;
import io.quarkus.qson.deserializer.GenericParser;
import io.quarkus.qson.deserializer.JsonParser;
import io.quarkus.qson.deserializer.StringParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class NioExampleParserTest {

    /**
     * map nonstring-key, object value, list value
     * list value, object
     */

    static String json = "{\n" +
            "  \"intMap\": {\n" +
            "    \"one\": 1,\n" +
            "    \"two\": 2\n" +
            "  },\n" +
            "  \"genericMap\": {\n" +
            "    \"three\": 3,\n" +
            "    \"four\": 4\n" +
            "  },\n" +
            "  \"name\": \"Bill\",\n" +
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


    static String arrayOnly = "{\n" +
            "  \"pets\": [ \"itchy\", \"scratchy\"]\n" +
            "}";
    @Test
    public void testNioArrayOnly() {
        List<String> breakup = breakup(arrayOnly, 1);
        ByteArrayParserContext ctx = new ByteArrayParserContext(NioPersonParser.PARSER, NioPersonParser.PARSER.startState());
        for (String str : breakup) {
            if (ctx.parse(str)) break;
        }
        Person person = ctx.finish();
        Assertions.assertTrue(person.getPets().contains("itchy"));
        Assertions.assertTrue(person.getPets().contains("scratchy"));

    }

    static String kidsOnly = "{\n" +
            "  \"kids\": {\n" +
            "    \"Sammy\": {\n" +
            "      \"name\": \"Sammy\",\n" +
            "      \"age\": 6\n" +
            "    },\n" +
            "    \"Suzi\": {\n" +
            "      \"name\": \"Suzi\",\n" +
            "      \"age\": 7\n" +
            "    }\n" +
            "  }\n" +
            "}";

    @Test
    public void testNioMapObjectOnly() {
        List<String> breakup = breakup(kidsOnly, 1);
        ByteArrayParserContext ctx = new ByteArrayParserContext(NioPersonParser.PARSER, NioPersonParser.PARSER.startState());
        for (String str : breakup) {
            if (ctx.parse(str)) break;
        }
        Person person = ctx.finish();
        Assertions.assertEquals("Sammy", person.getKids().get("Sammy").getName());
        Assertions.assertEquals(6, person.getKids().get("Sammy").getAge());
        Assertions.assertEquals("Suzi", person.getKids().get("Suzi").getName());
        Assertions.assertEquals(7, person.getKids().get("Suzi").getAge());

    }

    static String dadOnly = "{\n" +
            "  \"dad\": {\n" +
            "    \"name\": \"John\",\n" +
            "    \"married\": true\n" +
            "  }\n" +
            "}";

    @Test
    public void testNioObjectOnly() {
        List<String> breakup = breakup(dadOnly, 1);
        ByteArrayParserContext ctx = new ByteArrayParserContext(NioPersonParser.PARSER);
        for (String str : breakup) {
            if (ctx.parse(str)) break;
        }
        Person person = ctx.finish();
        Assertions.assertEquals("John", person.getDad().getName());
        Assertions.assertTrue(person.getDad().isMarried());

    }



    @Test
    public void testParser() {
        for (int i = 1; i <= json.length(); i++) {
            System.out.println("Buffer size: " + i);
            List<String> breakup = breakup(json, i);
            ByteArrayParserContext ctx = new ByteArrayParserContext(NioPersonParser.PARSER);
            for (String str : breakup) {
                if (ctx.parse(str)) break;
            }
            Person person = ctx.finish();
            validatePerson(person);

        }
        ByteArrayParserContext ctx = new ByteArrayParserContext(NioPersonParser.PARSER);
        Person person = ctx.finish(json);
        validatePerson(person);
    }

    @Test
    public void testQuotes() {
        String json = "{ \"name\": \"The \\\"Dude\\\"\" }";

        for (int i = 1; i <= json.length(); i++) {
            System.out.println("Buffer size: " + i);
            List<String> breakup = breakup(json, i);
            ByteArrayParserContext ctx = new ByteArrayParserContext(NioPersonParser.PARSER);
            for (String str : breakup) {
                if (ctx.parse(str)) break;
            }
            Person person = ctx.finish();
            Assertions.assertEquals("The \"Dude\"", person.getName());

        }
        ByteArrayParserContext ctx = new ByteArrayParserContext(NioPersonParser.PARSER);
        Person person = ctx.finish(json);
        Assertions.assertEquals("The \"Dude\"", person.getName());
    }

    @Test
    public void testStringParser() {
        String stringJson = "\"hello\"";
        StringParser stringParser = new StringParser();
        ByteArrayParserContext ctx = new ByteArrayParserContext(stringParser);
        Assertions.assertEquals("hello", ctx.finish(stringJson));
    }

    @Test
    public void testNioParser() {
        List<String> breakup = breakup(json, 7);
        ByteArrayParserContext ctx = new ByteArrayParserContext(NioPersonParser.PARSER, NioPersonParser.PARSER.startState());
        for (String str : breakup) {
            if (ctx.parse(str)) break;
        }
        System.out.println();
        Person person = ctx.finish();
        validatePerson(person);

    }
    @Test
    public void testNioParserBuffered() {
        ByteArrayParserContext ctx = new ByteArrayParserContext(NioPersonParser.PARSER, NioPersonParser.PARSER.startState());
        Person person = ctx.finish(json);
        validatePerson(person);

    }

    List<String> breakup(String str, int size) {
        List<String> breakup = new LinkedList<>();
        int i = 0;
        int len = str.length();
        while (true) {
            if (size > len - i) {
                breakup.add(str.substring(i));
                return breakup;
            }
            breakup.add(str.substring(i, i + size));
            i += size;
        }
    }

    public void validatePerson(Person person) {
        Assertions.assertEquals("Bill", person.getName());
        Assertions.assertEquals(50, person.getAge());
        Assertions.assertTrue(person.isMarried());
        Assertions.assertEquals(123.23F, person.getMoney());
        Assertions.assertEquals(1, person.getIntMap().get("one"));
        Assertions.assertEquals(2, person.getIntMap().get("two"));
        Assertions.assertEquals("Ritchie", person.getNested().get("one").get(0).getName());
        Assertions.assertEquals("Joani", person.getNested().get("one").get(1).getName());
        Assertions.assertEquals("Fonzi", person.getNested().get("two").get(0).getName());
        Assertions.assertEquals("Potsi", person.getNested().get("two").get(1).getName());
        Assertions.assertEquals(3l, person.getGenericMap().get("three"));
        Assertions.assertEquals(4l, person.getGenericMap().get("four"));
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

    static String generic = "{\n" +
            "  \"intMap\": {\n" +
            "    \"one\": 1,\n" +
            "    \"two\": 2\n" +
            "  },\n" +
            "  \"name\": \"Bill\",\n" +
            "  \"age\": 50,\n" +
            "  \"money\": 123.23,\n" +
            "  \"married\": true,\n" +
            "  \"list\": [\n" +
            "    \"one\",\n" +
            "    2,\n" +
            "    3.0,\n" +
            "    true,\n" +
            "    {\n" +
            "      \"name\": \"John\",\n" +
            "      \"married\": true\n" +
            "    }\n" +
            "  ],\n" +
            "  \"list2\": [0, 1, 2, 3   ]   ,\n" +
            "  \"dad\": {\n" +
            "    \"name\": \"John\",\n" +
            "    \"married\": true\n" +
            "  }\n" +
            "}";



    @Test
    public void testGenericParser() {
        for (int i = 1; i <= generic.length(); i++) {
            System.out.println("Buffer size: " + i);
            List<String> breakup = breakup(generic, i);
            ByteArrayParserContext ctx = new ByteArrayParserContext(GenericParser.PARSER, GenericParser.PARSER.startState());
            for (String str : breakup) {
                //System.out.println(str);
                if (ctx.parse(str)) break;
            }
            validateGeneric(ctx.finish());

        }

        ByteArrayParserContext ctx = new ByteArrayParserContext(GenericParser.PARSER, GenericParser.PARSER.startState());
        Assertions.assertTrue(ctx.parse(generic));
        validateGeneric(ctx.finish());

    }

    public void validateGeneric(Map person) {
        Assertions.assertEquals("Bill", person.get("name"));
        Assertions.assertEquals(50L, person.get("age"));
        Assertions.assertEquals(true, person.get("married"));
        Assertions.assertEquals(123.23F, person.get("money"));
        Assertions.assertEquals(1L, ((Map)person.get("intMap")).get("one"));
        Assertions.assertEquals(2L, ((Map)person.get("intMap")).get("two"));
        Assertions.assertEquals("John", ((Map)person.get("dad")).get("name"));
        Assertions.assertEquals(true, ((Map)person.get("dad")).get("married"));
        List list = (List)person.get("list");
        Assertions.assertEquals("one", list.get(0));
        Assertions.assertEquals(2L, list.get(1));
        Assertions.assertEquals(3.0F, list.get(2));
        Assertions.assertEquals(true, list.get(3));
        Assertions.assertEquals("John", ((Map)list.get(4)).get("name"));
        Assertions.assertEquals(true, ((Map)list.get(4)).get("married"));
        List list2 = (List)person.get("list2");
        Assertions.assertEquals(0L, list2.get(0));
        Assertions.assertEquals(1L, list2.get(1));
        Assertions.assertEquals(2L, list2.get(2));
        Assertions.assertEquals(3L, list2.get(3));
    }

    static String genericList = "[\n" +
            "  \"one\",\n" +
            "  2,\n" +
            "  3.0,\n" +
            "  true,\n" +
            "  {\n" +
            "    \"name\": \"John\",\n" +
            "    \"married\": true\n" +
            "  }\n" +
            "]\n";


    @Test
    public void testGenericList() {
        JsonParser p = GenericParser.PARSER;
        ByteArrayParserContext ctx = new ByteArrayParserContext(p);
        List list = ctx.finish(genericList);
        Assertions.assertEquals("one", list.get(0));
        Assertions.assertEquals(2L, list.get(1));
        Assertions.assertEquals(3.0F, list.get(2));
        Assertions.assertEquals(true, list.get(3));
        Assertions.assertEquals("John", ((Map)list.get(4)).get("name"));
        Assertions.assertEquals(true, ((Map)list.get(4)).get("married"));

    }
}
