package io.quarkus.qson.deserializer;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class OffsetDateTimeNumberParser {
    public static final DateTimeNumberParser MILLIS_UTC = new DateTimeNumberParser() {
        @Override
        public Object value(ParserContext ctx) {
            long time = ctx.popLongToken();
            return OffsetDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneOffset.UTC);
        }
    };
    public static final DateTimeNumberParser SECONDS_UTC = new DateTimeNumberParser() {
        @Override
        public Object value(ParserContext ctx) {
            long time = ctx.popLongToken();
            return OffsetDateTime.ofInstant(Instant.ofEpochSecond(time), ZoneOffset.UTC);
        }
    };
}
