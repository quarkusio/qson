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
     * Fine tune generator settings for a specific type.
     * This will scan for annotations and allocate default metadata for ClassMapping
     *
     * @param type
     * @return
     */
    ClassMapping mappingFor(Class type);

    /**
     * Fine tune generator settings for a specific type.
     * This will NOT SCAN for annotations or default setter/getter methods.
     * You will have to do this manually by calling methods on ClassMapping.
     *
     * @param type
     * @return
     */
    ClassMapping overrideMappingFor(Class type);

    boolean hasMappingFor(Class type);

    /**
     * Set default json date format
     * @param format
     * @return
     */
    QsonGenerator dateFormat(QsonDate.Format format);

    QsonDate.Format getDateFormat();
}
