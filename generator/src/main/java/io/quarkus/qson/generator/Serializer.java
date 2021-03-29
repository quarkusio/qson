package io.quarkus.qson.generator;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.qson.util.Types;
import io.quarkus.qson.serializer.CollectionWriter;
import io.quarkus.qson.serializer.GenericObjectWriter;
import io.quarkus.qson.serializer.JsonWriter;
import io.quarkus.qson.serializer.MapWriter;
import io.quarkus.qson.serializer.QsonObjectWriter;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.ACC_FINAL;

public class Serializer {

    // constructor
    public static final String INIT = "<init>";
    // static initializer
    public static final String CLINIT = "<clinit>";

    public static class Builder {
        Class targetType;
        Type targetGenericType;
        ClassOutput output;
        String className;
        Map<Type, Class> referenced = new HashMap<>();
        List<PropertyReference> properties;
        GeneratorMetadata generator;
        ClassMetadata classGen;

        Builder(GeneratorMetadata generator) {
            this.generator = generator;
        }

        public Builder type(Class targetType) {
            this.targetType = targetType;
            return this;
        }

        public Builder generic(Type targetGenericType) {
            this.targetGenericType = targetGenericType;
            return this;
        }

        public Builder output(ClassOutput output) {
            this.output = output;
            return this;
        }
        /**
         * If generating a collection class deserializer, use this name instead
         * of the hardcoded name generated.
         *
         * @param name
         * @return
         */
        public Builder collectionClassName(String name) {
            this.className = name;
            return this;
        }

        public String className() {
            return className;
        }

        public Map<Type, Class> referenced() {
            return referenced;
        }

        public Builder properties(List<PropertyReference> properties) {
            this.properties = properties;
            return this;
        }

        public List<PropertyReference> properties() {
            return properties;
        }

        public Builder generate() {
            if (Map.class.equals(targetType)
                    || List.class.equals(targetType)
                    || Set.class.equals(targetType)) {

            }
            if (targetGenericType == null) targetGenericType = targetType;
            if (isGeneric(targetType, targetGenericType)) {
                // use the generic object writer
                className = GenericObjectWriter.class.getName();
                return this;
            } else if ((Map.class.isAssignableFrom(targetType)
                    || List.class.isAssignableFrom(targetType)
                    || Set.class.isAssignableFrom(targetType)) && hasCollectionWriter(targetType, targetGenericType)) {
                // generate a writer for the collection
                if (className == null) {
                    className = Util.generatedClassName(targetGenericType);
                    className += "__Serializer";
                }
                Serializer s = new Serializer(output, className, targetType, targetGenericType);
                s.generateCollection();
                Util.addReference(referenced, targetType, targetGenericType);
                return this;
            } else if (targetType.isEnum()) {
                Serializer s = new Serializer(output, targetType, targetGenericType);
                s.generateEnum();
                className = fqn(targetType, targetGenericType);
                return this;
            } else {
                Serializer s = new Serializer(output, targetType, targetGenericType);
                classGen = generator.metadataFor(targetType);
                if (classGen != null) {
                    if (classGen.isValue) {

                    } else {
                        properties = classGen.getProperties();
                    }

                }
                // NOTE: keep full property list around just in case we're doing serialization
                // and somebody wants to reuse.
                List<PropertyReference> tmp = new ArrayList<>();

                Method anyGetter = null;
                for (PropertyReference ref : properties) {
                    if (ref.getter != null) {
                        if (ref.isAny) {
                            anyGetter = ref.getter;
                            continue;
                        }
                        tmp.add(ref);
                        Util.addReference(referenced, ref.type, ref.genericType);
                    }
                }
                s.properties = tmp;
                s.anyGetter = anyGetter;
                s.generate();
                className = fqn(targetType, targetGenericType);
                return this;
            }
        }
    }

    ClassCreator creator;

    Class targetType;
    Type targetGenericType;
    List<PropertyReference> properties;
    String className;
    Method anyGetter;

    public static String name(Class clz, Type genericType) {
        return clz.getSimpleName() + "__Serializer";
    }

    public static String fqn(Class clz, Type genericType) {
        return clz.getName() + "__Serializer";
    }

    Serializer(ClassOutput classOutput, Class targetType, Type targetGenericType) {
        this(classOutput, fqn(targetType, targetGenericType), targetType, targetGenericType);
    }

