package io.quarkus.qson.serializer;

import java.time.OffsetDateTime;
import java.util.Date;

public class DateNumberWriter {
    public static final QsonObjectWriter OFFSET_DATE_TIME_MILLISECONDS = new QsonObjectWriter() {
        @Override
        public void write(JsonWriter writer, Object target) {
            OffsetDateTime date = (OffsetDateTime)target;
            writer.write(date.toInstant().toEpochMilli());

        }
    };
    public static final QsonObjectWriter OFFSET_DATE_TIME_SECONDS = new QsonObjectWriter() {
        @Override
        public void write(JsonWriter writer, Object target) {
            OffsetDateTime date = (OffsetDateTime)target;
            writer.write(date.toInstant().toEpochMilli() / 1000);

        }
    };
    public static final QsonObjectWriter DATE_UTIL_MILLISECONDS = new QsonObjectWriter() {
        @Override
        public void write(JsonWriter writer, Object target) {
            Date date = (Date)target;
            writer.write(date.getTime());

        }
    };
    public static final QsonObjectWriter DATE_UTIL_SECONDS = new QsonObjectWriter() {
        @Override
        public void write(JsonWriter writer, Object target) {
            Date date = (Date)target;
            writer.write(date.getTime() / 1000);

        }
    };
}
