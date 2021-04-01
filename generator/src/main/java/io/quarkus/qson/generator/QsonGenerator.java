package io.quarkus.qson.generator;

import java.text.DateFormat;
import java.time.format.DateTimeFormatter;

/**
 * Before bytecode generation you can set up global and per-type configuration and mapping metadata
 * for how you want bytecode to be generated.
 *
 */
public interface QsonGenerator {
    /**
     * Set default for java.util.Date and java.time.* (de)serialization to be
     * number of milliseconds since epoch.
     */
    QsonGenerator millisecondsDateFormat();

    /**
     * Set default for java.util.Date and java.time.* (de)serialization to be
     * number of seconds since epoch.
     */
    QsonGenerator secondsDateFormat();

    /**
     * Set default for java.util.Date (de)serialization to be
     * a String formatted by DateTimeFormatter parameter
     */
    QsonGenerator dateFormat(DateTimeFormatter formatter, DateFormat format);

    /**
     * Default mapping for date and time classes for all parsers and writers
     * This can be overriden per class and per class property
     *
     * @return
     */
    DateHandler defaultDateHandler();

    /**
     * Fine tune generator class mappings for a specific type
     *
     * @param type
     * @return
     */
    ClassMapping mappingFor(Class type);
}
