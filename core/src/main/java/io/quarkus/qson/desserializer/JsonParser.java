package io.quarkus.qson.desserializer;

public interface JsonParser {
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
    <T> T getTarget(ParserContext ctx);
}
