package io.quarkus.qson.generator;

import io.quarkus.qson.QsonDate;

import java.text.DateFormat;
import java.time.format.DateTimeFormatter;

/**
 * Before bytecode generation you can set up global and per-type configuration and mapping metadata
 * for how you want bytecode to be generated.
 *
 */
public interface QsonGenerator {
    /**
     * Fine tune the class mappings for a specific type
     *
     * @param type
     * @return
     */
    ClassMapping mappingFor(Class type);

    /**
     * Set default json date format
     * @param format
     * @return
     */
    QsonGenerator dateFormat(QsonDate.Format format);

    QsonDate.Format getDateFormat();
}
