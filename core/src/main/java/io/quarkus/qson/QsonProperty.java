package io.quarkus.qson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can set the json property name.
 * Can set visibility of property too.
 *
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface QsonProperty {
    String value() default "";
    Serialization serialization() default Serialization.DEFAULT;

    public enum Serialization
    {
        /**
         * serialize and deserialize if there is a corresponding getter and setter
         */
        DEFAULT,
        /**
         * If property has a setter, then ignore the property on deserialization
         *
         */
        SERIALIZED_ONLY,
        /**
         * If property has a getter, then ignore it for serialization
         */
        DESERIALIZED_ONLY
        ;
    }
}
