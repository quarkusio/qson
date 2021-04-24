package io.quarkus.qson.generator;

import io.quarkus.qson.QsonDate;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

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

    /**
     * For the reader it can be a constructor on the type, or a member method on the type.
     * This member constructor or method must have one parameter, either a String or a primitive type.
     * The method can also be a static method on any other class.  In this case, the static method
     * must return the type and have one parameter that is a String or a primitive.
     *
     * For the writer method, it must be a member method on the type that takes no parameters and returns a string
     * or a primitive value.  It can also be any arbitrary static method on any other class.  In this
     * case it must return a String or primitive value and take the type as a parameter.
     *
     * @param type
     * @param reader constructor or method
     * @param writer
     * @return
     */
    ClassMapping valueMappingFor(Class type, Member reader, Method writer);

    /**
     * Has somebody registered a custom mapping for a specific type?
     *
     * @param type
     * @return
     */
    boolean hasMappingFor(Class type);

    /**
     * Set default json date format
     * @param format
     * @return
     */
    QsonGenerator dateFormat(QsonDate.Format format);

    /**
     * Current default date format
     *
     * @return
     */
    QsonDate.Format getDateFormat();
}
