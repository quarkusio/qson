package io.quarkus.qson.parser;

public class IntegerParser implements QsonParser {
    @Override
    public ParserState startState() {
        return ObjectParser.PARSER.startIntegerValue;
    }

    @Override
    public <T> T getTarget(ParserContext ctx) {
        return (T)Integer.valueOf(ctx.popIntToken());
    }
}
