package io.quarkus.qson.generator;

import io.quarkus.qson.QsonAny;
import io.quarkus.qson.QsonException;
import io.quarkus.qson.QsonIgnore;
import io.quarkus.qson.QsonIgnoreRead;
import io.quarkus.qson.QsonIgnoreWrite;
import io.quarkus.qson.QsonProperty;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Java mapping metadata for a json property.
 *
 */
public class PropertyReference {
    Method getter;
    Method setter;
    Type genericType;
    Class type;
    String propertyName;
    String jsonName;
    boolean isAny;
    DateHandler dateHandler;

    private QsonProperty fieldAnnotation;
    private QsonProperty getterAnnotation;
    private QsonProperty setterAnnotation;
    private boolean ignoreRead;
    private boolean ignoreWrite;


    public Method getGetter() {
        return getter;
    }

    public void setGetter(Method getter) {
        this.getter = getter;
    }

    public Method getSetter() {
        return setter;
    }

    public void setSetter(Method setter) {
        this.setter = setter;
    }

    public Type getGenericType() {
        return genericType;
    }

    public void setGenericType(Type genericType) {
        this.genericType = genericType;
    }

    public Class getType() {
        return type;
    }

    public void setType(Class type) {
        this.type = type;
    }

    /**
     * Java property name
     *
     */
    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    /**
     * JSON property name
     *
     */
    public String getJsonName() {
        return jsonName;
    }

    public void setJsonName(String jsonName) {
        this.jsonName = jsonName;
    }

    public boolean isAny() {
        return isAny;
    }

    public void setAny(boolean any) {
        isAny = any;
    }

    public DateHandler getDateHandler() {
        return dateHandler;
    }

    /**
     * For properties that are java.util.Date or TemporalAccessor
     * This allows you to override default date handling for this property
     */
    public void setDateHandler(DateHandler dateHandler) {
        this.dateHandler = dateHandler;
    }

    public static List<PropertyReference> getProperties(Class type) {
        if (type.equals(Object.class)) return Collections.emptyList();
        LinkedHashMap<String, PropertyReference> properties = getPropertyMap(type);
        return new ArrayList<>(properties.values());
    }

