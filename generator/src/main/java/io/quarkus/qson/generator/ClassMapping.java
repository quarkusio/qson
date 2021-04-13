package io.quarkus.qson.generator;

import io.quarkus.qson.QsonDate;
import io.quarkus.qson.QsonException;

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
    LinkedHashMap<String, PropertyReference> properties = new LinkedHashMap<>();

    public ClassMapping(QsonGenerator gen, Class type) {
        this.generator = gen;
        this.type = type;
    }

    public ClassMapping scanProperties() {
        if (propertiesScanned) return this;
        LinkedHashMap<String, PropertyReference> tmp = PropertyReference.getPropertyMap(type);
        tmp.putAll(properties);
        properties = tmp;
        propertiesScanned = true;
        return this;
    }

    public List<PropertyReference> getProperties() {
        if (scanProperties) scanProperties();
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
