package io.quarkus.qson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is a marker annotation so that build-time processors can scan and register
 * a custom writer for a specific class
 *
 * Must be placed on a public class that implements QsonObjectWriter
 *
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface QsonCustomWriter {
    /**
     * The class that this writer is a custom writer for
     *
     * @return
     */
    Class value();
}
