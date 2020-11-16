package io.quarkus.qson.deserializer;

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
    default
    <T> T getTarget(ParserContext ctx) {
        return ctx.target();
    }
}
