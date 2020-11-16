package io.quarkus.qson.deserializer;

public class LongParser implements JsonParser {
    @Override
    public ParserState startState() {
        return ObjectParser.PARSER.startIntegerValue;
    }

    @Override
    public <T> T getTarget(ParserContext ctx) {
        return (T)Long.valueOf(ctx.popLongToken());
    }
}
