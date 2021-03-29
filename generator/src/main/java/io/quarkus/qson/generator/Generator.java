package io.quarkus.qson.generator;

import io.quarkus.qson.QsonException;
import io.quarkus.qson.QsonValue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class Generator {

    Map<Class, ClassGenerator> classGenerators = new HashMap<>();
    DateHandler defaultDate = DateHandler.DEFAULT;

    /**
     * Set default for java.util.Date marshalling to be
     * number of milliseconds since epoch.
     */
    public Generator millisecondsDateFormat() {
        defaultDate = DateHandler.MILLISECONDS;
        return this;
    }

    /**
     * Set default for java.util.Date marshalling to be
     * number of seconds since epoch.
     */
    public Generator secondsDateFormat() {
        defaultDate = DateHandler.SECONDS;
        return this;
    }

    /**
     * Set default for java.util.Date marshalling to be
     * a String formatted by DateTimeFormatter parameter
     */
    public Generator dateFormat(DateTimeFormatter formatter, DateFormat format) {
        defaultDate = new DateHandler(format, formatter);
        defaultDate.formatter = formatter;
        return this;
    }

    /**
     * Fine tune generator settings for a specific type
     *
     * @param type
     * @return
     */
    public ClassGenerator generatorFor(Class type) {
        ClassGenerator generator = classGenerators.get(type);
        if (generator == null) {
            generator = new ClassGenerator(this, type);
            scanQsonValue(generator);
            classGenerators.put(type, generator);
        }
        return generator;
    }

    private void scanQsonValue(ClassGenerator generator) {
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

    public Map<Class, ClassGenerator> getClassGenerators() {
        return classGenerators;
    }

    public DateHandler getDateHandler() {
        return defaultDate;
    }

    public Deserializer.Builder deserializer(Class targetType) {
        return new Deserializer.Builder(this).type(targetType);
    }
    public Deserializer.Builder deserializer(Class targetType, Type generic) {
        return new Deserializer.Builder(this).type(targetType).generic(generic);
    }

    public Serializer.Builder serializer(Class targetType) {
        return new Serializer.Builder(this).type(targetType);
    }

    public Serializer.Builder serializer(Class targetType, Type genericType) {
        return new Serializer.Builder(this).type(targetType).generic(genericType);
    }




}
