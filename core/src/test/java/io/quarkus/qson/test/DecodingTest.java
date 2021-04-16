package io.quarkus.qson.test;

import io.quarkus.qson.parser.ParsePrimitives;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;

public class DecodingTest {

    @Test
    public void testDecoding() throws Exception{
        String string = "hello";
        decode("hello", "hello");
        decode("hello\\nworld", "hello\nworld");
        decode("hello\\\"world\\\"", "hello\"world\"");
    }

    private void decode(String string, String expected) throws UnsupportedEncodingException {
        byte[] bytes = string.getBytes("UTF-8");
        String decoded = ParsePrimitives.readString(bytes, 0, bytes.length);
        Assertions.assertEquals(expected, decoded);
    }
}
