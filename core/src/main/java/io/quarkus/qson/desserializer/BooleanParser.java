package io.quarkus.qson.desserializer;

public class BooleanParser implements JsonParser {
    @Override
    public ParserState parser() {
        return ObjectParser.PARSER.startBooleanValue;
    }

    @Override
    public <T> T getTarget(ParserContext ctx) {
        return (T)Boolean.valueOf(ctx.popBooleanToken());
    }
}
