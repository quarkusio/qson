package io.quarkus.qson.parser;

import io.quarkus.qson.writer.JsonByteWriter;

import java.io.IOException;
import java.io.InputStream;

public interface QsonParser {
    /**
     * Initial state
     *
     * @return
     */
    ParserState startState();

    /**
     * Get end target on successful parse
     *
     * @param ctx
     * @param <T>
     * @return
     */
    default
    <T> T getTarget(ParserContext ctx) {
        return ctx.target();
    }

    /**
     * Read object from InputStream
     *
     * @param is
     * @param <T>
     * @return
     * @throws IOException
     */
    default <T> T read(InputStream is) throws IOException {
        ByteArrayParserContext ctx = new ByteArrayParserContext(this);
        return ctx.finish(is);
    }

    /**
     * Read object from byte buffer.  Expects fully buffered json.
     *
     * @param bytes
     * @param <T>
     * @return
     * @throws IOException
     */
    default <T> T read(byte[] bytes) {
        ByteArrayParserContext ctx = new ByteArrayParserContext(this);
        return ctx.finish(bytes);
    }

    /**
     * Read object from json string.
     *
     * @param string
     * @param <T>
     * @return
     * @throws IOException
     */
    default <T> T read(String string) throws IOException {
        return read(string.getBytes(JsonByteWriter.UTF8));
    }
}
