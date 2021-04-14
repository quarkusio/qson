package io.quarkus.qson.generator;

import io.quarkus.qson.QsonDate;
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
    Member valueSetter;
    Method valueGetter;
    QsonGenerator generator;



    boolean scanProperties = true;
    boolean propertiesScanned;
    boolean qsonValueScanned;
    LinkedHashMap<String, PropertyReference> properties = new LinkedHashMap<>();

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
        LinkedHashMap<String, PropertyReference> tmp = PropertyReference.getPropertyMap(type);
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
                if (valueSetter != null) {
                    throw new QsonException("Cannot have two constructors with @QsonValue on it for class: " + type.getName());
                }
                hasConstructor = true;
                valueSetter(con);
            }
        }
        if (!hasConstructor) throw new QsonException(type.getName() + " does not have a default or noarg public constructor");

        for (Method method : type.getMethods()) {
            if (method.isAnnotationPresent(QsonValue.class)) {
                if (method.getParameterCount() == 0) {
                    if (valueGetter != null) {
                        throw new QsonException("Cannot have multiple @QsonValue annotations for write value for class: " + type.getName());
                    }
                    valueGetter(method);
                } else if (method.getParameterCount() == 1) {
                    if (valueSetter != null) {
                        throw new QsonException("Cannot have multiple @QsonValue annotations for read value for class: " + type.getName());
                    }
                    valueSetter(method);
                }
            }
        }
        qsonValueScanned = true;
        return this;
    }


    public List<PropertyReference> getProperties() {
        return new ArrayList<>(properties.values());
    }

    public ClassMapping valueSetter(Member setter) {
        isValue = true;
        this.valueSetter = setter;
        return this;
    }

    public ClassMapping valueGetter(Method getter) {
        isValue = true;
        this.valueGetter = getter;
        return this;
    }

    public ClassMapping clearValueMapping() {
        isValue = false;
        this.valueSetter = this.valueGetter = null;
        return this;
    }

    public ClassMapping addProperty(PropertyReference ref) {
        properties.put(ref.propertyName, ref);
        return this;
    }

    public ClassMapping scanProperties(boolean scan) {
        this.scanProperties = scan;
        return this;
    }

    public Class getType() {
        return type;
    }

    public boolean isValue() {
        return isValue;
    }

    public Member getValueSetter() {
        return valueSetter;
    }

    public Class getValueSetterType() {
        if (getValueSetter() == null) {
            throw new QsonException("There is no value setter for value class: " + getType().getName());
        }
        if (getValueSetter() instanceof Method) {
            Method setter = (Method)getValueSetter();
            return setter.getParameterTypes()[0];
        } else {
            Constructor setter = (Constructor)getValueSetter();
            return setter.getParameterTypes()[0];
        }
    }

    public Method getValueGetter() {
        return valueGetter;
    }

    public QsonGenerator getGenerator() {
        return generator;
    }
}
