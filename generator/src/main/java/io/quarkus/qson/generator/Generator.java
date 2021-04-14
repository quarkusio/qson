package io.quarkus.qson.generator;

import io.quarkus.qson.GenericType;
import io.quarkus.qson.QsonDate;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class Generator implements QsonGenerator {

    QsonDate.Format dateFormat = QsonDate.Format.ISO_8601_OFFSET_DATE_TIME;

    Map<Class, ClassMapping> classGenerators = new HashMap<>();

    /**
     * Fine tune generator settings for a specific type.
     * This will scan for annotations and allocate default metadata for ClassMapping
     *
     * @param type
     * @return
     */
    @Override
    public ClassMapping mappingFor(Class type) {
        ClassMapping mapping = classGenerators.get(type);
        if (mapping == null) {
            mapping = new ClassMapping(this, type);
            mapping.scan();
            classGenerators.put(type, mapping);
        }
        return mapping;
    }

    /**
     * Fine tune generator settings for a specific type.
     * This will NOT SCAN for annotations or default setter/getter methods.
     * You will have to do this manually by calling methods on ClassMapping.
     *
     * @param type
     * @return
     */
    @Override
    public ClassMapping overrideMappingFor(Class type) {
        ClassMapping mapping = classGenerators.get(type);
        if (mapping == null) {
            mapping = new ClassMapping(this, type);
            classGenerators.put(type, mapping);
        }
        return mapping;
    }

    @Override
    public boolean hasMappingFor(Class type) {
        return classGenerators.containsKey(type);
    }

    @Override
    public QsonGenerator dateFormat(QsonDate.Format format) {
        this.dateFormat = format;
        return this;
    }

    @Override
    public QsonDate.Format getDateFormat() {
        return dateFormat;
    }

    public Deserializer.Builder deserializer(Type generic) {
        return new Deserializer.Builder(this).type(generic);
    }

    public Deserializer.Builder deserializer(GenericType generic) {
        return new Deserializer.Builder(this).type(generic.getType());
    }

    public Serializer.Builder serializer(Type genericType) {
        return new Serializer.Builder(this).type(genericType);
    }
    public Serializer.Builder serializer(GenericType genericType) {
        return new Serializer.Builder(this).type(genericType.getType());
    }
}
