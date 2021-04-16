package io.quarkus.qson.generator;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.qson.QsonDate;
import io.quarkus.qson.QsonException;
import io.quarkus.qson.writer.DateNumberWriter;
import io.quarkus.qson.writer.DateUtilStringWriter;
import io.quarkus.qson.writer.OffsetDateTimeStringWriter;
import io.quarkus.qson.util.Types;
import io.quarkus.qson.writer.CollectionWriter;
import io.quarkus.qson.writer.GenericObjectWriter;
import io.quarkus.qson.writer.JsonWriter;
import io.quarkus.qson.writer.MapWriter;
import io.quarkus.qson.writer.QsonObjectWriter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.ACC_FINAL;

public class WriterGenerator {

    // constructor
    public static final String INIT = "<init>";
    // static initializer
    public static final String CLINIT = "<clinit>";

    public static class Builder {
        Type type;
        ClassOutput output;
        String className;
        Set<Type> referenced = new HashSet<>();
        List<PropertyMapping> properties;
        QsonGenerator generator;
        ClassMapping classGen;

        Builder(QsonGenerator generator) {
            this.generator = generator;
        }

        public Builder type(Type targetType) {
            this.type = targetType;
            return this;
        }

        public Builder output(ClassOutput output) {
            this.output = output;
            return this;
        }

        public String className() {
            return className;
        }

        public Set<Type> referenced() {
            return referenced;
        }

        public Builder properties(List<PropertyMapping> properties) {
            this.properties = properties;
            return this;
        }

        public List<PropertyMapping> properties() {
            return properties;
        }

        public Builder generate() {
            if (type instanceof Class) {
                Class targetType = (Class) type;
                if (isGeneric(generator, targetType, type)) {
                    className = GenericObjectWriter.class.getName();
                    return this;
                } else if (targetType.isEnum()) {
                    WriterGenerator s = new WriterGenerator(output, targetType, type);
                    s.generateEnum();
                    className = fqn(targetType, type);
                    return this;
                } else {
                    WriterGenerator s = new WriterGenerator(output, targetType, type);
                    classGen = generator.mappingFor(targetType);
                    s.mapping = classGen;
                    s.generator = generator;
                    if (classGen != null) {
                        if (classGen.isValue()) {
                            s.generateValueClass();
                            className = fqn(targetType, type);
                            return this;
                        } else {
                            properties = classGen.getProperties();
                        }

                    }
                    // remove any's from property list
                    List<PropertyMapping> tmp = new ArrayList<>();

                    Method anyGetter = null;
                    for (PropertyMapping ref : properties) {
                        if (ref.getter != null) {
                            if (ref.isAny) {
                                anyGetter = ref.getter;
                                continue;
                            }
                            tmp.add(ref);
                            Util.addReference(generator, referenced, ref.genericType);
                        }
                    }
                    s.properties = tmp;
                    s.anyGetter = anyGetter;
                    s.generate();
                    className = fqn(targetType, type);
                    return this;
                }
            } else if (type instanceof ParameterizedType) {
                Class targetType = Types.getRawType(type);
                if (Map.class.isAssignableFrom(targetType)
                        || List.class.isAssignableFrom(targetType)
                        || Set.class.isAssignableFrom(targetType)) {
                    if (hasCollectionWriter(generator, targetType, type)) {
                        // generate a writer for the collection
                        if (className == null) {
                            className = Util.generatedClassName(type);
                            className += "__Serializer";
                        }
                        WriterGenerator s = new WriterGenerator(output, className, targetType, type);
                        s.generateCollection();
                        Util.addReference(generator, referenced, type);
                        return this;
                    } else {
                        className = GenericObjectWriter.class.getName();
                        return this;
                    }
                }
            }
            throw new QsonException("Unsupported generic type for serializer generation: " + type.getTypeName());
        }
    }

    ClassCreator creator;

    Class targetType;
    Type targetGenericType;
    List<PropertyMapping> properties;
    String className;
    Method anyGetter;
    ClassMapping mapping;
    QsonGenerator generator;

