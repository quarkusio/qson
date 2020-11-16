package io.quarkus.qson.deserializer;

public class BooleanParser implements JsonParser {
    @Override
    public ParserState startState() {
        return ObjectParser.PARSER.startBooleanValue;
    }

    @Override
    public <T> T getTarget(ParserContext ctx) {
        return (T)Boolean.valueOf(ctx.popBooleanToken());
    }
}
