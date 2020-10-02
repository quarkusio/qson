package io.quarkus.qson.desserializer;

public interface JsonParser {
    /**
     * Initial state
     *
     * @return
     */
    ParserState parser();

    /**
     * Get end target on successful parse
     *
     * @param ctx
     * @param <T>
     * @return
     */
    <T> T getTarget(ParserContext ctx);
}