    public static String name(Class clz, Type genericType) {
        return clz.getSimpleName() + "__Serializer";
    }

    public static String fqn(Class clz, Type genericType) {
        String prefix = "";
        if (clz.getName().startsWith("java")) {
            prefix = "io.quarkus.qson.";
        }
        return prefix + clz.getName() + "__Serializer";
    }

    WriterGenerator(ClassOutput classOutput, Class targetType, Type targetGenericType) {
        this(classOutput, fqn(targetType, targetGenericType), targetType, targetGenericType);
    }

    WriterGenerator(ClassOutput classOutput, String className, Class targetType, Type targetGenericType) {
        this.targetType = targetType;
        this.targetGenericType = targetGenericType;
        this.className = className;
        creator = ClassCreator.builder().classOutput(classOutput)
                .className(className)
                .interfaces(QsonObjectWriter.class).build();
    }

    public static final String DEFAULT_OFFSET_DATE_TIME = "_defaultOffsetDateTime";
    public static final String DEFAULT_DATE_UTIL = "_defaultDateUtil";

    private String getPropertyDateWriter(PropertyMapping ref) {
        return ref.getPropertyName() + "_dateParser";
    }

    private ResultHandle allocateDateUtilWriter(BytecodeCreator scope, QsonDate.Format format, String pattern) {
        if (pattern != null) {
            return scope.newInstance(MethodDescriptor.ofConstructor(DateUtilStringWriter.class, String.class), scope.load(pattern));
        } else if (format == QsonDate.Format.MILLISECONDS) {
            return scope.readStaticField(FieldDescriptor.of(DateNumberWriter.class, "DATE_UTIL_MILLISECONDS", QsonObjectWriter.class));
        } else if (format == QsonDate.Format.SECONDS) {
            return scope.readStaticField(FieldDescriptor.of(DateNumberWriter.class, "DATE_UTIL_SECONDS", QsonObjectWriter.class));
        } else if (format == QsonDate.Format.ISO_8601_OFFSET_DATE_TIME) {
            return scope.readStaticField(FieldDescriptor.of(DateUtilStringWriter.class, "ISO_8601_OFFSET_DATE_TIME", DateUtilStringWriter.class));
        } else if (format == QsonDate.Format.RFC_1123_DATE_TIME) {
            return scope.readStaticField(FieldDescriptor.of(DateUtilStringWriter.class, "RFC_1123_DATE_TIME", DateUtilStringWriter.class));
        } else {
            throw new QsonException("Unsupported");
        }
    }

    private ResultHandle allocateOffsetDateTimeWriter(BytecodeCreator scope, QsonDate.Format format, String pattern) {
        if (pattern != null) {
            return scope.newInstance(MethodDescriptor.ofConstructor(OffsetDateTimeStringWriter.class, String.class), scope.load(pattern));
        } else if (format == QsonDate.Format.MILLISECONDS) {
            return scope.readStaticField(FieldDescriptor.of(DateNumberWriter.class, "OFFSET_DATE_TIME_MILLISECONDS", QsonObjectWriter.class));
        } else if (format == QsonDate.Format.SECONDS) {
            return scope.readStaticField(FieldDescriptor.of(DateNumberWriter.class, "OFFSET_DATE_TIME_SECONDS", QsonObjectWriter.class));
        } else if (format == QsonDate.Format.ISO_8601_OFFSET_DATE_TIME) {
            return scope.readStaticField(FieldDescriptor.of(OffsetDateTimeStringWriter.class, "ISO_8601_OFFSET_DATE_TIME", OffsetDateTimeStringWriter.class));
        } else if (format == QsonDate.Format.RFC_1123_DATE_TIME) {
            return scope.readStaticField(FieldDescriptor.of(OffsetDateTimeStringWriter.class, "RFC_1123_DATE_TIME", OffsetDateTimeStringWriter.class));
        } else {
            throw new QsonException("Unsupported");
        }
    }


