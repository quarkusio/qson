package io.quarkus.qson.generator;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

/**
 * Json type and format to use for java.util.Date and TemporalAccessor types
 *
 * For number format use MILLISECONDS or SECONDS constant
 *
 */
public class DateHandler {
    DateFormat format;
    DateTimeFormatter formatter;
    boolean isMilliseconds;
    boolean isSeconds;

    public static final DateHandler MILLISECONDS;
    public static final DateHandler SECONDS;

    /**
     * Defaults to String ISO_OFFSET_DATE_TIME
     */
    public static final DateHandler DEFAULT;

    static {
        MILLISECONDS = new DateHandler();
        MILLISECONDS.isMilliseconds = true;

        SECONDS = new DateHandler();
        SECONDS.isSeconds = true;

        DEFAULT = new DateHandler();
        DEFAULT.formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        DEFAULT.format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    }

    private DateHandler() {}

    public DateHandler(DateFormat format, DateTimeFormatter formatter) {
        this.format = format;
        this.formatter = formatter;
    }
}
