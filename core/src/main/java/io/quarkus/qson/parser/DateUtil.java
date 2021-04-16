package io.quarkus.qson.parser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class DateUtil {
    public static final DateFormat ISO_8601_OFFSET_DATE_TIME;
    public static final DateFormat RFC_1123_DATE_TIME;

    static {
        ISO_8601_OFFSET_DATE_TIME = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        RFC_1123_DATE_TIME = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
    }
}
