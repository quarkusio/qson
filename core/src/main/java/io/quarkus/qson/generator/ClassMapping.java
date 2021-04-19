package io.quarkus.qson.generator;

import io.quarkus.qson.QsonException;
import io.quarkus.qson.QsonTransformer;
import io.quarkus.qson.QsonValue;
import io.quarkus.qson.writer.JsonWriter;
import io.quarkus.qson.writer.QsonObjectWriter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Json class mapping metadata
 *
 */
public class ClassMapping {
    Class type;
    boolean isValue;
    Member valueReader;
    Method valueWriter;
    QsonGenerator generator;
    boolean isTransformed;
    Method transformer;
    Class transformerClass;
    boolean hasCustomWriter;
    Class<? extends QsonObjectWriter> customWriter;
    Field customWriterField;



    boolean propertiesScanned;
    boolean qsonValueScanned;
    LinkedHashMap<String, PropertyMapping> properties = new LinkedHashMap<>();

    ClassMapping(QsonGenerator gen, Class type) {
        this.generator = gen;
        this.type = type;
    }

    /**
     * Helper method that calls scanQsonvalue() and scanProperties()
     *
     * @return
     */
    public ClassMapping scan() {
        scanQsonValue();
        if (!isValue) scanProperties();
        return this;
    }

    /**
     * Scan for and set up properties for ClassMapping
     *
     * @return
     */
    public ClassMapping scanProperties() {
        if (propertiesScanned) return this;
        LinkedHashMap<String, PropertyMapping> tmp = PropertyMapping.scanPropertes(type);
        tmp.putAll(properties);
        properties = tmp;
        propertiesScanned = true;
        return this;
    }

    /**
     * Scan for @QsonValue annotation and set appropriate ClassMapping metadata
     * @return
     */
    public ClassMapping scanQsonValue() {
        if (qsonValueScanned) return this;
        if (type.isEnum()) return this;
        boolean hasConstructor = false;
        for (Constructor con : type.getConstructors()) {
            if (con.getParameterCount() == 0) {
                hasConstructor = true;
            } else if (con.getParameterCount() == 1 && con.isAnnotationPresent(QsonValue.class)) {
                if (valueReader != null) {
                    throw new QsonException("Cannot have two constructors with @QsonValue on it for class: " + type.getName());
                }
                hasConstructor = true;
                valueReader(con);
            }
        }
        for (Method method : type.getMethods()) {
            if (method.isAnnotationPresent(QsonValue.class)) {
                if (method.getParameterCount() == 0) {
                    if (valueWriter != null) {
                        throw new QsonException("Cannot have multiple @QsonValue annotations for write value for class: " + type.getName());
                    }
                    valueWriter(method);
                } else if (method.getParameterCount() == 1) {
                    if (valueReader != null) {
                        throw new QsonException("Cannot have multiple @QsonValue annotations for read value for class: " + type.getName());
                    }
                    valueReader(method);
                }
            }
        }
        qsonValueScanned = true;
        return this;
    }

    /**
     * List of java properties that will map to corresponding json properties
     *
     * @return
     */
    public List<PropertyMapping> getProperties() {
        return new ArrayList<>(properties.values());
    }

    /**
     * Mark this class as a value class.  A class with no properties that is created from a single
     * json value.  This method sets the method or constructor that will be used to create this
     * type from a single json value
     *
     * For the reader it can be a constructor on the type, or a member method on the type.
     * This member constructor or method must have one parameter, either a String or a primitive type.
     * The method can also be a static method on any other class.  In this case, the static method
     * must return the type and have one parameter that is a String or a primitive.
     *
     * @param setter
     * @return
     */
    public ClassMapping valueReader(Member setter) {
        if (setter == null) {
            this.valueReader = null;
            return this;
        }
        isValue = true;
        this.valueReader = setter;
        return this;
    }

    /**
     * Mark this class as a value class.  A class with no properties that is created from a single
     * json value.  This method sets the method that will be used to write this type to a json value
     *
     * This member method on the type and it must take no parameters and returnsa string
     * or a primitive value.  It can also be any arbitrary static method on any other class.  In this
     * case it must return a String or primitive value and take the type as a parameter.
     *
     * @param getter
     * @return
     */
    public ClassMapping valueWriter(Method getter) {
        if (getter == null) {
            this.valueWriter = null;
            return this;
        }
        isValue = true;
        this.valueWriter = getter;
        return this;
    }

    /**
     * If this class has been marked as a value class, it removes the reader and writer
     * and marks this class as a regular user class.
     *
     * @return
     */
    public ClassMapping clearValueMapping() {
        isValue = false;
        this.valueReader = this.valueWriter = null;
        return this;
    }

