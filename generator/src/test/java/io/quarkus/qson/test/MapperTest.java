package io.quarkus.qson.test;

import io.quarkus.qson.GenericType;
import io.quarkus.qson.parser.ByteArrayParserContext;
import io.quarkus.qson.parser.QsonParser;
import io.quarkus.qson.generator.QsonMapper;
import io.quarkus.qson.writer.ByteArrayJsonWriter;
import io.quarkus.qson.writer.JsonByteWriter;
import io.quarkus.qson.writer.QsonObjectWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
    public void testEnum() throws Exception {
        QsonMapper mapper = new QsonMapper();
        PojoEnum p = new PojoEnum();
        p.setColor(Color.GREEN);
        List<Color> colorList = new ArrayList<>();
        colorList.add(Color.RED);
        Map<String, Color> colorMap = new HashMap<>();
        colorMap.put("blue", Color.BLUE);
        p.setColorList(colorList);
        p.setColorMap(colorMap);
        String json = mapper.writeString(p);
        System.out.println(json);
        p = mapper.read(json, PojoEnum.class);
        Assertions.assertEquals(Color.GREEN, p.getColor());
        Assertions.assertEquals(Color.BLUE, p.getColorMap().get("blue"));
        Assertions.assertEquals(Color.RED, p.getColorList().get(0));
    }

    @Test
    public void testInteger() throws Exception {
        QsonMapper mapper = new QsonMapper();
        {
            QsonParser parser = mapper.parserFor(int.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Integer i = ctx.finish("1234");
            Assertions.assertEquals(1234, i.intValue());
        }
        {
            QsonParser parser = mapper.parserFor(Integer.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Integer i = ctx.finish("1234");
            Assertions.assertEquals(1234, i.intValue());
        }
        Assertions.assertEquals("123", mapper.writeString(Integer.valueOf((123))));
        Assertions.assertEquals("-123", mapper.writeString(Integer.valueOf((-123))));
    }

    @Test
    public void testBadInteger() throws Exception {
        QsonMapper mapper = new QsonMapper();
        QsonParser parser = mapper.parserFor(int.class);
        ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
        try {
            Integer i = ctx.finish("ABCDE");
            Assertions.fail();
        } catch (Exception e) {

        }
    }
    @Test
    public void testShort() throws Exception {
        QsonMapper mapper = new QsonMapper();
        {
            QsonParser parser = mapper.parserFor(short.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Short i = ctx.finish("1234");
            Assertions.assertEquals(1234, i.shortValue());
        }
        {
            QsonParser parser = mapper.parserFor(Short.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Short i = ctx.finish("1234");
            Assertions.assertEquals(1234, i.shortValue());
        }
        Assertions.assertEquals("123", mapper.writeString(Short.valueOf((short)123)));
        Assertions.assertEquals("-123", mapper.writeString(Short.valueOf((short)-123)));
    }

    @Test
    public void testLong() throws Exception {
        QsonMapper mapper = new QsonMapper();
        {
            QsonParser parser = mapper.parserFor(long.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Long i = ctx.finish("1234");
            Assertions.assertEquals(1234L, i.longValue());
        }
        {
            QsonParser parser = mapper.parserFor(Long.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Long i = ctx.finish("1234");
            Assertions.assertEquals(1234L, i.longValue());
        }
        Assertions.assertEquals("123", mapper.writeString(Long.valueOf(123)));
        Assertions.assertEquals("-123", mapper.writeString(Long.valueOf(-123)));
    }

    @Test
    public void testByte() throws Exception {
        QsonMapper mapper = new QsonMapper();
        {
            QsonParser parser = mapper.parserFor(byte.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Byte i = ctx.finish("123");
            Assertions.assertEquals(123, i.byteValue());
        }
        {
            QsonParser parser = mapper.parserFor(Byte.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Byte i = ctx.finish("123");
            Assertions.assertEquals(123, i.byteValue());
        }
        Assertions.assertEquals("123", mapper.writeString(Byte.valueOf((byte)123)));
        Assertions.assertEquals("-123", mapper.writeString(Byte.valueOf((byte)-123)));
    }

    @Test
    public void testFloat() throws Exception {
        QsonMapper mapper = new QsonMapper();
        {
            QsonParser parser = mapper.parserFor(float.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Float i = ctx.finish("123");
            Assertions.assertEquals(123.0f, i.floatValue());
        }
        {
            QsonParser parser = mapper.parserFor(Float.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Float i = ctx.finish("123");
            Assertions.assertEquals(123.0f, i.floatValue());
        }
        {
            QsonParser parser = mapper.parserFor(float.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Float i = ctx.finish("123.1");
            Assertions.assertEquals(123.1f, i.floatValue());
        }
        {
            QsonParser parser = mapper.parserFor(Float.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Float i = ctx.finish("123.1");
            Assertions.assertEquals(123.1f, i.floatValue());
        }
        Assertions.assertEquals("123.1", mapper.writeString(Float.valueOf(123.1f)));
        Assertions.assertEquals("-123.1", mapper.writeString(Float.valueOf(-123.1f)));
    }

    @Test
    public void testDouble() throws Exception {
        QsonMapper mapper = new QsonMapper();
        {
            QsonParser parser = mapper.parserFor(double.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Double i = ctx.finish("123");
            Assertions.assertEquals(123.0, i.doubleValue());
        }
        {
            QsonParser parser = mapper.parserFor(Double.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Double i = ctx.finish("123");
            Assertions.assertEquals(123.0, i.doubleValue());
        }
        {
            QsonParser parser = mapper.parserFor(double.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Double i = ctx.finish("123.1");
            Assertions.assertEquals(123.1, i.doubleValue());
        }
        {
            QsonParser parser = mapper.parserFor(Double.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Double i = ctx.finish("123.1");
            Assertions.assertEquals(123.1, i.doubleValue());
        }
        Assertions.assertEquals("123.1", mapper.writeString(Double.valueOf(123.1)));
        Assertions.assertEquals("-123.1", mapper.writeString(Double.valueOf(-123.1)));
    }

    @Test
    public void testBoolean() throws Exception {
        QsonMapper mapper = new QsonMapper();
        {
            QsonParser parser = mapper.parserFor(boolean.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Boolean i = ctx.finish("true");
            Assertions.assertEquals(true, i.booleanValue());
        }
        {
            QsonParser parser = mapper.parserFor(Boolean.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Boolean i = ctx.finish("true");
            Assertions.assertEquals(true, i.booleanValue());
        }
        {
            QsonParser parser = mapper.parserFor(boolean.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Boolean i = ctx.finish("false");
            Assertions.assertEquals(false, i.booleanValue());
        }
        {
            QsonParser parser = mapper.parserFor(Boolean.class);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Boolean i = ctx.finish("false");
            Assertions.assertEquals(false, i.booleanValue());
        }
        Assertions.assertEquals("true", mapper.writeString(Boolean.TRUE));
        Assertions.assertEquals("false", mapper.writeString(Boolean.FALSE));
    }

    @Test
    public void testString() throws Exception {
        QsonMapper mapper = new QsonMapper();
        QsonParser parser = mapper.parserFor(String.class);
        ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
        String str = ctx.finish("\"ABCDE\"");
        Assertions.assertEquals("ABCDE", str);
        String json = mapper.writeString(str);
        Assertions.assertEquals("\"ABCDE\"", json);
    }

    @Test
    public void testSingleEnum() throws Exception {
        QsonMapper mapper = new QsonMapper();
        QsonParser parser = mapper.parserFor(Color.class);
        ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
        Color color = ctx.finish("\"RED\"");
        Assertions.assertEquals(Color.RED, color);
        String json = mapper.writeString(Color.BLUE);
        color = mapper.read(json, Color.class);
        Assertions.assertEquals(Color.BLUE, color);
    }

    @Test
    public void testIncompleteString() throws Exception {
        QsonMapper mapper = new QsonMapper();
        QsonParser parser = mapper.parserFor(String.class);
        ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
        try {
            String str = ctx.finish("\"ABCDE");
            Assertions.fail();
        } catch (Exception e) {
        }
    }

    @Test
    public void testPersonProfile() throws Exception {
        QsonMapper mapper = new QsonMapper();
        QsonParser parser = mapper.parserFor(Person2.class);
        int ITERATIONS = 10000;
        for (int i = 0; i < ITERATIONS; i++) {
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            Person2 person = ctx.finish(json);
        }
    }

    @Test
    public void testPerson() throws Exception {
        QsonMapper mapper = new QsonMapper();
        QsonParser parser = mapper.parserFor(Person2.class);
        QsonObjectWriter objectWriter = mapper.writerFor(Person2.class);
        QsonParser parser2 = mapper.parserFor(Person2.class);
        QsonObjectWriter objectWriter2 = mapper.writerFor(Person2.class);

        // test cached
        Assertions.assertTrue(parser == parser2);
        Assertions.assertTrue(objectWriter == objectWriter2);

        ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
        Person2 person = ctx.finish(json);
        validatePerson(person);

        // serializer

        ByteArrayJsonWriter jsonWriter = new ByteArrayJsonWriter();
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
    public void testNested() throws Exception {
        QsonMapper mapper = new QsonMapper();
        QsonParser parser = mapper.parserFor(Team.class);
        QsonObjectWriter objectWriter = mapper.writerFor(Team.class);

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


        ByteArrayJsonWriter jsonWriter = new ByteArrayJsonWriter();
        objectWriter.write(jsonWriter, pats);
        byte[] bytes = jsonWriter.toByteArray();
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
    @Test
    public void testMapByteString() throws Exception {
        test("{ \"1\": \"one\", \"2\": \"two\"}",
                new GenericType<Map<Byte, String>>() {},
                (obj) -> {
                    Map<Byte, String> target = (Map<Byte, String>)obj;
                    Assertions.assertEquals("one", target.get((byte)1));
                    Assertions.assertEquals("two", target.get((byte)2));
                });
    }

    @Test
    public void testMapShortString() throws Exception {
        test("{ \"1\": \"one\", \"2\": \"two\"}",
                new GenericType<Map<Short, String>>() {},
                (obj) -> {
                    Map<Short, String> target = (Map<Short, String>)obj;
                    Assertions.assertEquals("one", target.get((short)1));
                    Assertions.assertEquals("two", target.get((short)2));
                });
    }

    @Test
    public void testMapIntegerString() throws Exception {
        test("{ \"1\": \"one\", \"2\": \"two\"}",
                new GenericType<Map<Integer, String>>() {},
                (obj) -> {
                    Map<Integer, String> target = (Map<Integer, String>)obj;
                    Assertions.assertEquals("one", target.get((int)1));
                    Assertions.assertEquals("two", target.get((int)2));
                });
    }

    @Test
    public void testMapLongString() throws Exception {
        test("{ \"1\": \"one\", \"2\": \"two\"}",
                new GenericType<Map<Long, String>>() {},
                (obj) -> {
                    Map<Long, String> target = (Map<Long, String>)obj;
                    Assertions.assertEquals("one", target.get((long)1));
                    Assertions.assertEquals("two", target.get((long)2));
                });
    }

    @Test
    public void testMapStringString() throws Exception {
        test("{ \"one\": \"1\", \"two\": \"2\"}",
                new GenericType<Map<String, String>>() {},
                (obj) -> {
                    Map<String, String> target = (Map<String, String>)obj;
                    Assertions.assertEquals("1", target.get("one"));
                    Assertions.assertEquals("2", target.get("two"));
                });
    }

    @Test
    public void testMapStringByte() throws Exception {
        test("{ \"one\": 1, \"two\": 2}",
                new GenericType<Map<String, Byte>>() {},
                (obj) -> {
                    Map<String, Byte> target = (Map<String, Byte>)obj;
                    Assertions.assertEquals(1, target.get("one").byteValue());
                    Assertions.assertEquals(2, target.get("two").byteValue());
                });
    }

    @Test
    public void testMapStringShort() throws Exception {
        test("{ \"one\": 1, \"two\": 2}",
                new GenericType<Map<String, Short>>() {},
                (obj) -> {
                    Map<String, Short> target = (Map<String, Short>)obj;
                    Assertions.assertEquals(1, target.get("one").shortValue());
                    Assertions.assertEquals(2, target.get("two").shortValue());
                });
    }

    @Test
    public void testMapStringInteger() throws Exception {
        test("{ \"one\": 1, \"two\": 2}",
                new GenericType<Map<String, Integer>>() {},
                (obj) -> {
                    Map<String, Integer> target = (Map<String, Integer>)obj;
                    Assertions.assertEquals(1, target.get("one").intValue());
                    Assertions.assertEquals(2, target.get("two").intValue());
                });
    }

    @Test
    public void testMapStringLong() throws Exception {
        test("{ \"one\": 1, \"two\": 2}",
                new GenericType<Map<String, Long>>() {},
                (obj) -> {
                    Map<String, Long> target = (Map<String, Long>)obj;
                    Assertions.assertEquals(1, target.get("one").longValue());
                    Assertions.assertEquals(2, target.get("two").longValue());
                });
    }

    @Test
    public void testMapStringFloat() throws Exception {
        test("{ \"one\": 1.1, \"two\": 2.2}",
                new GenericType<Map<String, Float>>() {},
                (obj) -> {
                    Map<String, Float> target = (Map<String, Float>)obj;
                    Assertions.assertEquals(1.1f, target.get("one").floatValue());
                    Assertions.assertEquals(2.2f, target.get("two").floatValue());
                });
    }

    @Test
    public void testMapStringDouble() throws Exception {
        test("{ \"one\": 1.1, \"two\": 2.2}",
                new GenericType<Map<String, Double>>() {},
                (obj) -> {
                    Map<String, Double> target = (Map<String, Double>)obj;
                    Assertions.assertEquals(1.1, target.get("one").doubleValue());
                    Assertions.assertEquals(2.2, target.get("two").doubleValue());
                });
    }

    @Test
    public void testListString() throws Exception {
        test("[ \"1\", \"2\"]",
                new GenericType<List<String>>() {},
                (obj) -> {
                    List<String> target = (List<String>)obj;
                    Assertions.assertEquals("1", target.get(0));
                    Assertions.assertEquals("2", target.get(1));
                });
    }

    @Test
    public void testListByte() throws Exception {
        test("[ 1, 2]",
                new GenericType<List<Byte>>() {},
                (obj) -> {
                    List<Byte> target = (List<Byte>)obj;
                    Assertions.assertEquals(1, target.get(0).byteValue());
                    Assertions.assertEquals(2, target.get(1).byteValue());
                });
    }

    @Test
    public void testListShort() throws Exception {
        test("[ 1, 2]",
                new GenericType<List<Short>>() {},
                (obj) -> {
                    List<Short> target = (List<Short>)obj;
                    Assertions.assertEquals(1, target.get(0).shortValue());
                    Assertions.assertEquals(2, target.get(1).shortValue());
                });
    }

    @Test
    public void testListInteger() throws Exception {
        test("[ 1, 2]",
                new GenericType<List<Integer>>() {},
                (obj) -> {
                    List<Integer> target = (List<Integer>)obj;
                    Assertions.assertEquals(1, target.get(0).intValue());
                    Assertions.assertEquals(2, target.get(1).intValue());
                });
    }

    @Test
    public void testListLong() throws Exception {
        test("[ 1, 2]",
                new GenericType<List<Long>>() {},
                (obj) -> {
                    List<Long> target = (List<Long>)obj;
                    Assertions.assertEquals(1, target.get(0).longValue());
                    Assertions.assertEquals(2, target.get(1).longValue());
                });
    }

    @Test
    public void testListFloat() throws Exception {
        test("[ 1.1, 2.2]",
                new GenericType<List<Float>>() {},
                (obj) -> {
                    List<Float> target = (List<Float>)obj;
                    Assertions.assertEquals(1.1f, target.get(0).floatValue());
                    Assertions.assertEquals(2.2f, target.get(1).floatValue());
                });
    }

    @Test
    public void testListDouble() throws Exception {
        test("[ 1.1, 2.2]",
                new GenericType<List<Double>>() {},
                (obj) -> {
                    List<Double> target = (List<Double>)obj;
                    Assertions.assertEquals(1.1, target.get(0).doubleValue());
                    Assertions.assertEquals(2.2, target.get(1).doubleValue());
                });
    }

    @Test
    public void testListBoolean() throws Exception {
        test("[ true, false]",
                new GenericType<List<Boolean>>() {},
                (obj) -> {
                    List<Boolean> target = (List<Boolean>)obj;
                    Assertions.assertEquals(true, target.get(0).booleanValue());
                    Assertions.assertEquals(false, target.get(1).booleanValue());
                });
    }

    @Test
    public void testEmpty() throws Exception {
        test("{}", new GenericType<Person2>() {},
                (obj) -> {
                    Person2 target = (Person2)obj;
                    Assertions.assertNotNull(target);
                });
        test("{}", new GenericType<Map<String, String>>() {},
                (obj) -> {
                    Map<String, String> target = (Map<String, String>)obj;
                    Assertions.assertTrue(target.isEmpty());
                });
        test("[]", new GenericType<List<String>>() {},
                (obj) -> {
                    List<String> target = (List<String>)obj;
                    Assertions.assertTrue(target.isEmpty());
                });
    }


    private void test(String json, GenericType type, Consumer assertions) {
        QsonMapper mapper = new QsonMapper();
        QsonParser parser = mapper.parserFor(type);
        Object target = mapper.read(json, type);
        assertions.accept(target);

        for (int i = 1; i <= json.length(); i++) {
            List<String> breakup = breakup(json, i);
            ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
            for (String str : breakup) {
                if (ctx.parse(str)) break;
            }
            target = ctx.finish();
            assertions.accept(target);
        }

        byte[] bytes = mapper.writeBytes(type, target);
        target = mapper.read(bytes, type);
        assertions.accept(target);
    }

}
