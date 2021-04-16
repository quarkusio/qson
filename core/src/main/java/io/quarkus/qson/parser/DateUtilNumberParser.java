package io.quarkus.qson.parser;

import java.util.Date;

public class DateUtilNumberParser {
    public static final DateTimeNumberParser MILLIS_UTC = new DateTimeNumberParser() {
        @Override
        public Object value(ParserContext ctx) {
            long time = ctx.popLongToken();
            return new Date(time);
        }
    };
    public static final DateTimeNumberParser SECONDS_UTC = new DateTimeNumberParser() {
        @Override
        public Object value(ParserContext ctx) {
            long time = ctx.popLongToken();
            return new Date(time * 1000l);
        }
    };
}
