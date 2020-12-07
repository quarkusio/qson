package io.quarkus.qson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that designates a property to collect any object properties
 * that are not mapped. The annotation must be applied twice if you want both
 * deserialization and serialization.
 *
 * For deserialization, the annotation must be placed on a method that has two arguments,
 * the first being String, the second being Object.
 *
 * For serialization the method must take no parameters and return a {@link java.util.Map}
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface QsonAny {
}