    /**
     * Manually define custom writer.  Must have a public no-arg constructor
     * This is less efficient than providing a custom writer through a static field
     * as Qson will have to allocate an instance every time you marshall this type.
     *
     * @param writer
     * @return
     */
    public ClassMapping customWriter(Class<? extends QsonObjectWriter> writer) {
        hasCustomWriter = true;
        customWriter = writer;
        return this;
    }

    /**
     * Manually define custom writer referenced by a public static field.
     * This is most efficient way to provide a custom writer for your class.
     *
     *
     * @param writer
     * @return
     */
    public ClassMapping customWriter(Field writer) {
        if (!Modifier.isPublic(writer.getModifiers()) && !Modifier.isStatic(writer.getModifiers()) && !QsonObjectWriter.class.equals(writer.getType())) {
            throw new QsonException("Custom writer field must be static, public, and be a QsonObjectWriter");
        }
        hasCustomWriter = true;
        customWriterField = writer;
        return this;
    }

    /**
     * Add a property mapping for this class
     *
     * @param ref
     * @return
     */
    public ClassMapping addProperty(PropertyMapping ref) {
        properties.put(ref.propertyName, ref);
        return this;
    }

    /**
     * Specify a class that will be used for parses in place of this type.
     * This transformer class must have a method on it that returns an instance
     * of the type of this ClassMapping.
     *
     * This is useful for scenarios where you have a thirdparty class that you cannot modify
     * that cannot be mapped using standard Qson. For example, a class that does not have an
     * empty parameter constructor or does not use standard get/set bean pattern
     *
     * Example:
     * <pre>
     * public class Thirdparty {
     *     public Thirdparty(int x, String y) {
     *
     *     }
     * }
     *
     * public class MyTransformer {
     *     private int x;
     *     private String y;
     *
     *     &#64;QsonTransformer
     *     public Thirdparty createThirdparty() {
     *         return new Thirdparty(x, y);
     *     }
     *
     *
     *     public int getX() { return x; }
     *     public void setX(int x) { this.x = x; }
     *
     *     public String getY() { return y; }
     *     public void setX(String y) { this.y = y; }
     * }
     *
     * QsonMapper mapper = new QsonMapper();
     * mapper.overrideMappingFor(Thirdparty.class).transformer(MyTransformer.class);
     * </pre>
     *
     * @param transformer
     * @return
     */
    public ClassMapping transformer(Class transformer) {
        for (Method m : transformer.getMethods()) {
            if (Modifier.isPublic(m.getModifiers())
                && !Modifier.isStatic(m.getModifiers())
                && type.equals(m.getReturnType())
                && m.getParameterCount() == 0
                && m.isAnnotationPresent(QsonTransformer.class)
            ) {
                this.isTransformed = true;
                this.transformerClass = transformer;
                this.transformer = m;
                return this;
            }
        }
        throw new QsonException("There is no method in transformer " + transformer.getName() + " that meets criteria to transform " + this.type.getName());
    }

    public Class getType() {
        return type;
    }

    /**
     * Does this class have a transformer?
     *
     * @return
     */
    public boolean isTransformed() {
        return isTransformed;
    }

    /**
     * Method on transformer class to use to convert to the type of this ClassMapping
     *
     * @return
     */
    public Method getTransformer() {
        return transformer;
    }

    /**
     * Transformer class to use to convert to the type of this ClassMapping
     *
     * @return
     */
    public Class getTransformerClass() {
        return transformerClass;
    }

    /**
     * Is this mapping a value mapping?
     *
     * @return
     */
    public boolean isValue() {
        return isValue;
    }

    /**
     * Returns the constructor or method that will be used if this mapping is a value mapping.
     * Otherwise returns null
     *
     * @return
     */
    public Member getValueReader() {
        return valueReader;
    }

    /**
     * If this mapping is a value mapping, it will return the reader method or constructor that
     * will be used to unmarshall json
     *
     * @return
     */
    public Class getValueReaderType() {
        if (getValueReader() == null) {
            throw new QsonException("There is no value setter for value class: " + getType().getName());
        }
        if (getValueReader() instanceof Method) {
            Method setter = (Method) getValueReader();
            return setter.getParameterTypes()[0];
        } else {
            Constructor setter = (Constructor) getValueReader();
            return setter.getParameterTypes()[0];
        }
    }

    /**
     * If this mapping is a value mapping, this will return the method that will be used
     * to serialize this type.
     *
     * @return
     */
    public Method getValueWriter() {
        return valueWriter;
    }

    public boolean hasCustomWriter() {
        return hasCustomWriter;
    }

    public Class<? extends QsonObjectWriter> getCustomWriter() {
        return customWriter;
    }

    public Field getCustomWriterField() {
        return customWriterField;
    }

    public QsonGenerator getGenerator() {
        return generator;
    }
}
