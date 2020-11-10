package io.quarkus.qson.test;

import io.quarkus.qson.desserializer.ByteArrayParserContext;
import io.quarkus.qson.desserializer.JsonParser;
import io.quarkus.qson.generator.JsonMapper;
import io.quarkus.qson.serializer.ByteArrayByteWriter;
import io.quarkus.qson.serializer.JsonByteWriter;
import io.quarkus.qson.serializer.ObjectWriter;
import io.quarkus.runtime.QuarkusApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MapperTest {

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
    public void testInteger() throws Exception {
        JsonMapper mapper = new JsonMapper();
        {
            JsonParser parser = mapper.parserFor(int.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Integer i = ctx.finish("1234");
            Assertions.assertEquals(1234, i.intValue());
        }
        {
            JsonParser parser = mapper.parserFor(Integer.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Integer i = ctx.finish("1234");
            Assertions.assertEquals(1234, i.intValue());
        }
    }

    @Test
    public void testBadInteger() throws Exception {
        JsonMapper mapper = new JsonMapper();
        JsonParser parser = mapper.parserFor(int.class);
        ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
        try {
            Integer i = ctx.finish("ABCDE");
            Assertions.fail();
        } catch (Exception e) {

        }
    }
    @Test
    public void testShort() throws Exception {
        JsonMapper mapper = new JsonMapper();
        {
            JsonParser parser = mapper.parserFor(short.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Short i = ctx.finish("1234");
            Assertions.assertEquals(1234, i.shortValue());
        }
        {
            JsonParser parser = mapper.parserFor(Short.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Short i = ctx.finish("1234");
            Assertions.assertEquals(1234, i.shortValue());
        }
    }

    @Test
    public void testLong() throws Exception {
        JsonMapper mapper = new JsonMapper();
        {
            JsonParser parser = mapper.parserFor(long.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Long i = ctx.finish("1234");
            Assertions.assertEquals(1234L, i.longValue());
        }
        {
            JsonParser parser = mapper.parserFor(Long.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Long i = ctx.finish("1234");
            Assertions.assertEquals(1234L, i.longValue());
        }
    }

    @Test
    public void testByte() throws Exception {
        JsonMapper mapper = new JsonMapper();
        {
            JsonParser parser = mapper.parserFor(byte.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Byte i = ctx.finish("123");
            Assertions.assertEquals(123, i.byteValue());
        }
        {
            JsonParser parser = mapper.parserFor(Byte.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Byte i = ctx.finish("123");
            Assertions.assertEquals(123, i.byteValue());
        }
    }

    @Test
    public void testFloat() throws Exception {
        JsonMapper mapper = new JsonMapper();
        {
            JsonParser parser = mapper.parserFor(float.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Float i = ctx.finish("123");
            Assertions.assertEquals(123.0f, i.floatValue());
        }
        {
            JsonParser parser = mapper.parserFor(Float.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Float i = ctx.finish("123");
            Assertions.assertEquals(123.0f, i.floatValue());
        }
        {
            JsonParser parser = mapper.parserFor(float.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Float i = ctx.finish("123.1");
            Assertions.assertEquals(123.1f, i.floatValue());
        }
        {
            JsonParser parser = mapper.parserFor(Float.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Float i = ctx.finish("123.1");
            Assertions.assertEquals(123.1f, i.floatValue());
        }
    }

    @Test
    public void testDouble() throws Exception {
        JsonMapper mapper = new JsonMapper();
        {
            JsonParser parser = mapper.parserFor(double.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Double i = ctx.finish("123");
            Assertions.assertEquals(123.0, i.doubleValue());
        }
        {
            JsonParser parser = mapper.parserFor(Double.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Double i = ctx.finish("123");
            Assertions.assertEquals(123.0, i.doubleValue());
        }
        {
            JsonParser parser = mapper.parserFor(double.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Double i = ctx.finish("123.1");
            Assertions.assertEquals(123.1, i.doubleValue());
        }
        {
            JsonParser parser = mapper.parserFor(Double.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Double i = ctx.finish("123.1");
            Assertions.assertEquals(123.1, i.doubleValue());
        }
    }

    @Test
    public void testBoolean() throws Exception {
        JsonMapper mapper = new JsonMapper();
        {
            JsonParser parser = mapper.parserFor(boolean.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Boolean i = ctx.finish("true");
            Assertions.assertEquals(true, i.booleanValue());
        }
        {
            JsonParser parser = mapper.parserFor(Boolean.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Boolean i = ctx.finish("true");
            Assertions.assertEquals(true, i.booleanValue());
        }
        {
            JsonParser parser = mapper.parserFor(boolean.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Boolean i = ctx.finish("false");
            Assertions.assertEquals(false, i.booleanValue());
        }
        {
            JsonParser parser = mapper.parserFor(Boolean.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Boolean i = ctx.finish("false");
            Assertions.assertEquals(false, i.booleanValue());
        }
    }

    @Test
    public void testString() throws Exception {
        JsonMapper mapper = new JsonMapper();
        JsonParser parser = mapper.parserFor(String.class);
        ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
        String str = ctx.finish("\"ABCDE\"");
        Assertions.assertEquals("ABCDE", str);
    }

    @Test
    public void testIncompleteString() throws Exception {
        JsonMapper mapper = new JsonMapper();
        JsonParser parser = mapper.parserFor(String.class);
        ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
        try {
            String str = ctx.finish("\"ABCDE");
            Assertions.fail();
        } catch (Exception e) {
        }
    }

    @Test
    public void testPerson() throws Exception {
        JsonMapper mapper = new JsonMapper();
        JsonParser parser = mapper.parserFor(Person2.class);
        ObjectWriter objectWriter = mapper.writerFor(Person2.class);
        JsonParser parser2 = mapper.parserFor(Person2.class);
        ObjectWriter objectWriter2 = mapper.writerFor(Person2.class);

        // test cached
        Assertions.assertTrue(parser == parser2);
        Assertions.assertTrue(objectWriter == objectWriter2);

        ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
        Person2 person = ctx.finish(json);
        validatePerson(person);

        // serializer

        ByteArrayByteWriter writer = new ByteArrayByteWriter();
        JsonByteWriter jsonWriter = new JsonByteWriter(writer);
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
    public void testNested() throws Exception {
        JsonMapper mapper = new JsonMapper();
        JsonParser parser = mapper.parserFor(Team.class);
        ObjectWriter objectWriter = mapper.writerFor(Team.class);

        Team pats = new Team();
        pats.setName("Patriots 2018");
        Player tb12 = new Player();
        tb12.setName("Tom");
        Player gronk = new Player();
        gronk.setName("Gronk");
        Player jules = new Player();
        jules.setName("Julien");
        List<Player> receivers = new LinkedList<>();
        receivers.add(gronk);
        receivers.add(jules);
        Map<String, Player> players = new HashMap<>();
        players.put("Gronk", gronk);
        players.put("Julien", jules);
        players.put("Tom", tb12);
        pats.setQuarterback(tb12);
        pats.setReceivers(receivers);
        pats.setPlayers(players);


        ByteArrayByteWriter writer = new ByteArrayByteWriter();
        JsonByteWriter jsonWriter = new JsonByteWriter(writer);
        objectWriter.write(jsonWriter, pats);
        byte[] bytes = writer.getBytes();
        ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
        Team team = ctx.finish(bytes);
        Assertions.assertEquals("Patriots 2018", team.getName());
        Assertions.assertEquals("Tom", team.getQuarterback().getName());
        Assertions.assertEquals("Gronk", team.getReceivers().get(0).getName());
        Assertions.assertEquals("Julien", team.getReceivers().get(1).getName());
        Assertions.assertEquals("Tom", team.getPlayers().get("Tom").getName());
        Assertions.assertEquals("Gronk", team.getPlayers().get("Gronk").getName());
        Assertions.assertEquals("Julien", team.getPlayers().get("Julien").getName());

    }

}
