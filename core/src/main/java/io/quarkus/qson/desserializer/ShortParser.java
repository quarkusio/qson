package io.quarkus.qson.desserializer;

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
