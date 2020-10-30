package io.quarkus.qson.generator;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Types {
    /**
     * Checks to see if clz is a user object or contains one (i.e. List<UserObject>)
     * If it does, it adds the class and generic type to the referenceMap
     *
     * @param referenceMap
     * @param clz
     * @param genericType
     */
    public static void addReference(Map<Class, Type> referenceMap, Class clz, Type genericType) {
        if (clz.isPrimitive()) return;
        if (clz.equals(String.class)
                || clz.equals(Integer.class)
                || clz.equals(Short.class)
                || clz.equals(Long.class)
                || clz.equals(Byte.class)
                || clz.equals(Boolean.class)
                || clz.equals(Double.class)
                || clz.equals(Float.class)
                || clz.equals(Character.class)) {
            return;
        }
        if (Map.class.isAssignableFrom(clz)
                || List.class.isAssignableFrom(clz)
                || Set.class.isAssignableFrom(clz)) {
            if (genericType != null && genericType instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType)genericType;
                if (Map.class.isAssignableFrom(clz)) {
                    addReference(referenceMap, getRawType(pt.getActualTypeArguments()[1]), pt.getActualTypeArguments()[1]);
                } else {
                    addReference(referenceMap, getRawType(pt.getActualTypeArguments()[0]), pt.getActualTypeArguments()[0]);
                }

            }
        } else {
            referenceMap.put(clz, genericType);
        }
    }

    public static <T> Class<T> getRawType(Type type) {
        if (type instanceof Class<?>) {
            return (Class<T>) type;
        }
        if (type instanceof ParameterizedType) {
            if (((ParameterizedType) type).getRawType() instanceof Class<?>) {
                return (Class<T>) ((ParameterizedType) type).getRawType();
            }
        }
        if (type instanceof TypeVariable<?>) {
            TypeVariable<?> variable = (TypeVariable<?>) type;
            Type[] bounds = variable.getBounds();
            return getBound(bounds);
        }
        if (type instanceof WildcardType) {
            WildcardType wildcard = (WildcardType) type;
            return getBound(wildcard.getUpperBounds());
        }
        if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            Class<?> rawType = getRawType(genericArrayType.getGenericComponentType());
            if (rawType != null) {
                return (Class<T>) Array.newInstance(rawType, 0).getClass();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> getBound(Type[] bounds) {
        if (bounds.length == 0) {
            return (Class<T>) Object.class;
        } else {
            return getRawType(bounds[0]);
        }
    }

}
