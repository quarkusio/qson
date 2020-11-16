package io.quarkus.qson.deserializer;

public interface ParserState {
    boolean parse(ParserContext ctx);
}
