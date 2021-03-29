package io.quarkus.qson.generator;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Json class mapping metadata
 *
 */
public class ClassMetadata {
    Class type;
    boolean isValue;
    Member valueSetter;
    Method valueGetter;
    GeneratorMetadata generator;


    boolean scanProperties = true;
    boolean propertiesScanned;
    LinkedHashMap<String, PropertyReference> properties = new LinkedHashMap<>();

    DateHandler dateHandler;

    public ClassMetadata(GeneratorMetadata gen, Class type) {
        this.generator = gen;
        this.type = type;
    }

    public ClassMetadata scanProperties() {
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

    public ClassMetadata valueSetter(Member setter) {
        isValue = true;
        this.valueSetter = setter;
        return this;
    }

    public ClassMetadata valueGetter(Method getter) {
        isValue = true;
        this.valueSetter = getter;
        return this;
    }

    public ClassMetadata clearValueMapping() {
        isValue = false;
        this.valueSetter = this.valueGetter = null;
        return this;
    }

    public ClassMetadata addProperty(PropertyReference ref) {
        properties.put(ref.propertyName, ref);
        return this;
    }
    public ClassMetadata dateHandler(DateHandler dateHandler) {
        this.dateHandler = dateHandler;
        return this;
    }

    public ClassMetadata scanProperties(boolean scan) {
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

    public GeneratorMetadata getGenerator() {
        return generator;
    }

    public DateHandler getDateHandler() {
        if (dateHandler == null) return generator.defaultDateHandler();
        return dateHandler;
    }
}
