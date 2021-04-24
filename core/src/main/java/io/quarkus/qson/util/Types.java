package io.quarkus.qson.util;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

public class Types {

    /**
     * Search generic type arguments for target class type.
     *
     * @param type
     * @param target
     * @return
     */
    public static boolean typeContainsType(Type type, Class target) {
        if (type.equals(target)) return true;
        if (type instanceof ParameterizedType) {
            for (Type param : ((ParameterizedType)type).getActualTypeArguments()) {
                if (typeContainsType(param, target)) return true;
            }
        } else if (type instanceof GenericArrayType) {
            GenericArrayType arrayType = (GenericArrayType) type;
            return typeContainsType(arrayType.getGenericComponentType(), target);
        }
        return false;
    }

    public static boolean containsTypeVariable(Type type) {
        if (type instanceof TypeVariable) return true;
        if (type instanceof ParameterizedType) {
            for (Type param : ((ParameterizedType)type).getActualTypeArguments()) {
                if (containsTypeVariable(param)) return true;
            }
        } else if (type instanceof GenericArrayType) {
            GenericArrayType arrayType = (GenericArrayType) type;
            return containsTypeVariable(arrayType.getGenericComponentType());
        }

        return false;
    }
    /**
     * Some frameworks like Resteasy override Type to create their own ParameterizedTypes and
     * such.  As a result Type.getTypeName() is not consistent.  This attempts to make it consistent
     *
     * @param type
     * @return
     */
    public static String typename(Type type) {
        if (type instanceof Class) {
            return ((Class)type).getName();
        }
        StringBuilder sb = new StringBuilder();
        Type ownerType = null;
        Type rawType = type;
        Type[] params = null;
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType)type;
            params = pt.getActualTypeArguments();
            ownerType = pt.getOwnerType();
            rawType = pt.getRawType();
        }
        if (ownerType != null)
            sb.append(typename(ownerType)).append(".");
        sb.append(rawType);
        if (params != null && params.length > 0)
        {
            sb.append("<");
            boolean first = true;
            for (Type actual : params)
            {
                if (first)
                    first = false;
                else
                    sb.append(", ");
                sb.append(typename(actual));
            }
            sb.append(">");
        }
        return sb.toString();
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
