package io.quarkus.qson.generator;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class ClassGenerator {
    Class type;
    boolean isValue;
    Member valueSetter;
    Method valueGetter;
    Generator generator;


    boolean scanProperties = true;
    boolean propertiesScanned;
    LinkedHashMap<String, PropertyReference> properties = new LinkedHashMap<>();

    DateHandler dateHandler;

    public ClassGenerator(Generator gen, Class type) {
        this.generator = gen;
        this.type = type;
    }

    public ClassGenerator scanProperties() {
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

    public ClassGenerator valueSetter(Member setter) {
        isValue = true;
        this.valueSetter = setter;
        return this;
    }

    public ClassGenerator valueGetter(Method getter) {
        isValue = true;
        this.valueSetter = getter;
        return this;
    }

    public ClassGenerator clearValueMapping() {
        isValue = false;
        this.valueSetter = this.valueGetter = null;
        return this;
    }

    public ClassGenerator addProperty(PropertyReference ref) {
        properties.put(ref.propertyName, ref);
        return this;
    }
    public ClassGenerator dateHandler(DateHandler dateHandler) {
        this.dateHandler = dateHandler;
        return this;
    }

    public ClassGenerator scanProperties(boolean scan) {
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

    public Method getValueGetter() {
        return valueGetter;
    }

    public Generator getGenerator() {
        return generator;
    }

    public DateHandler getDateHandler() {
        if (dateHandler == null) return generator.getDateHandler();
        return dateHandler;
    }
}
