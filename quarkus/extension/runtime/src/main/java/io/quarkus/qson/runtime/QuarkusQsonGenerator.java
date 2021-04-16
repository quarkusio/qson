package io.quarkus.qson.runtime;

import io.quarkus.qson.GenericType;
import io.quarkus.qson.generator.QsonGenerator;

import java.lang.reflect.Type;

/**
 * Will only be used at build time!  An instance of this interface can only be injected
 * using the @QuarkusQsonInitializer annotation.  Note again that this code will
 * only run at build time!
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
public interface QuarkusQsonGenerator extends QsonGenerator {

    /**
     * Register a class to generate a bytecode json mapping for.  Useful
     * in situation where Quarkus does not automatically scan and discover
     * a class to generate bytecode parsers and writers for.
     *
     * @param type
     * @param parser
     * @param writer
     * @return
     */
    QuarkusQsonGenerator register(Type type, boolean parser, boolean writer);

    /**
     * Register a class to generate a bytecode json mapping for.  Useful
     * in situation where Quarkus does not automatically scan and discover
     * a class to generate bytecode parsers and writers for.
     *
     * @param type
     * @param parser
     * @param writer
     * @return
     */
    QuarkusQsonGenerator register(GenericType type, boolean parser, boolean writer);
}
