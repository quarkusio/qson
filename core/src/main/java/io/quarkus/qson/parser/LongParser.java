package io.quarkus.qson.parser;

public class LongParser implements QsonParser {
    @Override
    public ParserState startState() {
        return ObjectParser.PARSER.startIntegerValue;
    }

    @Override
    public <T> T getTarget(ParserContext ctx) {
        return (T)Long.valueOf(ctx.popLongToken());
    }
}
