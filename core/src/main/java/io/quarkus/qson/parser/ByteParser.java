package io.quarkus.qson.parser;

public class ByteParser implements QsonParser {
    @Override
    public ParserState startState() {
        return ObjectParser.PARSER.startIntegerValue;
    }

    @Override
    public <T> T getTarget(ParserContext ctx) {
        return (T)Byte.valueOf(ctx.popByteToken());
    }
}
