package io.quarkus.qson.generator;

import io.quarkus.qson.QsonException;
import io.quarkus.qson.QsonValue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
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
        scanProperties();
        return this;
    }

    /**
     * Scan for and set up properties for ClassMapping
     *
     * @return
     */
    public ClassMapping scanProperties() {
        if (propertiesScanned) return this;
        LinkedHashMap<String, PropertyMapping> tmp = PropertyMapping.getPropertyMap(type);
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
        if (!hasConstructor) throw new QsonException(type.getName() + " does not have a default or noarg public constructor");

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
     * Add a property mapping for this class
     *
     * @param ref
     * @return
     */
    public ClassMapping addProperty(PropertyMapping ref) {
        properties.put(ref.propertyName, ref);
        return this;
    }

    public Class getType() {
        return type;
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

    public QsonGenerator getGenerator() {
        return generator;
    }
}
