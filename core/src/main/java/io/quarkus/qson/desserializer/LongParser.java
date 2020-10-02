package io.quarkus.qson.desserializer;

public class LongParser implements JsonParser {
    @Override
    public ParserState parser() {
        return ObjectParser.PARSER.startIntegerValue;
    }

    @Override
    public <T> T getTarget(ParserContext ctx) {
        return (T)Long.valueOf(ctx.popLongToken());
    }
}
