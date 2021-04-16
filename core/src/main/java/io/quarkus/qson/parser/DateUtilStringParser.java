package io.quarkus.qson.parser;

import io.quarkus.qson.QsonException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class DateUtilStringParser extends ValueParser {

    public static final DateUtilStringParser ISO_8601_OFFSET_DATE_TIME = new DateUtilStringParser(DateUtil.ISO_8601_OFFSET_DATE_TIME);
    public static final DateUtilStringParser RFC_1123_DATE_TIME = new DateUtilStringParser(DateUtil.RFC_1123_DATE_TIME);



    private final DateFormat formatter;

    private DateUtilStringParser(DateFormat formatter) {
        this.formatter = formatter;
    }

    public DateUtilStringParser(String pattern) {
        formatter = new SimpleDateFormat(pattern);
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


    @Override
    public Object value(ParserContext ctx) {
        String string = ctx.popToken();
        try {
            return formatter.parse(string);
        } catch (ParseException e) {
            throw new QsonException(e);
        }
    }
}
