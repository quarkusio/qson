package io.quarkus.qson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can be used on a property to override default handling of java.util.Date
 * and java.time.OffsetDateTime
 *
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface QsonDate {
    public static enum Format {
        /**
         * parse/output from and to a json number that represents time in milliseconds since epoch
         */
        MILLISECONDS,
        /**
         * parse/output from and to a json number that represents time in seconds since epoch
         */
        SECONDS,
        /**
         * parse/output from and to a json string that represents ISO 8601 Offset Date Time '2011-12-03T10:15:30+01:00'
         */
        ISO_8601_OFFSET_DATE_TIME,
        /**
         * parse/output from and to a json string that represents RFC 1123 Date Time 'Tue, 3 Jun 2008 11:05:30 GMT'
         */
        RFC_1123_DATE_TIME,
        /**
         * Use a pattern provided in configuration or the pattern field in the @QsonDate annotation
         */
        PATTERN
    }
    Format format() default Format.PATTERN;
    /**
     * Sets json field name
     *
     */
    String pattern() default "";
}
