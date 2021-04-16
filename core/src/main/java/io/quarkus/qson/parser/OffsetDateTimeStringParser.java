package io.quarkus.qson.parser;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class OffsetDateTimeStringParser extends AbstractStringDateTimeParser {

    public static final OffsetDateTimeStringParser ISO_8601_OFFSET_DATE_TIME = new OffsetDateTimeStringParser(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    public static final OffsetDateTimeStringParser RFC_1123_DATE_TIME = new OffsetDateTimeStringParser(DateTimeFormatter.RFC_1123_DATE_TIME);


    public OffsetDateTimeStringParser(DateTimeFormatter formatter) {
        super(formatter);
    }

    public OffsetDateTimeStringParser(String pattern) {
        super(pattern);
    }

    @Override
    public Object value(ParserContext ctx) {
        String string = ctx.popToken();
        return OffsetDateTime.parse(string, formatter);
    }
}
