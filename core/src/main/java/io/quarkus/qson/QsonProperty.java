package io.quarkus.qson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that provides finer grain mapping for the specific property it annotates.
 * For getter and setter methods and fields, if you put this annotation on one of those, then
 * you do not need to place it on all of them as that one annotation will be used for all of them.
 *
 * If your method does not match a getter or setter pattern (method name starts with "get", "is", or "set")
 * then you must place this annotation on the method if you want it to map to json.  In this case
 * an empty value() will default to the method's name.
 *
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface QsonProperty {
    /**
     * Sets json property name
     *
     */
    String value() default "";
}
