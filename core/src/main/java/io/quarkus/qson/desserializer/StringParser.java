package io.quarkus.qson.desserializer;

public class StringParser implements JsonParser {
    @Override
    public ParserState parser() {
        return ObjectParser.PARSER.startStringValue;
    }

    @Override
    public <T> T getTarget(ParserContext ctx) {
        return (T)ctx.popToken();
    }
}
