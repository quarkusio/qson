package io.quarkus.qson.deserializer;

public class StringParser implements QsonParser {
    @Override
    public ParserState startState() {
        return ObjectParser.PARSER.startStringValue;
    }

    @Override
    public <T> T getTarget(ParserContext ctx) {
        return (T)ctx.popToken();
    }
}
