package io.quarkus.qson.generator;

import io.quarkus.qson.QsonAny;
import io.quarkus.qson.QsonDate;
import io.quarkus.qson.QsonException;
import io.quarkus.qson.QsonIgnore;
import io.quarkus.qson.QsonIgnoreRead;
import io.quarkus.qson.QsonIgnoreWrite;
import io.quarkus.qson.QsonProperty;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Java mapping metadata for a json property.
 *
 */
public class PropertyMapping {
    Method getter;
    Method setter;
    Type genericType;
    Class type;
    String propertyName;
    String jsonName;
    boolean isAny;
    QsonDate.Format dateFormat;
    String datePattern;

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

    public QsonDate.Format getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(QsonDate.Format dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getDatePattern() {
        return datePattern;
    }

    public void setDatePattern(String datePattern) {
        this.dateFormat = QsonDate.Format.PATTERN;
        this.datePattern = datePattern;
    }

    public static LinkedHashMap<String, PropertyMapping> scanPropertes(Class type) {
        LinkedHashMap<String, Method> getters = new LinkedHashMap<>();
        LinkedHashMap<String, Method> setters = new LinkedHashMap<>();
        LinkedHashMap<String, PropertyMapping> mappings = new LinkedHashMap<>();
        for (Method m : type.getMethods()) {
            if (m.isAnnotationPresent(QsonAny.class)) {
                if (m.getParameterTypes().length == 2
                        && m.getParameterTypes()[0].equals(String.class)
                        && m.getParameterTypes()[1].equals(Object.class)) {
                    PropertyMapping ref = new PropertyMapping();
                    ref.setter = m;
                    ref.isAny = true;
                    mappings.put("@QsonAnySetter", ref);
                } else if (m.getParameterTypes().length == 0 && m.getReturnType().equals(Map.class)) {
                    PropertyMapping ref = new PropertyMapping();
                    ref.getter = m;
                    ref.isAny = true;
                    mappings.put("@QsonAnyGetter", ref);
                } else {
                    throw new QsonException("Illegal use of @QsonAny: " + m.toString());
                }
            } else if (isGetter(m)) {
                String javaName;
                if (m.getName().startsWith("is")) {
                    if (m.getName().length() > 3) {
                        javaName = Character.toLowerCase(m.getName().charAt(2)) + m.getName().substring(3);
                    } else {
                        javaName = m.getName().substring(2).toLowerCase();
                    }

                } else if (m.getName().startsWith("get")) {
                    if (m.getName().length() > 4) {
                        javaName = Character.toLowerCase(m.getName().charAt(3)) + m.getName().substring(4);
                    } else {
                        javaName = m.getName().substring(3).toLowerCase();
                    }
                } else {
                    throw new QsonException("Unreachable");
                }
                getters.put(javaName, m);
            } else if (isSetter(m)) {
                String javaName;
                if (m.getName().length() > 4) {
                    javaName = Character.toLowerCase(m.getName().charAt(3)) + m.getName().substring(4);
                } else {
                    javaName = m.getName().substring(3).toLowerCase();
                }
                setters.put(javaName, m);
            }
        }
        LinkedHashMap<String, Field> fields = new LinkedHashMap<>();
        fields(fields, type);
        Set<String> props = new HashSet<>();
        props.addAll(fields.keySet());
        props.addAll(setters.keySet());
        props.addAll(getters.keySet());

        for (String prop : props) {
            Method getter = getters.get(prop);
            Method setter = setters.get(prop);
            Field field = fields.get(prop);

            if ( (getter == null && setter == null)
                    || (getter != null && getter.isAnnotationPresent(QsonIgnore.class))
                    || (setter != null && setter.isAnnotationPresent(QsonIgnore.class))
                    || (field != null && field.isAnnotationPresent(QsonIgnore.class))
            ) {
                continue; // @QsonIgnore
            }

            String jsonName = checkQsonProperty(null, prop, type, getter);
            jsonName = checkQsonProperty(jsonName, prop, type, setter);
            jsonName = checkQsonProperty(jsonName, prop, type, field);
            if (jsonName == null) jsonName = prop;
            PropertyMapping ref = createPropertyMapping(prop, getter, setter, field, jsonName);
            if (mappings.containsKey(jsonName)) {
                throw new QsonException("Duplicate @QsonProperty values on java property " + prop + " for json name: " + jsonName);
            }
            mappings.put(jsonName, ref);
        }

        setters.clear();
        getters.clear();
        fields.clear();
        for (Method m : type.getMethods()) {
            if (m.isAnnotationPresent(QsonAny.class)) {
                continue;
            } else if (m.isAnnotationPresent(QsonProperty.class) && !isGetter(m) && isGetterSignature(m)) {
                QsonProperty qp = m.getAnnotation(QsonProperty.class);
                String jsonName = qp.value();
                if (qp.value().isEmpty()) {
                    jsonName = m.getName();
                }
                if (mappings.containsKey(jsonName) || getters.containsKey(jsonName)) {
                    throw new QsonException("Duplicate @QsonProperty values on: " + m.toString());
                }
                getters.put(jsonName, m);
            } else if (m.isAnnotationPresent(QsonProperty.class) && !isSetter(m) && isSetterSignature(m)) {
                QsonProperty qp = m.getAnnotation(QsonProperty.class);
                String jsonName = qp.value();
                if (qp.value().isEmpty()) {
                    jsonName = m.getName();
                }
                if (mappings.containsKey(jsonName) || setters.containsKey(jsonName)) {
                    throw new QsonException("Duplicate @QsonProperty values on: " + m.toString());
                }
                setters.put(jsonName, m);
            }
        }
        props = new HashSet<>();
        props.addAll(setters.keySet());
        props.addAll(getters.keySet());
        for (String jsonName : props) {
            Method getter = getters.get(jsonName);
            Method setter = setters.get(jsonName);
            String prop = getter != null ? getter.getName() : setter.getName();
            PropertyMapping ref = createPropertyMapping(prop, getter, setter, null, jsonName);
            mappings.put(jsonName, ref);
        }

        return mappings;
    }

    private static PropertyMapping createPropertyMapping(String prop, Method getter, Method setter, Field field, String jsonName) {
        Class propertyType;
        Type propertyGenericType;
        if (getter != null) {
            propertyType = getter.getReturnType();
            propertyGenericType = getter.getGenericReturnType();
        } else {
            propertyType = setter.getParameterTypes()[0];
            propertyGenericType = setter.getGenericParameterTypes()[0];
        }
        PropertyMapping ref = new PropertyMapping();
        ref.getter = getter;
        ref.setter = setter;
        ref.type = propertyType;
        ref.genericType = propertyGenericType;
        ref.propertyName = prop;
        ref.jsonName = jsonName;
        if (hasAnnotation(QsonIgnoreRead.class, getter, setter, field)) {
            ref.ignoreRead = true;
            ref.setter = null;
        }
        if (hasAnnotation(QsonIgnoreWrite.class, getter, setter, field)) {
            ref.ignoreWrite = true;
            ref.getter = null;
        }
        if (hasAnnotation(QsonDate.class, getter, setter, field)) {
            QsonDate date = getAnnotation(QsonDate.class, getter, setter, field);
            ref.dateFormat = date.format();
            if (!date.pattern().isEmpty()) ref.datePattern = date.pattern();
        }
        return ref;
    }

    private static boolean hasAnnotation(Class<? extends Annotation> annotation, AnnotatedElement... elements) {
        for (AnnotatedElement e : elements) {
            if (e != null && e.isAnnotationPresent(annotation)) return true;
        }
        return false;
    }

    private static <T extends Annotation> T getAnnotation(Class<T> annotation, AnnotatedElement... elements) {
        T val = null;
        for (AnnotatedElement e : elements) {
            if (e != null && e.isAnnotationPresent(annotation)) {
                if (val == null) {
                    val = e.getAnnotation(annotation);
                } else {
                    throw new QsonException("Duplicate @" + annotation.getName() + " on " + e.toString());
                }
            };
        }
        return val;
    }

    private static String checkQsonProperty(String jsonName, String prop, Class type, AnnotatedElement member) {
        if (member != null && member.isAnnotationPresent(QsonProperty.class)) {
            QsonProperty qp = member.getAnnotation(QsonProperty.class);
            if (!qp.value().isEmpty()) {
                if (jsonName != null && !jsonName.equals(qp.value())) {
                    throw new QsonException("Conflicting @QsonProperty values: " + type.getName() + "." + prop);
                }
                jsonName = qp.value();
            }
        }
        return jsonName;
    }

    private static void fields(Map<String, Field> fields, Class target) {
        if (Object.class.equals(target)) return;
        else fields(fields, target.getSuperclass());

        for (Field field : target.getDeclaredFields()) {
            if (field.isAnnotationPresent(QsonIgnore.class)
                    || field.isAnnotationPresent(QsonProperty.class)
                    || field.isAnnotationPresent(QsonIgnoreRead.class)
                    || field.isAnnotationPresent(QsonIgnoreWrite.class)
                    || field.isAnnotationPresent(QsonDate.class)
               ) {
                fields.put(field.getName(), field);
            }
        }
    }


    static boolean isSetterSignature(Method m) {
        return Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers())
                && m.getParameterCount() == 1;
    }

    private static boolean isSetter(Method m) {
        return isSetterSignature(m) &&
                m.getName().startsWith("set") && m.getName().length() > "set".length();
    }

    static boolean isGetter(Method m) {
        return isGetterSignature(m) &&
                ((m.getName().startsWith("get") && m.getName().length() > "get".length()) || (m.getName().startsWith("is")) && m.getName().length() > "is".length())
                ;
    }
    static boolean isGetterSignature(Method m) {
        return Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers())
                && m.getParameterCount() == 0 && !m.getReturnType().equals(void.class)
                && !m.getDeclaringClass().equals(Object.class);
    }
}