    private void processDateProperties(MethodCreator staticConstructor) {
        FieldCreator defaultDateUtil = null;
        FieldCreator defaultOffsetDateTime = null;
        for (PropertyMapping ref : properties) {
            if (Types.typeContainsType(ref.genericType, Date.class)) {
                if (ref.dateFormat == null) {
                    if (defaultDateUtil == null && !generator.hasMappingFor(Date.class)) {
                        defaultDateUtil = creator.getFieldCreator(DEFAULT_DATE_UTIL, QsonObjectWriter.class).setModifiers(ACC_STATIC | ACC_PRIVATE | ACC_FINAL);
                        staticConstructor.writeStaticField(defaultDateUtil.getFieldDescriptor(), allocateDateUtilWriter(staticConstructor, generator.getDateFormat(), null));
                    }
                } else {
                    FieldCreator dateProperty = creator.getFieldCreator(getPropertyDateWriter(ref), QsonObjectWriter.class).setModifiers(ACC_STATIC | ACC_PRIVATE | ACC_FINAL);
                    staticConstructor.writeStaticField(dateProperty.getFieldDescriptor(), allocateDateUtilWriter(staticConstructor, ref.getDateFormat(), ref.getDatePattern()));
                }
            } else if (Types.typeContainsType(ref.genericType, OffsetDateTime.class)) {
                if (ref.dateFormat == null) {
                    if (defaultOffsetDateTime == null && !generator.hasMappingFor(OffsetDateTime.class)) {
                        defaultOffsetDateTime = creator.getFieldCreator(DEFAULT_OFFSET_DATE_TIME, QsonObjectWriter.class).setModifiers(ACC_STATIC | ACC_PRIVATE | ACC_FINAL);
                        staticConstructor.writeStaticField(defaultOffsetDateTime.getFieldDescriptor(), allocateOffsetDateTimeWriter(staticConstructor, generator.getDateFormat(), null));
                    }
                } else {
                    FieldCreator dateProperty = creator.getFieldCreator(getPropertyDateWriter(ref), QsonObjectWriter.class).setModifiers(ACC_STATIC | ACC_PRIVATE | ACC_FINAL);
                    staticConstructor.writeStaticField(dateProperty.getFieldDescriptor(), allocateOffsetDateTimeWriter(staticConstructor, ref.getDateFormat(), ref.getDatePattern()));
                }
            }
        }
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

    void generateValueClass() {
        FieldCreator SERIALIZER = creator.getFieldCreator("SERIALIZER", fqn()).setModifiers(ACC_STATIC | ACC_PUBLIC);
        MethodCreator staticConstructor = creator.getMethodCreator(CLINIT, void.class);
        staticConstructor.setModifiers(ACC_STATIC);
        ResultHandle instance = staticConstructor.newInstance(MethodDescriptor.ofConstructor(fqn()));
        staticConstructor.writeStaticField(SERIALIZER.getFieldDescriptor(), instance);
        staticConstructor.returnValue(null);

        MethodCreator method = creator.getMethodCreator("write", void.class, JsonWriter.class, Object.class);
        ResultHandle jsonWriter = method.getMethodParam(0);
        AssignableResultHandle target = method.createVariable(targetType);
        method.assign(target, method.getMethodParam(1));
        Method valueGetter = mapping.getValueWriter();
        Class type = valueGetter.getReturnType();
        boolean isStatic = Modifier.isStatic(valueGetter.getModifiers());

        ResultHandle val;
        if (isStatic) {
            val = method.invokeStaticMethod(MethodDescriptor.ofMethod(valueGetter), target);
        } else {
            val = method.invokeVirtualMethod(MethodDescriptor.ofMethod(valueGetter), target);
        }
        method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "write", void.class, type),
                jsonWriter,
                val);
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
        if (Types.typeContainsType(targetGenericType, Date.class) && !generator.hasMappingFor(Date.class)) {
            FieldCreator defaultDateUtil = creator.getFieldCreator(DEFAULT_DATE_UTIL, QsonObjectWriter.class).setModifiers(ACC_STATIC | ACC_PRIVATE | ACC_FINAL);
            staticConstructor.writeStaticField(defaultDateUtil.getFieldDescriptor(), allocateDateUtilWriter(staticConstructor, generator.getDateFormat(), null));
        } else if (Types.typeContainsType(targetGenericType, OffsetDateTime.class) && !generator.hasMappingFor(OffsetDateTime.class)) {
            FieldCreator defaultOffsetDateTime = creator.getFieldCreator(DEFAULT_OFFSET_DATE_TIME, QsonObjectWriter.class).setModifiers(ACC_STATIC | ACC_PRIVATE | ACC_FINAL);
            staticConstructor.writeStaticField(defaultOffsetDateTime.getFieldDescriptor(), allocateOffsetDateTimeWriter(staticConstructor, generator.getDateFormat(), null));
        }

