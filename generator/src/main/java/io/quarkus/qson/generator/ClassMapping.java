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
public class ClassMapping {
    Class type;
    boolean isValue;
    Member valueSetter;
    Method valueGetter;
    QsonGenerator generator;


    boolean scanProperties = true;
    boolean propertiesScanned;
    LinkedHashMap<String, PropertyReference> properties = new LinkedHashMap<>();

    DateHandler dateHandler;

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
        this.valueSetter = getter;
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
    /**
     * Set default for java.util.Date and java.time.OffsetDateTime marshalling to be
     * number of milliseconds since epoch.
     */
    public ClassMapping millisecondsDateFormat() {
        dateHandler = DateHandler.MILLISECONDS;
        return this;
    }

    /**
     * Set default for java.util.Date and java.time.OffsetDateTime marshalling to be
     * number of seconds since epoch.
     */
    public ClassMapping secondsDateFormat() {
        dateHandler = DateHandler.SECONDS;
        return this;
    }

    /**
     * Set default for java.util.Date and java.time.OffsetDateTime marshalling to be
     * a String formatted by string pattern.  Pattern corresponds to DatTimeFormatter configuration
     */
    public ClassMapping dateFormat(String pattern) {
        dateHandler = new DateHandler(pattern);
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

    public Method getValueGetter() {
        return valueGetter;
    }

    public QsonGenerator getGenerator() {
        return generator;
    }

    public DateHandler getDateHandler() {
        if (dateHandler == null) return generator.defaultDateHandler();
        return dateHandler;
    }
}
