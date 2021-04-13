package io.quarkus.qson.deserializer;

import io.quarkus.qson.QsonDate;

import java.time.format.DateTimeFormatter;

public abstract class AbstractStringDateTimeParser extends ValueParser {
    DateTimeFormatter formatter;

    public AbstractStringDateTimeParser(DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    public AbstractStringDateTimeParser(String pattern) {
        this.formatter = DateTimeFormatter.ofPattern(pattern);
    }

    public boolean start(ParserContext ctx) {
        int index = ctx.stateIndex();
        if (!ObjectParser.PARSER.startStringValue(ctx)) {
            ctx.pushState(continueEndValue, index);
            return false;
        }
        Object val = value(ctx);
        ctx.pushTarget(val);
        return true;
    }
}
