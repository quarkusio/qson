package io.quarkus.qson.deserializer;

public abstract class DateTimeNumberParser extends ValueParser {
    public boolean start(ParserContext ctx) {
        int index = ctx.stateIndex();
        if (!ObjectParser.PARSER.startIntegerValue(ctx)) {
            ctx.pushState(continueEndValue, index);
            return false;
        }
        Object val = value(ctx);
        ctx.pushTarget(val);
        return true;
    }
 }
