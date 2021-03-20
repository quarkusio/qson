package io.quarkus.qson.resteasy.deployment;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Type conversions and generic type manipulations
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 *
 * TODO: replace with Jandex type handling
 */
final class TypeUtil
{

   private static Object searchForInterfaceTemplateParameter(Class base, Class desiredInterface)
   {
      for (int i = 0; i < base.getInterfaces().length; i++)
      {
         Class intf = base.getInterfaces()[i];
         Type generic = base.getGenericInterfaces()[i];
         if (intf.equals(desiredInterface))
         {
            if (generic instanceof ParameterizedType)
            {
               ParameterizedType p = (ParameterizedType) generic;
               Type type = p.getActualTypeArguments()[0];
               Class rtn = getRawTypeNoException(type);
               if (rtn != null)
                  return rtn;
               return type;
            }
            else
            {
               return null;
            }
         }
         Object ret = searchForInterfaceTemplateParameterInSupertype(intf, generic, desiredInterface);
         if(ret != null)
            return ret;
      }
      return searchForInterfaceTemplateParameterInSupertype(base.getSuperclass(), base.getGenericSuperclass(), desiredInterface);
   }

   private static Object searchForInterfaceTemplateParameterInSupertype(Class<?> supertype, Type genericSupertype, Class<?> desiredInterface) {
       if (supertype == null || supertype.equals(Object.class))
           return null;
       Object rtn = searchForInterfaceTemplateParameter(supertype, desiredInterface);
       if (rtn == null || rtn instanceof Class)
           return rtn;
       if (!(rtn instanceof TypeVariable))
           return null;

       String name = ((TypeVariable) rtn).getName();
       int index = -1;
       TypeVariable[] variables = supertype.getTypeParameters();
       if (variables == null || variables.length < 1)
           return null;

       for (int i = 0; i < variables.length; i++)
       {
           if (variables[i].getName().equals(name))
               index = i;
       }
       if (index == -1)
           return null;

       if (!(genericSupertype instanceof ParameterizedType))
           return null;

       ParameterizedType pt = (ParameterizedType) genericSupertype;
       Type type = pt.getActualTypeArguments()[index];

       Class clazz = getRawTypeNoException(type);
       if (clazz != null)
           return clazz;
       return type;
   }

   public static Class<?> getRawType(Type type)
   {
      if (type instanceof Class<?>)
      {
         // type is a normal class.
         return (Class<?>) type;

      }
      else if (type instanceof ParameterizedType)
      {
         ParameterizedType parameterizedType = (ParameterizedType) type;
         Type rawType = parameterizedType.getRawType();
         return (Class<?>) rawType;
      }
      else if (type instanceof GenericArrayType)
      {
         final GenericArrayType genericArrayType = (GenericArrayType) type;
         final Class<?> componentRawType = getRawType(genericArrayType.getGenericComponentType());
         return Array.newInstance(componentRawType, 0).getClass();
      }
      else if (type instanceof TypeVariable)
      {
         final TypeVariable typeVar = (TypeVariable) type;
         if (typeVar.getBounds() != null && typeVar.getBounds().length > 0)
         {
            return getRawType(typeVar.getBounds()[0]);
         }
      }
      else if (type instanceof WildcardType)
      {
         WildcardType wildcardType = (WildcardType) type;
         Type[] upperBounds = wildcardType.getUpperBounds();
         if (upperBounds != null && upperBounds.length > 0)
         {
            return getRawType(upperBounds[0]);
         }
      }
      throw new RuntimeException("Unable to determine base class from Type");
   }

   public static Class<?> getRawTypeNoException(Type type)
   {
      if (type instanceof Class<?>)
      {
         // type is a normal class.
         return (Class<?>) type;

      }
      else if (type instanceof ParameterizedType)
      {
         ParameterizedType parameterizedType = (ParameterizedType) type;
         Type rawType = parameterizedType.getRawType();
         return (Class<?>) rawType;
      }
      else if (type instanceof GenericArrayType)
      {
         final GenericArrayType genericArrayType = (GenericArrayType) type;
         final Class<?> componentRawType = getRawType(genericArrayType.getGenericComponentType());
         return Array.newInstance(componentRawType, 0).getClass();
      }
      return null;
   }

