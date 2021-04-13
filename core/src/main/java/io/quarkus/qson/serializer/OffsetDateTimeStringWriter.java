package io.quarkus.qson.serializer;

import io.quarkus.qson.QsonDate;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class OffsetDateTimeStringWriter implements QsonObjectWriter {

    public static final OffsetDateTimeStringWriter ISO_8601_OFFSET_DATE_TIME = new OffsetDateTimeStringWriter(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    public static final OffsetDateTimeStringWriter RFC_1123_DATE_TIME = new OffsetDateTimeStringWriter(DateTimeFormatter.RFC_1123_DATE_TIME);


    protected DateTimeFormatter formatter;

    private OffsetDateTimeStringWriter(DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    public OffsetDateTimeStringWriter(String pattern) {
        this.formatter = DateTimeFormatter.ofPattern(pattern);
    }
    @Override
    public void write(JsonWriter writer, Object target) {
        OffsetDateTime date = (OffsetDateTime)target;
        writer.write(date.format(formatter));
    }
}
