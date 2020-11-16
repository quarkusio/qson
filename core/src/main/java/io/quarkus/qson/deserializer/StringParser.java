package io.quarkus.qson.deserializer;

public class StringParser implements JsonParser {
    @Override
    public ParserState startState() {
        return ObjectParser.PARSER.startStringValue;
    }

    @Override
    public <T> T getTarget(ParserContext ctx) {
        return (T)ctx.popToken();
    }
}