        staticConstructor.setModifiers(ACC_STATIC);
        collectionProperty(null, staticConstructor, targetType, targetGenericType, "collection");
        staticConstructor.returnValue(null);

        MethodCreator method = creator.getMethodCreator("write", void.class, JsonWriter.class, Object.class);
        ResultHandle jsonWriter = method.getMethodParam(0);
        AssignableResultHandle target = method.createVariable(targetType);
        method.assign(target, method.getMethodParam(1));

        if (Map.class.isAssignableFrom(targetType)) {
            if (hasCollectionWriter(generator, targetType, targetGenericType)) {
                ResultHandle writer = getMapWriter(null, method, "collection", targetGenericType);
                method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "write", void.class, Map.class, QsonObjectWriter.class), jsonWriter,
                        target,
                        writer
                );
            } else {
                method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "write", void.class, Map.class), jsonWriter,
                        target);
            }
        } else {
            if (hasCollectionWriter(generator, targetType, targetGenericType)) {
                ResultHandle writer = getCollectionWriter(null, method, "collection", targetGenericType);
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
        processDateProperties(staticConstructor);
        for (PropertyMapping getter : properties) {
            collectionField(staticConstructor, getter);
        }

        staticConstructor.returnValue(null);
    }

    private void collectionField(MethodCreator staticConstructor, PropertyMapping getter) {
        Type genericType = getter.genericType;
        Class type = getter.type;
        String property = getter.propertyName;
        collectionProperty(getter, staticConstructor, type, genericType, property);
    }

    private void collectionProperty(PropertyMapping ref, MethodCreator staticConstructor, Class type, Type genericType, String property) {
        if (genericType instanceof ParameterizedType) {
            if (Map.class.isAssignableFrom(type)) {
                ParameterizedType pt = (ParameterizedType) genericType;
                Type valueType = pt.getActualTypeArguments()[1];
                Class valueClass = Types.getRawType(valueType);
                if (hasNestedWriter(generator, valueClass, valueType)) {
                    collectionField(ref, staticConstructor, valueClass, valueType, property + "_n");
                }
            } else if (List.class.isAssignableFrom(type) || Set.class.isAssignableFrom(type)) {
                ParameterizedType pt = (ParameterizedType) genericType;
                Type valueType = pt.getActualTypeArguments()[0];
                Class valueClass = Types.getRawType(valueType);
                if (hasNestedWriter(generator, valueClass, valueType)) {
                    collectionField(ref, staticConstructor, valueClass, valueType, property + "_n");
                }
            } else {
                // ignore we don't need a special parser
            }
        }
    }

    private ResultHandle getNestedValueWriter(PropertyMapping ref, MethodCreator staticConstructor, Class type, Type genericType, String property) {
        if (Util.isDateType(type) && !generator.hasMappingFor(type)) {
            if (type.equals(OffsetDateTime.class)) {
                return getOffsetDateTimeWriter(ref, staticConstructor);
            } else if (type.equals(Date.class)) {
                return getDateUtilWriter(ref, staticConstructor);
            }
        }
        if (!hasNestedWriter(generator, type, genericType)) return null;
        if (Util.isUserType(generator, type)) {
            return staticConstructor.readStaticField(FieldDescriptor.of(fqn(type, genericType), "SERIALIZER", fqn(type, genericType)));
        }
        collectionField(ref, staticConstructor, type, genericType, property);
        return staticConstructor.readStaticField(FieldDescriptor.of(fqn(), property, QsonObjectWriter.class));
    }

    private void collectionField(PropertyMapping ref, MethodCreator staticConstructor, Class type, Type genericType, String property) {
        if (genericType instanceof ParameterizedType) {
            if (Map.class.isAssignableFrom(type)) {
                ParameterizedType pt = (ParameterizedType) genericType;
                Type valueType = pt.getActualTypeArguments()[1];
                Class valueClass = Types.getRawType(valueType);
                ResultHandle nested = getNestedValueWriter(ref, staticConstructor, valueClass, valueType, property + "_n");
                if (nested == null) return;

                FieldCreator mapWriter = creator.getFieldCreator(property, QsonObjectWriter.class).setModifiers(ACC_STATIC | ACC_PRIVATE | ACC_FINAL);
                ResultHandle instance = staticConstructor.newInstance(MethodDescriptor.ofConstructor(MapWriter.class, QsonObjectWriter.class),
                        nested);
                staticConstructor.writeStaticField(mapWriter.getFieldDescriptor(), instance);
            } else if (List.class.isAssignableFrom(type) || Set.class.isAssignableFrom(type)) {
                ParameterizedType pt = (ParameterizedType) genericType;
                Type valueType = pt.getActualTypeArguments()[0];
                Class valueClass = Types.getRawType(valueType);
                ResultHandle nested = getNestedValueWriter(ref, staticConstructor, valueClass, valueType, property + "_n");
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

    private ResultHandle getOffsetDateTimeWriter(PropertyMapping ref, BytecodeCreator scope) {
        String field = DEFAULT_OFFSET_DATE_TIME;
        if (ref != null && ref.getDateFormat() != null) {
            field = getPropertyDateWriter(ref);
        }
        FieldDescriptor parserField = FieldDescriptor.of(fqn(), field, QsonObjectWriter.class);
        return scope.readStaticField(parserField);
    }

    private ResultHandle getDateUtilWriter(PropertyMapping ref, BytecodeCreator scope) {
        String field = DEFAULT_DATE_UTIL;
        if (ref != null && ref.getDateFormat() != null) {
            field = getPropertyDateWriter(ref);
        }
        FieldDescriptor parserField = FieldDescriptor.of(fqn(), field, QsonObjectWriter.class);
        return scope.readStaticField(parserField);
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
        for (PropertyMapping getter : properties) {
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
                if (hasCollectionWriter(generator, getter)) {
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
                if (hasCollectionWriter(generator, getter)) {
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
            } else if (getter.type.equals(OffsetDateTime.class) && !generator.hasMappingFor(OffsetDateTime.class)) {

                ResultHandle result = method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeObjectProperty", boolean.class, String.class, Object.class, QsonObjectWriter.class, boolean.class), jsonWriter,
                        method.load(getter.jsonName),
                        method.invokeVirtualMethod(MethodDescriptor.ofMethod(targetType, getter.getter.getName(), getter.type), target),
                        getOffsetDateTimeWriter(getter, method),
                        comma
                );
                if (!forceComma) method.assign(comma, result);
            } else if (getter.type.equals(Date.class) && !generator.hasMappingFor(Date.class)) {

                ResultHandle result = method.invokeInterfaceMethod(MethodDescriptor.ofMethod(JsonWriter.class, "writeObjectProperty", boolean.class, String.class, Object.class, QsonObjectWriter.class, boolean.class), jsonWriter,
                        method.load(getter.jsonName),
                        method.invokeVirtualMethod(MethodDescriptor.ofMethod(targetType, getter.getter.getName(), getter.type), target),
                        getDateUtilWriter(getter, method),
                        comma
                );
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

    private ResultHandle getCollectionWriter(MethodCreator method, PropertyMapping getter) {
        String property = getter.propertyName;
        Type genericType = getter.genericType;
        return getCollectionWriter(getter, method, property, (ParameterizedType) genericType);
    }

    private ResultHandle getCollectionWriter(PropertyMapping ref, MethodCreator method, String property, Type genericType) {
        ParameterizedType pt = (ParameterizedType) genericType;
        Class valueClass = Types.getRawType(pt.getActualTypeArguments()[0]);
        Type valueType = pt.getActualTypeArguments()[0];
        return getWriter(ref, method, property, valueClass, valueType);
    }

    private ResultHandle getWriter(PropertyMapping ref, MethodCreator method, String property, Class valueClass, Type valueType) {
        if (Util.isDateType(valueClass) && !generator.hasMappingFor(valueClass)) {
            if (valueClass.equals(OffsetDateTime.class)) {
                return getOffsetDateTimeWriter(ref, method);
            } else if (valueClass.equals(Date.class)) {
                return getDateUtilWriter(ref, method);
            } else {
                throw new QsonException("Should be unreachable");
            }
        } else if (Util.isUserType(generator, valueClass)) {
            return method.readStaticField(FieldDescriptor.of(fqn(valueClass, valueType), "SERIALIZER", fqn(valueClass, valueType)));
        } else {
            return method.readStaticField(FieldDescriptor.of(fqn(), property + "_n", QsonObjectWriter.class));
        }
    }

    private ResultHandle getMapWriter(MethodCreator method, PropertyMapping getter) {
        String property = getter.propertyName;
        Type genericType = getter.genericType;
        return getMapWriter(getter, method, property, (ParameterizedType) genericType);
    }

    private ResultHandle getMapWriter(PropertyMapping ref, MethodCreator method, String property, Type genericType) {
        ParameterizedType pt = (ParameterizedType) genericType;
        Class valueClass = Types.getRawType(pt.getActualTypeArguments()[1]);
        Type valueType = pt.getActualTypeArguments()[1];
        return getWriter(ref, method, property, valueClass, valueType);
    }

    private static boolean isGeneric(QsonGenerator generator, Class type, Type generic) {
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
            return hasCollectionWriter(generator, type, generic) == false;
        }
        return false;
    }

    private static boolean hasCollectionWriter(QsonGenerator generator, PropertyMapping getter) {
        Class type = getter.type;
        Type genericType = getter.genericType;
        return hasCollectionWriter(generator, type, genericType);
    }

    private static boolean hasCollectionWriter(QsonGenerator generator, Class type, Type genericType) {
        if (!(genericType instanceof ParameterizedType)) return false;
        if (Map.class.isAssignableFrom(type)) {
            ParameterizedType pt = (ParameterizedType) genericType;
            Type valueType = pt.getActualTypeArguments()[1];
            Class valueClass = Types.getRawType(valueType);
            return hasNestedWriter(generator, valueClass, valueType);
        } else if (Collection.class.isAssignableFrom(type)) {
            ParameterizedType pt = (ParameterizedType) genericType;
            Class valueClass = Types.getRawType(pt.getActualTypeArguments()[0]);
            Type valueGenericType = pt.getActualTypeArguments()[0];
            return hasNestedWriter(generator, valueClass, valueGenericType);
        } else {
            return false;
        }
    }

    private static boolean hasNestedWriter(QsonGenerator generator, Class type, Type genericType) {
        if (Util.isDateType(type)) return true;
        if (Util.isUserType(generator, type)) return true;
        if (!Map.class.isAssignableFrom(type)
                && !List.class.isAssignableFrom(type)
                && !Set.class.isAssignableFrom(type)
        ) {
            return false;
        }

        if (!(genericType instanceof ParameterizedType)) return false;

        if (Map.class.isAssignableFrom(type)) {
            ParameterizedType pt = (ParameterizedType) genericType;
            Class valueClass = Types.getRawType(pt.getActualTypeArguments()[1]);
            Type valueGenericType = pt.getActualTypeArguments()[1];
            return hasNestedWriter(generator, valueClass, valueGenericType);
        } else if (Collection.class.isAssignableFrom(type)) {
            ParameterizedType pt = (ParameterizedType) genericType;
            Class valueClass = Types.getRawType(pt.getActualTypeArguments()[0]);
            Type valueGenericType = pt.getActualTypeArguments()[0];
            return hasNestedWriter(generator, valueClass, valueGenericType);
        }
        return false;
    }


    private String fqn() {
        return className;
    }


}