   public static Type resolveTypeVariables(Class<?> root, Type type)
   {
      if (type instanceof TypeVariable)
      {
         Type newType = resolveTypeVariable(root, (TypeVariable) type);
         return (newType == null) ? type : newType;
      }
      else if (type instanceof ParameterizedType)
      {
         final ParameterizedType param = (ParameterizedType) type;
         final Type[] actuals = new Type[param.getActualTypeArguments().length];
         for (int i = 0; i < actuals.length; i++)
         {
            Type newType = resolveTypeVariables(root, param.getActualTypeArguments()[i]);
            actuals[i] = newType == null ? param.getActualTypeArguments()[i] : newType;
         }
         return new ResteasyParameterizedType(actuals, param.getRawType(), param.getOwnerType());
      }
      else if (type instanceof GenericArrayType)
      {
         GenericArrayType arrayType = (GenericArrayType) type;
         final Type componentType = resolveTypeVariables(root, arrayType.getGenericComponentType());
         if (componentType == null)
            return type;
         return (GenericArrayType) () -> componentType;
      }
      else
      {
         return type;
      }
   }

   /**
    * Finds an actual value of a type variable. The method looks in a class hierarchy for a class defining the variable
    * and returns the value if present.
    *
    * @param root root class
    * @param typeVariable type variable
    * @return actual type of the type variable
    */
   public static Type resolveTypeVariable(Class<?> root, TypeVariable<?> typeVariable)
   {
      if (typeVariable.getGenericDeclaration() instanceof Class<?>)
      {
         Class<?> classDeclaringTypeVariable = (Class<?>) typeVariable.getGenericDeclaration();
         Type[] types = findParameterizedTypes(root, classDeclaringTypeVariable);
         if (types == null)
            return null;
         for (int i = 0; i < types.length; i++)
         {
            TypeVariable<?> tv = classDeclaringTypeVariable.getTypeParameters()[i];
            if (tv.equals(typeVariable))
            {
               return types[i];
            }
         }
      }
      return null;
   }

   private static final Type[] EMPTY_TYPE_ARRAY =
   {};

   /**
    * Search for the given interface or class within the root's class/interface hierarchy.
    * If the searched for class/interface is a generic return an array of real types that fill it out.
    *
    * @param root root class
    * @param searchedFor searched class
    * @return for generic class/interface returns array of real types
    */
   public static Type[] findParameterizedTypes(Class<?> root, Class<?> searchedFor)
   {
      if (searchedFor.isInterface())
      {
         return findInterfaceParameterizedTypes(root, null, searchedFor);
      }
      return findClassParameterizedTypes(root, null, searchedFor);
   }

   public static Type[] findClassParameterizedTypes(Class<?> root, ParameterizedType rootType,
         Class<?> searchedForClass)
   {
      if (Object.class.equals(root))
         return null;

      Map<TypeVariable<?>, Type> typeVarMap = populateParameterizedMap(root, rootType);

      Class<?> superclass = root.getSuperclass();
      Type genericSuper = root.getGenericSuperclass();

      if (superclass.equals(searchedForClass))
      {
         return extractTypes(typeVarMap, genericSuper);
      }

      if (genericSuper instanceof ParameterizedType)
      {
         ParameterizedType intfParam = (ParameterizedType) genericSuper;
         Type[] types = findClassParameterizedTypes(superclass, intfParam, searchedForClass);
         if (types != null)
         {
            return extractTypeVariables(typeVarMap, types);
         }
      }
      else
      {
         Type[] types = findClassParameterizedTypes(superclass, null, searchedForClass);
         if (types != null)
         {
            return types;
         }
      }
      return null;
   }

   private static Map<TypeVariable<?>, Type> populateParameterizedMap(Class<?> root, ParameterizedType rootType)
   {
      Map<TypeVariable<?>, Type> typeVarMap = new HashMap<>();
      if (rootType != null)
      {
         TypeVariable<? extends Class<?>>[] vars = root.getTypeParameters();
         for (int i = 0; i < vars.length; i++)
         {
            typeVarMap.put(vars[i], rootType.getActualTypeArguments()[i]);
         }
      }
      return typeVarMap;
   }

