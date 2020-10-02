package io.quarkus.qson.desserializer;

public class IntegerParser implements JsonParser {
    @Override
    public ParserState parser() {
        return ObjectParser.PARSER.startIntegerValue;
    }

    @Override
    public <T> T getTarget(ParserContext ctx) {
        return (T)Integer.valueOf(ctx.popIntToken());
    }
}
