package io.quarkus.qson.deserializer;

public abstract class EnumParser extends ObjectParser {

    public EnumParser() {
        start = this::startStringValue;
        continueStart = this::continueStartStringValue;
    }

    @Override
    public boolean start(ParserContext ctx) {
        return startStringValue(ctx);
    }

    @Override
    public ParserState startState() {
        return startStringValue;
    }

    @Override
    public boolean continueStart(ParserContext ctx) {
        return continueStartStringValue(ctx);
    }
}
