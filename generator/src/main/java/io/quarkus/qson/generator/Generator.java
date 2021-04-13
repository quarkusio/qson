package io.quarkus.qson.generator;

import io.quarkus.qson.GenericType;
import io.quarkus.qson.QsonDate;
import io.quarkus.qson.QsonException;
import io.quarkus.qson.QsonValue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class Generator implements QsonGenerator {

    QsonDate.Format dateFormat = QsonDate.Format.ISO_8601_OFFSET_DATE_TIME;

    Map<Class, ClassMapping> classGenerators = new HashMap<>();

    /**
     * Fine tune generator settings for a specific type
     *
     * @param type
     * @return
     */
    @Override
    public ClassMapping mappingFor(Class type) {
        ClassMapping generator = classGenerators.get(type);
        if (generator == null) {
            generator = new ClassMapping(this, type);
            scanQsonValue(generator);
            classGenerators.put(type, generator);
        }
        return generator;
    }

    private void scanQsonValue(ClassMapping generator) {
        if (generator.type.isEnum()) return;
        boolean hasConstructor = false;
        for (Constructor con : generator.type.getConstructors()) {
            if (con.getParameterCount() == 0) {
                hasConstructor = true;
            } else if (con.getParameterCount() == 1 && con.isAnnotationPresent(QsonValue.class)) {
                if (generator.valueSetter != null) {
                    throw new QsonException("Cannot have two constructors with @QsonValue on it for class: " + generator.type.getName());
                }
                hasConstructor = true;
                generator.valueSetter(con);
            }
        }
        if (!hasConstructor) throw new QsonException(generator.type.getName() + " does not have a default or noarg public constructor");

        for (Method method : generator.type.getMethods()) {
            if (method.isAnnotationPresent(QsonValue.class)) {
               if (method.getParameterCount() == 0) {
                   if (generator.valueGetter != null) {
                       throw new QsonException("Cannot have multiple @QsonValue annotations for write value for class: " + generator.type.getName());
                   }
                   generator.valueGetter(method);
               } else if (method.getParameterCount() == 1) {
                   if (generator.valueSetter != null) {
                       throw new QsonException("Cannot have multiple @QsonValue annotations for read value for class: " + generator.type.getName());
                   }
                   generator.valueSetter(method);
               }
            }
        }
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