    public static LinkedHashMap<String, PropertyReference> getPropertyMap(Class type) {
        if (type.equals(Object.class)) return new LinkedHashMap<>();
        LinkedHashMap<String, PropertyReference> properties = new LinkedHashMap<>();
        Set<String> ignored = new HashSet<>();
        for (Method m : type.getMethods()) {
            if (m.isAnnotationPresent(QsonAny.class)) {
                if (m.getParameterTypes().length == 2
                    && m.getParameterTypes()[0].equals(String.class)
                        && m.getParameterTypes()[1].equals(Object.class)) {
                    PropertyReference ref = new PropertyReference();
                    ref.setter = m;
                    ref.isAny = true;
                    properties.put("@QsonAnySetter", ref);
                } else if (m.getParameterTypes().length == 0 && m.getReturnType().equals(Map.class)) {
                    PropertyReference ref = new PropertyReference();
                    ref.getter = m;
                    ref.isAny = true;
                    properties.put("@QsonAnyGetter", ref);

                } else {
                    throw new QsonException("Illegal use of @QsonAny: " + m.toString());
                }
                continue;
            }
            if (isSetter(m)) {
                String javaName;
                if (m.getName().length() > 4) {
                    javaName = Character.toLowerCase(m.getName().charAt(3)) + m.getName().substring(4);
                } else {
                    javaName = m.getName().substring(3).toLowerCase();
                }
                if (ignored.contains(javaName)) continue;
                if (m.isAnnotationPresent(QsonIgnore.class)) {
                    ignored.add(javaName);
                    properties.remove(javaName);
                    continue;
                };
                Class paramType = m.getParameterTypes()[0];
                Type paramGenericType = m.getGenericParameterTypes()[0];
                PropertyReference ref = properties.get(javaName);
                if (ref != null) {
                    if (ref.setter != null) {
                        throw new QsonException("Duplicate setter methods: " + type.getName() + "." + m.getName());
                    }
                    if (!ref.type.equals(paramType) || !ref.genericType.equals(paramGenericType)) {
                        throw new QsonException("Type mismatch between getter and setter methods: "+ type.getName() + "." + m.getName());
                    }
                } else {
                    ref = new PropertyReference();
                    ref.type = paramType;
                    ref.genericType = paramGenericType;
                    ref.propertyName = javaName;
                    ref.jsonName = javaName;
                    properties.put(javaName, ref);
                }
                if (m.isAnnotationPresent(QsonIgnoreRead.class)) {
                    ref.ignoreRead = true;
                }
                if (m.isAnnotationPresent(QsonIgnoreWrite.class)) {
                    ref.ignoreWrite = true;
                }
                ref.setter = m;
                ref.setterAnnotation = m.getAnnotation(QsonProperty.class);
            } else if (isGetter(m)) {
                String javaName;
                if (m.getName().startsWith("is")) {
                    if (m.getName().length() > 3) {
                        javaName = Character.toLowerCase(m.getName().charAt(2)) + m.getName().substring(3);
                    } else {
                        javaName = m.getName().substring(2).toLowerCase();
                    }

                } else {
                    if (m.getName().length() > 4) {
                        javaName = Character.toLowerCase(m.getName().charAt(3)) + m.getName().substring(4);
                    } else {
                        javaName = m.getName().substring(3).toLowerCase();
                    }
                }
                if (ignored.contains(javaName)) continue;
                if (m.isAnnotationPresent(QsonIgnore.class)) {
                    ignored.add(javaName);
                    properties.remove(javaName);
                    continue;
                };
                Class mType = m.getReturnType();
                Type mGenericType = m.getGenericReturnType();
                PropertyReference ref = properties.get(javaName);
                if (ref != null) {
                    if (ref.getter != null) {
                        throw new QsonException("Duplicate getter methods: " + type.getName() + "." + m.getName());
                    }
                    if (!ref.type.equals(mType) || !ref.genericType.equals(mGenericType)) {
                        throw new QsonException("Type mismatch between getter and setter methods: "+ type.getName() + "." + m.getName());
                    }
                } else {
                    ref = new PropertyReference();
                    ref.type = mType;
                    ref.genericType = mGenericType;
                    ref.propertyName = javaName;
                    ref.jsonName = javaName;
                    properties.put(javaName, ref);
                }
                if (m.isAnnotationPresent(QsonIgnoreRead.class)) {
                    ref.ignoreRead = true;
                }
                if (m.isAnnotationPresent(QsonIgnoreWrite.class)) {
                    ref.ignoreWrite = true;
                }
                ref.getter = m;
                ref.getterAnnotation = m.getAnnotation(QsonProperty.class);
            }
        }
        Class target = type;
        while (target != null && !target.equals(Object.class)) {
            for (Field field : target.getDeclaredFields()) {
                PropertyReference ref = properties.get(field.getName());
                if (ref == null) continue;
                if (ignored.contains(field.getName())) continue;
                if (field.isAnnotationPresent(QsonIgnore.class)) {
                    properties.remove(field.getName());
                    continue;
                }
                if (field.isAnnotationPresent(QsonProperty.class)) {
                    QsonProperty property = field.getAnnotation(QsonProperty.class);
                    ref.fieldAnnotation = property;
                }
                if (field.isAnnotationPresent(QsonIgnoreRead.class)) {
                    ref.ignoreRead = true;
                }
                if (field.isAnnotationPresent(QsonIgnoreWrite.class)) {
                    ref.ignoreWrite = true;
                }
            }
            target = target.getSuperclass();
        }
        for (PropertyReference ref : properties.values()) {
            QsonProperty property = null;
            if (ref.fieldAnnotation != null) {
                if (property != null) {
                    throw new QsonException("Can only have one @QsonProperty annotation between field and setter/getter methods: " + ref.propertyName);
                }
                property = ref.fieldAnnotation;

            }
            if (ref.getterAnnotation != null) {
                if (property != null) {
                    throw new QsonException("Can only have one @QsonProperty annotation between field and setter/getter methods: " + ref.propertyName);
                }
                property = ref.getterAnnotation;

            }
            if (ref.setterAnnotation != null) {
                if (property != null) {
                    throw new QsonException("Can only have one @QsonProperty annotation between field and setter/getter methods: " + ref.propertyName);
                }
                property = ref.setterAnnotation;

            }
            if (ref.ignoreRead) ref.setter = null;
            if (ref.ignoreWrite) ref.getter = null;
            if (property != null) {
                if (!property.value().isEmpty()) ref.jsonName = property.value();
            }
        }
        return properties;
    }

    static boolean isSetter(Method m) {
        return Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()) && m.getName().startsWith("set") && m.getName().length() > "set".length()
                && m.getParameterCount() == 1;
    }

    static boolean isGetter(Method m) {
        return Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()) && ((m.getName().startsWith("get") && m.getName().length() > "get".length()) || (m.getName().startsWith("is")) && m.getName().length() > "is".length())
                && m.getParameterCount() == 0 && !m.getReturnType().equals(void.class)
                && !m.getDeclaringClass().equals(Object.class);
    }


}
