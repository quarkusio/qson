package io.quarkus.qson.deserializer;

public class ShortParser implements JsonParser {
    @Override
    public ParserState startState() {
        return ObjectParser.PARSER.startIntegerValue;
    }

    @Override
    public <T> T getTarget(ParserContext ctx) {
        return (T)Short.valueOf(ctx.popShortToken());
    }
}
