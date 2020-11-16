package io.quarkus.qson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface QsonProperty {
    String value() default "";
    Serialization serialization() default Serialization.DEFAULT;

    public enum Serialization
    {
        DEFAULT,
        SERIALIZED_ONLY,
        DESERIALIZED_ONLY
        ;
    }
}
