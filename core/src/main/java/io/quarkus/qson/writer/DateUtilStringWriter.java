package io.quarkus.qson.writer;

import io.quarkus.qson.parser.DateUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtilStringWriter implements QsonObjectWriter {

    public static final DateUtilStringWriter ISO_8601_OFFSET_DATE_TIME = new DateUtilStringWriter(DateUtil.ISO_8601_OFFSET_DATE_TIME);
    public static final DateUtilStringWriter RFC_1123_DATE_TIME = new DateUtilStringWriter(DateUtil.RFC_1123_DATE_TIME);


    protected DateFormat formatter;

    public DateUtilStringWriter(String pattern) {
        this.formatter = new SimpleDateFormat(pattern);
    }

    private DateUtilStringWriter(DateFormat formatter) {
        this.formatter = formatter;
    }

    @Override
    public void write(JsonWriter writer, Object target) {
        Date date = (Date)target;
        writer.write(formatter.format(date));
    }
}