   public static Type[] findInterfaceParameterizedTypes(Class<?> root, ParameterizedType rootType,
         Class<?> searchedForInterface)
   {
      Map<TypeVariable<?>, Type> typeVarMap = populateParameterizedMap(root, rootType);

      for (int i = 0; i < root.getInterfaces().length; i++)
      {
         Class<?> sub = root.getInterfaces()[i];
         Type genericSub = root.getGenericInterfaces()[i];
         if (sub.equals(searchedForInterface))
         {
            return extractTypes(typeVarMap, genericSub);
         }
      }

      for (int i = 0; i < root.getInterfaces().length; i++)
      {
         Type genericSub = root.getGenericInterfaces()[i];
         Class<?> sub = root.getInterfaces()[i];

         Type[] types = recurseSuperclassForInterface(searchedForInterface, typeVarMap, genericSub, sub);
         if (types != null)
            return types;
      }
      if (root.isInterface())
         return null;

      Class<?> superclass = root.getSuperclass();
      Type genericSuper = root.getGenericSuperclass();

      return recurseSuperclassForInterface(searchedForInterface, typeVarMap, genericSuper, superclass);
   }

   private static Type[] recurseSuperclassForInterface(Class<?> searchedForInterface,
         Map<TypeVariable<?>, Type> typeVarMap, Type genericSub, Class<?> sub)
   {
      if (genericSub instanceof ParameterizedType)
      {
         ParameterizedType intfParam = (ParameterizedType) genericSub;
         Type[] types = findInterfaceParameterizedTypes(sub, intfParam, searchedForInterface);
         if (types != null)
         {
            return extractTypeVariables(typeVarMap, types);
         }
      }
      else
      {
         Type[] types = findInterfaceParameterizedTypes(sub, null, searchedForInterface);
         if (types != null)
         {
            return types;
         }
      }
      return null;
   }

   /**
    * Resolve generic types to actual types.
    *
    * @param typeVarMap The mapping for generic types to actual types.
    * @param types The types to resolve.
    * @return An array of resolved method parameter types in declaration order.
    */
   private static Type[] extractTypeVariables(final Map<TypeVariable<?>, Type> typeVarMap, final Type[] types)
   {
      final Type[] resolvedMethodParameterTypes = new Type[types.length];

      for (int i = 0; i < types.length; i++)
      {
         final Type methodParameterType = types[i];

         if (methodParameterType instanceof TypeVariable<?>)
         {
            resolvedMethodParameterTypes[i] = typeVarMap.get(methodParameterType);
         }
         else
         {
            resolvedMethodParameterTypes[i] = methodParameterType;
         }
      }

      return resolvedMethodParameterTypes;
   }

   private static Type[] extractTypes(Map<TypeVariable<?>, Type> typeVarMap, Type genericSub)
   {
      if (genericSub instanceof ParameterizedType)
      {
         ParameterizedType param = (ParameterizedType) genericSub;
         Type[] types = param.getActualTypeArguments();

         return extractTypeVariables(typeVarMap, types);
      }
      else
      {
         return EMPTY_TYPE_ARRAY;
      }
   }

   public static class ResteasyParameterizedType implements ParameterizedType
   {

      private final Type[] actuals;

      private final Type rawType;

      private final Type ownerType;

      public ResteasyParameterizedType(final Type[] actuals, final Type rawType, final Type ownerType)
      {
         this.actuals = actuals;
         this.rawType = rawType;
         this.ownerType = ownerType;
      }

      @Override
      public Type[] getActualTypeArguments()
      {
         return actuals;
      }

      @Override
      public Type getRawType()
      {
         return rawType;
      }

      @Override
      public Type getOwnerType()
      {
         return ownerType;
      }

      @Override
      public boolean equals(Object other)
      {
         if (other == null)
            return false;
         if (!(other instanceof ParameterizedType))
            return false;
         ParameterizedType b = (ParameterizedType) other;
         // WARNING: contract defined by ParameterizedType
         return Arrays.equals(actuals, b.getActualTypeArguments()) && Objects.equals(rawType, b.getRawType())
               && Objects.equals(ownerType, b.getOwnerType());
      }

      @Override
      public int hashCode()
      {
         // WARNING: stay true to http://hg.openjdk.java.net/jdk8/jdk8/jdk/file/687fd7c7986d/src/share/classes/sun/reflect/generics/reflectiveObjects/ParameterizedTypeImpl.java
         return Arrays.hashCode(actuals) ^ Objects.hashCode(ownerType) ^ Objects.hashCode(rawType);
      }

      @Override
      public String toString()
      {
         StringBuilder sb = new StringBuilder();
         if (getOwnerType() != null)
            sb.append(getOwnerType()).append(".");
         sb.append(getRawType());
         if (actuals != null && actuals.length > 0)
         {
            sb.append("<");
            boolean first = true;
            for (Type actual : actuals)
            {
               if (first)
                  first = false;
               else
                  sb.append(", ");
               sb.append(actual);
            }
            sb.append(">");
         }
         return sb.toString();
      }

   }

}
