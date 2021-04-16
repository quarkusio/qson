package io.quarkus.qson.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be called back for Qson setup and initialization when running within
 * Quarkus.  This method must be public and static and return void and take
 * QuarkusQsonGenerator as a parameter.  Note that this method runs at build time!!!
 *
 * <pre>
 *     public class MyQsonInitializers {
 *          &#64;QuarkusQsonInitilizer
 *          public static void initQsonForMe(QuarkusQsonGenerator generator) {
 *              ... do some stuff ...
 *          }
 *     }
 * </pre>
 *
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface QuarkusQsonInitializer {
}
