package io.quarkus.qson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation specifies that the class is serialized and/or deserialized to/from
 * a json value: a string, number, or boolean value.
 *
 * For deserialization, this annotation must be placed on only one constructor or method.
 * The constructor or method must have one and only one parameter this is a String, a String, a primitive, or the object class for that primitive.
 *
 * For serialization, this annotation must be placed on one and only one method that takes no parameters
 * and returns a String, a primitive, or the object class for that primitive.
 *
 */
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface QsonValue {
}
