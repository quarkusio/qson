package io.quarkus.qson.parser;

public abstract class ValueParser implements QsonParser {
    public final ParserState start = this::start;
    public final ContextValue value = this::value;
    public final ParserState continueEndValue = this::continueEndValue;
    public final ParserState continueStart = this::continueStart;

    @Override
    public ParserState startState() {
        return start;
    }

    public boolean continueEndValue(ParserContext ctx) {
        ctx.popState();
        Object val = value(ctx);
        ctx.pushTarget(val);
        return true;
    }

    public void endValue(ParserContext ctx) {
        Object val = value(ctx);
        ctx.pushTarget(val);
    }

    public boolean continueStart(ParserContext ctx) {
        ctx.popState();
        return start(ctx);
    }

    public abstract boolean start(ParserContext ctx);
    public abstract Object value(ParserContext ctx);
}
