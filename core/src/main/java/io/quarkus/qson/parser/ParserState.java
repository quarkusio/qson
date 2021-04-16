package io.quarkus.qson.parser;

public interface ParserState {
    boolean parse(ParserContext ctx);
}
