package io.quarkus.qson.test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.qson.deserializer.ByteArrayParserContext;
import io.quarkus.qson.deserializer.GenericParser;
import io.quarkus.qson.serializer.ByteArrayJsonWriter;
import io.quarkus.qson.serializer.GenericObjectWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static io.quarkus.qson.serializer.JsonByteWriter.UTF8;

public class TestCharEscaping
{
    private void _testSimpleEscaping(String json, String expected) {
        ByteArrayParserContext ctx = new ByteArrayParserContext(GenericParser.PARSER, GenericParser.PARSER.startState());
        List list = ctx.finish(json);
        Assertions.assertEquals(expected, list.get(0));

    }

    @Test
    public void testSimpleEscaping() throws Exception {
        _testSimpleEscaping("[\"LF=\\n\"]", "LF=\n");
        _testSimpleEscaping("[\"NULL:\\u0000!\"]", "NULL:\0!");
        _testSimpleEscaping("[\"\\u0123\"]", "\u0123");
        _testSimpleEscaping("[\"\\u0041\\u0043\"]", "AC");
    }

    @Test
    public void testSimpleNameEscaping() throws Exception {
        ByteArrayParserContext ctx = new ByteArrayParserContext(GenericParser.PARSER, GenericParser.PARSER.startState());
        Map<String, Object> map = ctx.finish("{\"hello\\\"\" : 42}");
        Long l = (Long)map.get("hello\"");
        Assertions.assertEquals(42L, l.longValue());
    }

    @Test
    public void testInvalid() throws Exception
    {
        // 2-char sequences not allowed:
        String DOC = "[\"\\u41=A\"]";
        ByteArrayParserContext ctx = new ByteArrayParserContext(GenericParser.PARSER, GenericParser.PARSER.startState());
        try {
            ctx.parse(DOC);
            Assertions.fail();
        } catch (Exception e) {
            // TODO correct exception
        }
    }


    /**
     * Test to verify that decoder does not allow 8-digit escapes
     * (non-BMP characters must be escaped using two 4-digit sequences)
     */
    @Test
    public void test8DigitSequence() throws Exception {
        String DOC = "[\"\\u00411234\"]";
        ByteArrayParserContext ctx = new ByteArrayParserContext(GenericParser.PARSER);
        List l = ctx.finish(DOC);
        Assertions.assertEquals("A1234", l.get(0));
    }
    @Test
    public void testInvalidEscape() throws Exception
    {
        String DOC = "\"\\u\u0080...\"";
        ByteArrayParserContext ctx = new ByteArrayParserContext(GenericParser.PARSER);
        try {
            ctx.parse(DOC);
            Assertions.fail();
        } catch (Exception e) {
            // TODO correct exception
        }
    }

    @Test
    public void testEscapeNonLatin() throws Exception {
        GenericObjectWriter objectWriter = new GenericObjectWriter();
        ByteArrayJsonWriter writer = new ByteArrayJsonWriter();
        String target = "Line\u2028feed, \u00D6l!";
        objectWriter.write(writer, target);
        String json = new String(writer.getBytes(), UTF8);
        Assertions.assertEquals("\"" + target + "\"", json);
    }

    private String generateRandom(int len)
    {
        StringBuilder sb = new StringBuilder(len+1000); // pad for surrogates
        Random r = new Random(len);
        for (int i = 0; i < len; ++i) {
            if (r.nextBoolean()) { // non-ascii
                int value = r.nextInt() & 0xFFFF;
                // Otherwise easy, except that need to ensure that
                // surrogates are properly paired: and, also
                // their values do not exceed 0x10FFFF
                if (value >= 0xD800 && value <= 0xDFFF) {
                    // Let's discard first value, then, and produce valid pair
                    int fullValue = (r.nextInt() & 0xFFFFF);
                    sb.append((char) (0xD800 + (fullValue >> 10)));
                    value = 0xDC00 + (fullValue & 0x3FF);
                }
                sb.append((char) value);
            } else { // ascii
                sb.append((char) (r.nextInt() & 0x7F));
            }
        }
        return sb.toString();
    }

    @Test
    public void testRandom() throws Exception {
        GenericObjectWriter objectWriter = new GenericObjectWriter();
        ByteArrayJsonWriter jsonWriter = new ByteArrayJsonWriter();

        String target = generateRandom(1000);
        objectWriter.write(jsonWriter, target);
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, false);

        // We assume Jackson is correct.  This tests our writer against Jackson
        String jackson = mapper.readValue(jsonWriter.getBytes(), String.class);
        Assertions.assertEquals(target, jackson);

        // now test our parser
        ByteArrayParserContext ctx = new ByteArrayParserContext(GenericParser.PARSER, GenericParser.PARSER.startState());
        String value = ctx.finish(jsonWriter.getBytes());
        Assertions.assertEquals(target, value);
    }

    @Test
    public void testRandomOne() throws Exception {
        String target = generateRandom(75000);
        char[] chars = target.toCharArray();

        for (char c : chars) {
            try {
                testOneChar(c);
            } catch (Throwable e) {
                throw new RuntimeException("Failed on: " + ((int)c));
            }
        }
    }

    private void testOneChar(char c) {
        ByteArrayJsonWriter jsonWriter = new ByteArrayJsonWriter();
        jsonWriter.write(c);
        ByteArrayParserContext ctx = new ByteArrayParserContext(GenericParser.PARSER, GenericParser.PARSER.startState());
        String value = ctx.finish(jsonWriter.getBytes());
        Assertions.assertEquals(Character.toString(c), value);
    }

    @Test
    public void testOne() throws Exception {
        testOneChar((char)1129);
    }


}