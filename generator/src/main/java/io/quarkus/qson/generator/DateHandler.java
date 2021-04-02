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
    String format;
    boolean isMilliseconds;
    boolean isSeconds;

    public static final DateHandler MILLISECONDS;
    public static final DateHandler SECONDS;

    /**
     * Defaults to JSON string with ISO_OFFSET_DATE_TIME format
     */
    public static final DateHandler DEFAULT;

    static {
        MILLISECONDS = new DateHandler();
        MILLISECONDS.isMilliseconds = true;

        SECONDS = new DateHandler();
        SECONDS.isSeconds = true;

        DEFAULT = new DateHandler();
        DEFAULT.format = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    }

    private DateHandler() {}

    public DateHandler(String format) {
        this.format = format;
    }
}
