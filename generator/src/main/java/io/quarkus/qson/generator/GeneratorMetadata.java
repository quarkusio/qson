package io.quarkus.qson.generator;

import java.text.DateFormat;
import java.time.format.DateTimeFormatter;

/**
 * Before bytecode generator you can set up global and per-type configuration and metadata
 * for how you want bytecode to be generated.
 *
 */
public interface GeneratorMetadata {
    /**
     * Set default for java.util.Date marshalling to be
     * number of milliseconds since epoch.
     */
    Generator millisecondsDateFormat();

    /**
     * Set default for java.util.Date marshalling to be
     * number of seconds since epoch.
     */
    Generator secondsDateFormat();

    /**
     * Set default for java.util.Date marshalling to be
     * a String formatted by DateTimeFormatter parameter
     */
    Generator dateFormat(DateTimeFormatter formatter, DateFormat format);

    /**
     * Default way to handle dates for all parsers and writers
     *
     * @return
     */
    DateHandler defaultDateHandler();

    /**
     * Fine tune generator settings for a specific type
     *
     * @param type
     * @return
     */
    ClassMetadata metadataFor(Class type);
}