    Serializer(ClassOutput classOutput, String className, Class targetType, Type targetGenericType) {
        this.targetType = targetType;
        this.targetGenericType = targetGenericType;
        this.className = className;
        creator = ClassCreator.builder().classOutput(classOutput)
                .className(className)
                .interfaces(QsonObjectWriter.class).build();
    }

    void generateEnum() {
        FieldCreator SERIALIZER = creator.getFieldCreator("SERIALIZER", fqn()).setModifiers(ACC_STATIC | ACC_PUBLIC);
        MethodCreator staticConstructor = creator.getMethodCreator(CLINIT, void.class);
        staticConstructor.setModifiers(ACC_STATIC);
        ResultHandle instance = staticConstructor.newInstance(MethodDescriptor.ofConstructor(fqn()));
        staticConstructor.writeStaticField(SERIALIZER.getFieldDescriptor(), instance);
        staticConstructor.returnValue(null);

        MethodCreator method = creator.getMethodCreator("write", void.class, JsonWriter.class, Object.class);
        ResultHandle writer = method.getMethodParam(0);
        AssignableResultHandle target = method.createVariable(targetType);
        method.assign(target, method.getMethodParam(1));
        method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "write", void.class, Enum.class),
                writer,
                target
        );
        method.returnValue(null);
        creator.close();
    }

    void generate() {
        singleton();
        writeMethod();
        creator.close();
    }

    void generateCollection() {


        MethodCreator staticConstructor = creator.getMethodCreator(CLINIT, void.class);
        staticConstructor.setModifiers(ACC_STATIC);
        collectionProperty(staticConstructor, targetType, targetGenericType, "collection");
        staticConstructor.returnValue(null);

        MethodCreator method = creator.getMethodCreator("write", void.class, JsonWriter.class, Object.class);
        ResultHandle jsonWriter = method.getMethodParam(0);
        AssignableResultHandle target = method.createVariable(targetType);
        method.assign(target, method.getMethodParam(1));

        if (Map.class.isAssignableFrom(targetType)) {
            if (hasCollectionWriter(targetType, targetGenericType)) {
                ResultHandle writer = getMapWriter(method, "collection", targetGenericType);
                method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "write", void.class, Map.class, QsonObjectWriter.class), jsonWriter,
                        target,
                        writer
                );
            } else {
                method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "write", void.class,Map.class), jsonWriter,
                        target);
            }
        } else {
            if (hasCollectionWriter(targetType, targetGenericType)) {
                ResultHandle writer = getCollectionWriter(method, "collection", targetGenericType);
                method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "write", void.class, Collection.class, QsonObjectWriter.class), jsonWriter,
                        target,
                        writer
                );
            } else {
                method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "write", void.class, Collection.class), jsonWriter,
                        target);
            }
        }
        method.returnValue(null);

        creator.close();
    }

    private void singleton() {
        FieldCreator SERIALIZER = creator.getFieldCreator("SERIALIZER", fqn()).setModifiers(ACC_STATIC | ACC_PUBLIC);


        MethodCreator staticConstructor = creator.getMethodCreator(CLINIT, void.class);
        staticConstructor.setModifiers(ACC_STATIC);
        ResultHandle instance = staticConstructor.newInstance(MethodDescriptor.ofConstructor(fqn()));
        staticConstructor.writeStaticField(SERIALIZER.getFieldDescriptor(), instance);
        for (PropertyReference getter : properties) {
            collectionField(staticConstructor, getter);
        }

        staticConstructor.returnValue(null);
    }

    private void collectionField(MethodCreator staticConstructor, PropertyReference getter) {
        Type genericType = getter.genericType;
        Class type = getter.type;
        String property = getter.propertyName;
        collectionProperty(staticConstructor, type, genericType, property);
    }

    private void collectionProperty(MethodCreator staticConstructor, Class type, Type genericType, String property) {
        if (genericType instanceof ParameterizedType) {
            if (Map.class.isAssignableFrom(type)) {
                ParameterizedType pt = (ParameterizedType) genericType;
                Type valueType = pt.getActualTypeArguments()[1];
                Class valueClass = Types.getRawType(valueType);
                if (hasNestedWriter(valueClass, valueType)) {
                    collectionField(staticConstructor, valueClass, valueType, property + "_n");
                }
            } else if (List.class.isAssignableFrom(type) || Set.class.isAssignableFrom(type)) {
                ParameterizedType pt = (ParameterizedType) genericType;
                Type valueType = pt.getActualTypeArguments()[0];
                Class valueClass = Types.getRawType(valueType);
                if (hasNestedWriter(valueClass, valueType)) {
                    collectionField(staticConstructor, valueClass, valueType, property + "_n");
                }
            } else {
                // ignore we don't need a special parser
            }
        }
    }

    private ResultHandle getNestedValueWriter(MethodCreator staticConstructor, Class type, Type genericType, String property) {
        if (!hasNestedWriter(type, genericType)) return null;
        if (isUserObject(type)) {
            return staticConstructor.readStaticField(FieldDescriptor.of(fqn(type, genericType), "SERIALIZER", fqn(type, genericType)));
        }
        collectionField(staticConstructor, type, genericType, property);
        return staticConstructor.readStaticField(FieldDescriptor.of(fqn(), property, QsonObjectWriter.class));
    }

    private void collectionField(MethodCreator staticConstructor, Class type, Type genericType, String property) {
        if (genericType instanceof ParameterizedType) {
            if (Map.class.isAssignableFrom(type)) {
                ParameterizedType pt = (ParameterizedType) genericType;
                Type valueType = pt.getActualTypeArguments()[1];
                Class valueClass = Types.getRawType(valueType);
                ResultHandle nested = getNestedValueWriter(staticConstructor, valueClass, valueType, property + "_n");
                if (nested == null) return;

                FieldCreator mapWriter = creator.getFieldCreator(property, QsonObjectWriter.class).setModifiers(ACC_STATIC | ACC_PRIVATE | ACC_FINAL);
                ResultHandle instance = staticConstructor.newInstance(MethodDescriptor.ofConstructor(MapWriter.class, QsonObjectWriter.class),
                        nested);
                staticConstructor.writeStaticField(mapWriter.getFieldDescriptor(), instance);
            } else if (List.class.isAssignableFrom(type) || Set.class.isAssignableFrom(type)) {
                ParameterizedType pt = (ParameterizedType) genericType;
                Type valueType = pt.getActualTypeArguments()[0];
                Class valueClass = Types.getRawType(valueType);
                ResultHandle nested = getNestedValueWriter(staticConstructor, valueClass, valueType, property + "_n");
                if (nested == null) return;

                FieldCreator mapWriter = creator.getFieldCreator(property, QsonObjectWriter.class).setModifiers(ACC_STATIC | ACC_PRIVATE | ACC_FINAL);
                ResultHandle instance = staticConstructor.newInstance(MethodDescriptor.ofConstructor(CollectionWriter.class, QsonObjectWriter.class),
                        nested);
                staticConstructor.writeStaticField(mapWriter.getFieldDescriptor(), instance);
            } else {
                // ignore we don't need a special parser
            }
        }
    }


    private void writeMethod() {
        MethodCreator method = creator.getMethodCreator("write", void.class, JsonWriter.class, Object.class);
        ResultHandle jsonWriter = method.getMethodParam(0);
        AssignableResultHandle target = method.createVariable(targetType);
        method.assign(target, method.getMethodParam(1));
        AssignableResultHandle comma = method.createVariable(boolean.class);
        method.assign(comma, method.load(false));
        boolean forceComma = false;
        method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeLCurley", void.class), jsonWriter);
        // todo support an interface as type
        for (PropertyReference getter : properties) {
            if (getter.type.equals(int.class)) {
                method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeProperty", void.class, String.class, int.class, boolean.class), jsonWriter,
                        method.load(getter.jsonName),
                        method.invokeVirtualMethod(MethodDescriptor.ofMethod(targetType, getter.getter.getName(), getter.type), target),
                        comma);
                if (!forceComma) {
                    method.assign(comma, method.load(true));
                    forceComma = true;
                }
            } else if (getter.type.equals(Integer.class)) {
                ResultHandle result = method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeProperty", boolean.class, String.class, Integer.class, boolean.class), jsonWriter,
                        method.load(getter.jsonName),
                        method.invokeVirtualMethod(MethodDescriptor.ofMethod(targetType, getter.getter.getName(), getter.type), target),
                        comma);
                if (!forceComma) method.assign(comma, result);
            } else if (getter.type.equals(short.class)) {
                method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeProperty", void.class, String.class, short.class, boolean.class), jsonWriter,
                        method.load(getter.jsonName),
                        method.invokeVirtualMethod(MethodDescriptor.ofMethod(targetType, getter.getter.getName(), getter.type), target),
                        comma);
                if (!forceComma) {
                    method.assign(comma, method.load(true));
                    forceComma = true;
                }
            } else if (getter.type.equals(Short.class)) {
                ResultHandle result = method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeProperty", boolean.class, String.class, Short.class, boolean.class), jsonWriter,
                        method.load(getter.jsonName),
                        method.invokeVirtualMethod(MethodDescriptor.ofMethod(targetType, getter.getter.getName(), getter.type), target),
                        comma);
                if (!forceComma) method.assign(comma, result);
            } else if (getter.type.equals(long.class)) {
                method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeProperty", void.class, String.class, long.class, boolean.class), jsonWriter,
                        method.load(getter.jsonName),
                        method.invokeVirtualMethod(MethodDescriptor.ofMethod(targetType, getter.getter.getName(), getter.type), target),
                        comma);
                if (!forceComma) {
                    method.assign(comma, method.load(true));
                    forceComma = true;
                }
            } else if (getter.type.equals(Long.class)) {
                ResultHandle result = method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeProperty", boolean.class, String.class, Long.class, boolean.class), jsonWriter,
                        method.load(getter.jsonName),
                        method.invokeVirtualMethod(MethodDescriptor.ofMethod(targetType, getter.getter.getName(), getter.type), target),
                        comma);
                if (!forceComma) method.assign(comma, result);
            } else if (getter.type.equals(byte.class)) {
                method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeProperty", void.class, String.class, byte.class, boolean.class), jsonWriter,
                        method.load(getter.jsonName),
                        method.invokeVirtualMethod(MethodDescriptor.ofMethod(targetType, getter.getter.getName(), getter.type), target),
                        comma);
                if (!forceComma) {
                    method.assign(comma, method.load(true));
                    forceComma = true;
                }
            } else if (getter.type.equals(Byte.class)) {
                ResultHandle result = method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeProperty", boolean.class, String.class, Byte.class, boolean.class), jsonWriter,
                        method.load(getter.jsonName),
                        method.invokeVirtualMethod(MethodDescriptor.ofMethod(targetType, getter.getter.getName(), getter.type), target),
                        comma);
                if (!forceComma) method.assign(comma, result);
            } else if (getter.type.equals(boolean.class)) {
                method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeProperty", void.class, String.class, boolean.class, boolean.class), jsonWriter,
                        method.load(getter.jsonName),
                        method.invokeVirtualMethod(MethodDescriptor.ofMethod(targetType, getter.getter.getName(), getter.type), target),
                        comma);
                if (!forceComma) {
                    method.assign(comma, method.load(true));
                    forceComma = true;
                }
            } else if (getter.type.equals(Boolean.class)) {
                ResultHandle result = method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeProperty", boolean.class, String.class, Boolean.class, boolean.class), jsonWriter,
                        method.load(getter.jsonName),
                        method.invokeVirtualMethod(MethodDescriptor.ofMethod(targetType, getter.getter.getName(), getter.type), target),
                        comma);
                if (!forceComma) method.assign(comma, result);
            } else if (getter.type.equals(float.class)) {
                method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeProperty", void.class, String.class, float.class, boolean.class), jsonWriter,
                        method.load(getter.jsonName),
                        method.invokeVirtualMethod(MethodDescriptor.ofMethod(targetType, getter.getter.getName(), getter.type), target),
                        comma);
                if (!forceComma) {
                    method.assign(comma, method.load(true));
                    forceComma = true;
                }
            } else if (getter.type.equals(Float.class)) {
                ResultHandle result = method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeProperty", boolean.class, String.class, Float.class, boolean.class), jsonWriter,
                        method.load(getter.jsonName),
                        method.invokeVirtualMethod(MethodDescriptor.ofMethod(targetType, getter.getter.getName(), getter.type), target),
                        comma);
                if (!forceComma) method.assign(comma, result);
            } else if (getter.type.equals(double.class)) {
                method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeProperty", void.class, String.class, double.class, boolean.class), jsonWriter,
                        method.load(getter.jsonName),
                        method.invokeVirtualMethod(MethodDescriptor.ofMethod(targetType, getter.getter.getName(), getter.type), target),
                        comma);
                if (!forceComma) {
                    method.assign(comma, method.load(true));
                    forceComma = true;
                }
            } else if (getter.type.equals(Double.class)) {
                ResultHandle result = method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeProperty", boolean.class, String.class, Double.class, boolean.class), jsonWriter,
                        method.load(getter.jsonName),
                        method.invokeVirtualMethod(MethodDescriptor.ofMethod(targetType, getter.getter.getName(), getter.type), target),
                        comma);
                if (!forceComma) method.assign(comma, result);
            } else if (getter.type.equals(char.class)) {
                method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeProperty", void.class, String.class, char.class, boolean.class), jsonWriter,
                        method.load(getter.jsonName),
                        method.invokeVirtualMethod(MethodDescriptor.ofMethod(targetType, getter.getter.getName(), getter.type), target),
                        comma);
                if (!forceComma) {
                    method.assign(comma, method.load(true));
                    forceComma = true;
                }
            } else if (getter.type.equals(Character.class)) {
                ResultHandle result = method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeProperty", boolean.class, String.class, Character.class, boolean.class), jsonWriter,
                        method.load(getter.jsonName),
                        method.invokeVirtualMethod(MethodDescriptor.ofMethod(targetType, getter.getter.getName(), getter.type), target),
                        comma);
                if (!forceComma) method.assign(comma, result);
            } else if (getter.type.equals(String.class)) {
                ResultHandle result = method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeProperty", boolean.class, String.class, String.class, boolean.class), jsonWriter,
                        method.load(getter.jsonName),
                        method.invokeVirtualMethod(MethodDescriptor.ofMethod(targetType, getter.getter.getName(), getter.type), target),
                        comma);
                if (!forceComma) method.assign(comma, result);
            } else if (Map.class.isAssignableFrom(getter.type)) {
                if (hasCollectionWriter(getter)) {
                    ResultHandle result = method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeProperty", boolean.class, String.class, Map.class, QsonObjectWriter.class, boolean.class), jsonWriter,
                            method.load(getter.jsonName),
                            method.invokeVirtualMethod(MethodDescriptor.ofMethod(targetType, getter.getter.getName(), getter.type), target),
                            getMapWriter(method, getter),
                            comma
                    );
                    if (!forceComma) method.assign(comma, result);
                } else {
                    ResultHandle result = method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeProperty", boolean.class, String.class, Map.class, boolean.class), jsonWriter,
                            method.load(getter.jsonName),
                            method.invokeVirtualMethod(MethodDescriptor.ofMethod(targetType, getter.getter.getName(), getter.type), target),
                            comma);
                    if (!forceComma) method.assign(comma, result);
                }
            } else if (Collection.class.isAssignableFrom(getter.type)) {
                if (hasCollectionWriter(getter)) {
                    ResultHandle result = method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeProperty", boolean.class, String.class, Collection.class, QsonObjectWriter.class, boolean.class), jsonWriter,
                            method.load(getter.jsonName),
                            method.invokeVirtualMethod(MethodDescriptor.ofMethod(targetType, getter.getter.getName(), getter.type), target),
                            getCollectionWriter(method, getter),
                            comma
                    );
                    if (!forceComma) method.assign(comma, result);
                } else {
                    ResultHandle result = method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeProperty", boolean.class, String.class, Collection.class, boolean.class), jsonWriter,
                            method.load(getter.jsonName),
                            method.invokeVirtualMethod(MethodDescriptor.ofMethod(targetType, getter.getter.getName(), getter.type), target),
                            comma);
                    if (!forceComma) method.assign(comma, result);
                }
            } else if (getter.type.equals(Object.class)) {
                ResultHandle result = method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeObjectProperty", boolean.class, String.class, Object.class, boolean.class), jsonWriter,
                        method.load(getter.jsonName),
                        method.invokeVirtualMethod(MethodDescriptor.ofMethod(targetType, getter.getter.getName(), getter.type), target),
                        comma);
                if (!forceComma) method.assign(comma, result);
            } else {
                ResultHandle result = method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeObjectProperty", boolean.class, String.class, Object.class, QsonObjectWriter.class, boolean.class), jsonWriter,
                        method.load(getter.jsonName),
                        method.invokeVirtualMethod(MethodDescriptor.ofMethod(targetType, getter.getter.getName(), getter.type), target),
                        method.readStaticField(FieldDescriptor.of(fqn(getter.type, getter.genericType), "SERIALIZER", fqn(getter.type, getter.genericType))),
                        comma
                );
                if (!forceComma) method.assign(comma, result);
            }
        }
        if (anyGetter != null) {
            method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeAny", boolean.class, Map.class, boolean.class), jsonWriter,
                    method.invokeVirtualMethod(MethodDescriptor.ofMethod(anyGetter), target),
                    comma
            );
        }
        method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeRCurley", void.class), jsonWriter);
        method.returnValue(null);
    }

    private ResultHandle getCollectionWriter(MethodCreator method, PropertyReference getter) {
        String property = getter.propertyName;
        Type genericType = getter.genericType;
        return getCollectionWriter(method, property, (ParameterizedType) genericType);
    }

    private ResultHandle getCollectionWriter(MethodCreator method, String property, Type genericType) {
        ParameterizedType pt = (ParameterizedType)genericType;
        Class valueClass = Types.getRawType(pt.getActualTypeArguments()[0]);
        Type valueType = pt.getActualTypeArguments()[0];
        return getWriter(method, property, valueClass, valueType);
    }

    private ResultHandle getWriter(MethodCreator method, String property, Class valueClass, Type valueType) {
        if (isUserObject(valueClass)) {
            return method.readStaticField(FieldDescriptor.of(fqn(valueClass, valueType), "SERIALIZER", fqn(valueClass, valueType)));
        } else {
            return method.readStaticField(FieldDescriptor.of(fqn(), property + "_n", QsonObjectWriter.class));
        }
    }

    private ResultHandle getMapWriter(MethodCreator method, PropertyReference getter) {
        String property = getter.propertyName;
        Type genericType = getter.genericType;
        return getMapWriter(method, property, (ParameterizedType) genericType);
    }

    private ResultHandle getMapWriter(MethodCreator method, String property, Type genericType) {
        ParameterizedType pt = (ParameterizedType)genericType;
        Class valueClass = Types.getRawType(pt.getActualTypeArguments()[1]);
        Type valueType = pt.getActualTypeArguments()[1];
        return getWriter(method, property, valueClass, valueType);
    }

    private static boolean isUserObject(Class type) {
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
                || Map.class.isAssignableFrom(type)
                || List.class.isAssignableFrom(type)
                || Set.class.isAssignableFrom(type)
        ) {
            return false;
        }
        return true;
    }

    private static boolean isGeneric(Class type, Type generic) {
        if (type.isPrimitive()) return true;
        if (type.equals(String.class)
                || type.equals(Integer.class)
                || type.equals(Short.class)
                || type.equals(Long.class)
                || type.equals(Byte.class)
                || type.equals(Boolean.class)
                || type.equals(Double.class)
                || type.equals(Float.class)
                || type.equals(Character.class)) {
            return true;
        }
        if (Map.class.isAssignableFrom(type)
                || List.class.isAssignableFrom(type)
                || Set.class.isAssignableFrom(type)) {
            return hasCollectionWriter(type, generic) == false;
        }
        return false;
    }

    private boolean hasCollectionWriter(PropertyReference getter) {
        Class type = getter.type;
        Type genericType = getter.genericType;
        return hasCollectionWriter(type, genericType);
    }

    private static boolean hasCollectionWriter(Class type, Type genericType) {
        if (!(genericType instanceof ParameterizedType)) return false;
        if (Map.class.isAssignableFrom(type)) {
            ParameterizedType pt = (ParameterizedType) genericType;
            Type valueType = pt.getActualTypeArguments()[1];
            Class valueClass = Types.getRawType(valueType);
            return hasNestedWriter(valueClass ,valueType);
        } else if (Collection.class.isAssignableFrom(type)) {
            ParameterizedType pt = (ParameterizedType)genericType;
            Class valueClass = Types.getRawType(pt.getActualTypeArguments()[0]);
            Type valueGenericType = pt.getActualTypeArguments()[0];
            return hasNestedWriter(valueClass, valueGenericType);
        } else {
            return false;
        }
    }

    private static boolean hasNestedWriter(Class type, Type genericType) {
        if (isUserObject(type)) return true;
        if (!Map.class.isAssignableFrom(type)
                && !List.class.isAssignableFrom(type)
                && !Set.class.isAssignableFrom(type)
        ) {
            return false;
        }

        if (!(genericType instanceof ParameterizedType)) return false;

        if (Map.class.isAssignableFrom(type)) {
            ParameterizedType pt = (ParameterizedType)genericType;
            Class valueClass = Types.getRawType(pt.getActualTypeArguments()[1]);
            Type valueGenericType = pt.getActualTypeArguments()[1];
            return hasNestedWriter(valueClass, valueGenericType);
        } else if (Collection.class.isAssignableFrom(type)) {
            ParameterizedType pt = (ParameterizedType)genericType;
            Class valueClass = Types.getRawType(pt.getActualTypeArguments()[0]);
            Type valueGenericType = pt.getActualTypeArguments()[0];
            return hasNestedWriter(valueClass, valueGenericType);
        }
        return false;
    }


    private String fqn() {
        return className;
    }


}
