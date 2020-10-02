package io.quarkus.qson.desserializer;

public class ShortParser implements JsonParser {
    @Override
    public ParserState parser() {
        return ObjectParser.PARSER.startIntegerValue;
    }

    @Override
    public <T> T getTarget(ParserContext ctx) {
        return (T)Integer.valueOf(ctx.popIntToken());
    }
}
