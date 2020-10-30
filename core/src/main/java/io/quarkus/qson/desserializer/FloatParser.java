package io.quarkus.qson.desserializer;

public class FloatParser implements JsonParser {
    @Override
    public ParserState startState() {
        return ObjectParser.PARSER.startNumberValue;
    }

    @Override
    public <T> T getTarget(ParserContext ctx) {
        return (T)Float.valueOf(ctx.popFloatToken());
    }
}
