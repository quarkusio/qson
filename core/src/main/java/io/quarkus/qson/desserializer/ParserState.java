package io.quarkus.qson.desserializer;

public interface ParserState {
    boolean parse(ParserContext ctx);
}
