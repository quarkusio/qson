package io.quarkus.qson.deserializer;

public class ByteParser implements JsonParser {
    @Override
    public ParserState startState() {
        return ObjectParser.PARSER.startIntegerValue;
    }

    @Override
    public <T> T getTarget(ParserContext ctx) {
        return (T)Byte.valueOf(ctx.popByteToken());
    }
}
