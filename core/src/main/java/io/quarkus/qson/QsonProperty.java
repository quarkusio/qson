package io.quarkus.qson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that provides finer grain mapping for the specific property it annotates.
 *
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface QsonProperty {
    /**
     * Sets json field name
     *
     */
    String value() default "";
}
