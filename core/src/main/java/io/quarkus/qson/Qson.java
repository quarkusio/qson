package io.quarkus.qson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is a marker annotation so that build-time processors can scan for which
 * classes to generate.
 *
 * Can only be placed on a public and non-abstract class.
 *
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Qson {
    boolean generateParser() default true;
    boolean generateWriter() default true;
}
