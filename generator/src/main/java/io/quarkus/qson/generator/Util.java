package io.quarkus.qson.generator;

import io.quarkus.qson.util.Types;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Util {
    /**
     * Generate a classname from a pure generic type.  For example List&lt;Foo&gt; to what
     * a generated class would be for that  List_Foo
     *
     * @param type
     * @return
     */
    public static String generatedClassName(Type type) {
        return generatedClassName("io.quarkus.qson.generated", type);
    }

    /**
     * Generate a classname from a pure generic type.  For example List&lt;Foo&gt; to what
     * a generated class would be for that  List_Foo
     *
     * @param type
     * @return
     */
    public static String generatedClassName(String packageName, Type type) {
        String name = type.getTypeName()
                .replace(" ", "")
                .replace(',', '$')
                .replace("<", "_")
                .replace(">", "")
                .replace("java.util.", "")
                .replace("java.lang.", "")
                .replace('.', '_');
        return packageName + "." + name;
    }

    /**
     * Checks to see if clz is a user object or is a collection that contains one (i.e. List&lt;UserObject&gt;)
     * If it does, it adds the class and generic type to the referenceMap
     *
     * @param referenceSet
     * @param type
     */
    public static void addReference(QsonGenerator generator, Set<Type> referenceSet, Type type) {
        if (type instanceof Class) {
            Class clz = (Class)type;
            if (generator.hasMappingFor(clz) || isUserType(clz)) {
                referenceSet.add(type);
            }
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType)type;
            if (Map.class.isAssignableFrom(Types.getRawType(type))) {
                addReference(generator,referenceSet, pt.getActualTypeArguments()[1]);
            } else {
                addReference(generator,referenceSet, pt.getActualTypeArguments()[0]);
            }
        }
    }

    /**
     * Is supported date type
     *
     * @param type
     * @return
     */
    public static boolean isDateType(Class type) {
        return OffsetDateTime.class.equals(type) || Date.class.equals(type);
    }

    public static boolean isUserType(Class type) {
        if (type.isPrimitive()) return false;
        if (type.equals(String.class)
                || type.equals(Integer.class)
                || type.equals(Short.class)
                || type.equals(Long.class)
                || type.equals(Byte.class)
                || type.equals(Boolean.class)
                || type.equals(Double.class)
                || type.equals(Float.class)
                || type.equals(Character.class)
                || isDateType(type)
                || Map.class.isAssignableFrom(type)
                || List.class.isAssignableFrom(type)
                || Set.class.isAssignableFrom(type)
        ) {
            return false;
        }
        return true;
    }
}
