package io.quarkus.qson.generator;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.qson.QsonDate;
import io.quarkus.qson.QsonException;
import io.quarkus.qson.deserializer.AnySetter;
import io.quarkus.qson.deserializer.DateTimeNumberParser;
import io.quarkus.qson.deserializer.DateUtilNumberParser;
import io.quarkus.qson.deserializer.DateUtilStringParser;
import io.quarkus.qson.deserializer.EnumParser;
import io.quarkus.qson.deserializer.OffsetDateTimeNumberParser;
import io.quarkus.qson.deserializer.OffsetDateTimeStringParser;
import io.quarkus.qson.deserializer.ValueParser;
import io.quarkus.qson.util.Types;
import io.quarkus.qson.deserializer.BaseParser;
import io.quarkus.qson.deserializer.BooleanParser;
import io.quarkus.qson.deserializer.ByteParser;
import io.quarkus.qson.deserializer.DoubleParser;
import io.quarkus.qson.deserializer.FloatParser;
import io.quarkus.qson.deserializer.IntegerParser;
import io.quarkus.qson.deserializer.QsonParser;
import io.quarkus.qson.deserializer.LongParser;
import io.quarkus.qson.deserializer.ParserContext;
import io.quarkus.qson.deserializer.ContextValue;
import io.quarkus.qson.deserializer.GenericParser;
import io.quarkus.qson.deserializer.GenericSetParser;
import io.quarkus.qson.deserializer.ListParser;
import io.quarkus.qson.deserializer.MapParser;
import io.quarkus.qson.deserializer.ObjectParser;
import io.quarkus.qson.deserializer.ParserState;
import io.quarkus.qson.deserializer.SetParser;
import io.quarkus.qson.deserializer.ShortParser;
import io.quarkus.qson.deserializer.StringParser;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

/**
 * Generates a parser class based on passed in type using Gizmo.
 *
 */
public class Deserializer {

    // constructor
    public static final String INIT = "<init>";
    // static initializer
    public static final String CLINIT = "<clinit>";
    public static final String QSON_ANY_SETTER = "_qson_any_setter";
    public static final String DEFAULT_DATE_UTIL = "_defaultDateUtil";
    public static final String DEFAULT_OFFSET_DATE_TIME = "_defaultOffsetDateTime";

    public static class Builder {
        Type type;
        ClassOutput output;
        String className;
        List<PropertyMapping> properties;
        Set<Type> referenced = new HashSet<>();
        Generator generator;

        protected Builder(Generator generator) {
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

        /**
         * Name of the generated parser class
         *
         * @return
         */
        public String className() {
            return className;
        }

        /**
         * Nested types that will need a parser generated for.
         *
         * @return
         */
        public Set<Type> referenced() {
            return referenced;
        }
        public Builder generate() {
            if (type instanceof Class) {
                if (int.class.equals(type)
                        || Integer.class.equals(type)) {
                    className = IntegerParser.class.getName();
                    return this;
                }
                if (short.class.equals(type)
                        || Short.class.equals(type)) {
                    className = ShortParser.class.getName();
                    return this;
                }
                if (long.class.equals(type)
                        || Long.class.equals(type)) {
                    className = LongParser.class.getName();
                    return this;
                }
                if (byte.class.equals(type)
                        || Byte.class.equals(type)) {
                    className = ByteParser.class.getName();
                    return this;
                }
                if (float.class.equals(type)
                        || Float.class.equals(type)
                ) {
                    className = FloatParser.class.getName();
                    return this;
                }
                if (double.class.equals(type)
                        || Double.class.equals(type)
                ) {
                    className = DoubleParser.class.getName();
                    return this;
                }
                if (boolean.class.equals(type)
                        || Boolean.class.equals(type)
                ) {
                    className = BooleanParser.class.getName();
                    return this;
                }
                if (String.class.equals(type)) {
                    className = StringParser.class.getName();
                    return this;
                }
                if (Map.class.equals(type) || List.class.equals(type)) {
                    className = GenericParser.class.getName();
                    return this;
                }
                if (Set.class.equals(type)) {
                    className = GenericSetParser.class.getName();
                    return this;
                }
                Class targetType = (Class)type;
                if (targetType.isEnum()) {
                    Deserializer deserializer = new Deserializer(output, targetType, type);
                    deserializer.generateEnum();
                    className = fqn(targetType);
                    return this;
                }
                // user class parser generation

                ClassMapping mapping = generator.mappingFor(targetType);
                if (mapping != null) {
                    if (mapping.isValue) {
                        Deserializer deserializer = new Deserializer(output, targetType, type);
                        deserializer.mapping = mapping;
                        deserializer.generateValueClass();
                        className = fqn(targetType);
                        return this;
                    } else {
                        properties = mapping.getProperties();
                    }

                }

                // remove any's from property list and sort properties by name
                List<PropertyMapping> tmp = new ArrayList<>();

                Method anySetter = null;
                for (PropertyMapping ref : properties) {
                    if (ref.setter != null) {
                        if (ref.isAny) {
                            anySetter = ref.setter;
                            continue;
                        }
                        tmp.add(ref);
                        Util.addReference(generator, referenced, ref.genericType);
                    }
                }
                // make sure properties are sorted so key matching works.
                Collections.sort(tmp, (ref, t1) -> ref.jsonName.compareTo(t1.jsonName));
                Deserializer deserializer = new Deserializer(output, targetType, type);
                deserializer.anyMethod = anySetter;
                deserializer.mapping = mapping;
                deserializer.generator = generator;
                // set properties list to setters only list
                deserializer.properties = tmp;
                deserializer.generate();
                className = fqn(targetType);
                return this;
            } else if (type instanceof ParameterizedType) {
                Class rawType = Types.getRawType(type);
                if (Map.class.equals(rawType)
                        || List.class.equals(rawType)
                        || Set.class.equals(rawType)) {
                    if (className == null) {
                        className = Util.generatedClassName(type);
                        className += "__Parser";
                    }
                    Deserializer deserializer = new Deserializer(output, className, Types.getRawType(type), type);
                    deserializer.generator = generator;
                    deserializer.generateCollection();
                    Util.addReference(generator, referenced, type);
                    return this;
                }
            }
            throw new QsonException("Deserializer generation unsupported for generic type: " + type.getTypeName());

        }
    }

    ClassCreator creator;

    final Class targetType;
    final Type targetGenericType;
    List<PropertyMapping> properties;
    final ClassOutput classOutput;
    final String className;
    Method anyMethod;
    ClassMapping mapping;
    QsonGenerator generator;

    private static String fqn(Class clz) {
        String prefix = "";
        if (clz.getName().startsWith("java")) {
            prefix = "io.quarkus.qson.";
        }
        return prefix + clz.getName() + "__Parser";
    }

    Deserializer(ClassOutput classOutput, Class targetType, Type targetGenericType) {
        this(classOutput, fqn(targetType), targetType, targetGenericType);
    }

    Deserializer(ClassOutput classOutput, String className, Class targetType, Type targetGenericType) {
        this.targetType = targetType;
        this.targetGenericType = targetGenericType;
        this.classOutput = classOutput;
        this.className = className;
    }

    ResultHandle parseValueClass(ClassMapping mapper, BytecodeCreator scope, _ParserContext ctx) {
        if (mapper.getValueReader() == null) {
            throw new QsonException("There is no value setter for value class: " + mapper.getType().getName());
        }
        if (mapper.getValueReader() instanceof Method) {
            Method setter = (Method)mapper.getValueReader();
            if (Modifier.isStatic(setter.getModifiers())) {
                return scope.invokeStaticMethod(MethodDescriptor.ofMethod(setter), popValue(ctx, scope, setter.getParameterTypes()[0]));
            } else {
                ResultHandle instance = scope.newInstance(MethodDescriptor.ofConstructor(mapper.getType()));
                scope.invokeVirtualMethod(MethodDescriptor.ofMethod(setter), instance, popValue(ctx, scope, setter.getParameterTypes()[0]));
                return instance;
            }
        } else {
            Constructor setter = (Constructor)mapper.getValueReader();
            return scope.newInstance(MethodDescriptor.ofConstructor(mapper.getType(), setter.getParameterTypes()[0]), popValue(ctx, scope, setter.getParameterTypes()[0]));
        }
    }

    ResultHandle callValueClassStartState(_ParserContext ctx, BytecodeCreator scope, ClassMapping mapper) {
        Class type = mapper.getValueReaderType();
        if (type.equals(String.class)) {
            FieldDescriptor parserField = FieldDescriptor.of(ObjectParser.class, "PARSER", ObjectParser.class);
            ResultHandle PARSER = scope.readStaticField(parserField);
            MethodDescriptor descriptor = MethodDescriptor.ofMethod(ObjectParser.class, "startStringValue", boolean.class.getName(), ParserContext.class.getName());
            return scope.invokeVirtualMethod(descriptor, PARSER, ctx.ctx);
        } else if (type.equals(short.class) || type.equals(Short.class)
                || type.equals(byte.class) || type.equals(Byte.class)
                || type.equals(int.class) || type.equals(Integer.class)
                || type.equals(long.class) || type.equals(Long.class)
        ) {
            FieldDescriptor parserField = FieldDescriptor.of(ObjectParser.class, "PARSER", ObjectParser.class);
            ResultHandle PARSER = scope.readStaticField(parserField);
            MethodDescriptor descriptor = MethodDescriptor.ofMethod(ObjectParser.class, "startIntegerValue", boolean.class.getName(), ParserContext.class.getName());
            return scope.invokeVirtualMethod(descriptor, PARSER, ctx.ctx);
        } else if (type.equals(float.class) || type.equals(Float.class)
                || type.equals(double.class) || type.equals(Double.class)
                || type.equals(BigDecimal.class)
        ) {
            FieldDescriptor parserField = FieldDescriptor.of(ObjectParser.class, "PARSER", ObjectParser.class);
            ResultHandle PARSER = scope.readStaticField(parserField);
            MethodDescriptor descriptor = MethodDescriptor.ofMethod(ObjectParser.class, "startNumberValue", boolean.class.getName(), ParserContext.class.getName());
            return scope.invokeVirtualMethod(descriptor, PARSER, ctx.ctx);
        } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            FieldDescriptor parserField = FieldDescriptor.of(ObjectParser.class, "PARSER", ObjectParser.class);
            ResultHandle PARSER = scope.readStaticField(parserField);
            MethodDescriptor descriptor = MethodDescriptor.ofMethod(ObjectParser.class, "startBooleanValue", boolean.class.getName(), ParserContext.class.getName());
            return scope.invokeVirtualMethod(descriptor, PARSER, ctx.ctx);
        } else {
            throw new QsonException("Invalid value type for class: " + mapper.getType().getName());
        }
    }

    void generateValueClass() {
        creator = ClassCreator.builder().classOutput(classOutput)
                .className(className)
                .superClass(ValueParser.class).build();
        FieldCreator PARSER = creator.getFieldCreator("PARSER", fqn()).setModifiers(ACC_STATIC | ACC_PUBLIC);
        MethodCreator staticConstructor = creator.getMethodCreator(CLINIT, void.class);
        staticConstructor.setModifiers(ACC_STATIC);
        ResultHandle instance = staticConstructor.newInstance(MethodDescriptor.ofConstructor(fqn()));
        staticConstructor.writeStaticField(PARSER.getFieldDescriptor(), instance);
        staticConstructor.returnValue(null);

        {
            MethodCreator method = creator.getMethodCreator("value", Object.class, ParserContext.class);
            _ParserContext ctx = new _ParserContext(method.getMethodParam(0));
            method.returnValue(parseValueClass(mapping, method, ctx));
        }


        {
            MethodCreator method = creator.getMethodCreator("start", boolean.class, ParserContext.class);
            _ParserContext ctx = new _ParserContext(method.getMethodParam(0));
            ResultHandle stateIndex = ctx.stateIndex(method);
            BytecodeCreator ifScope = method.createScope();
            ResultHandle passed = callValueClassStartState(ctx, ifScope, mapping);
            ifScope = ifScope.ifZero(passed).trueBranch();
            ctx.pushState(ifScope,
                    ifScope.readInstanceField(FieldDescriptor.of(fqn(), "continueEndValue", ParserState.class), ifScope.getThis()),
                    stateIndex);

            ifScope.returnValue(ifScope.load(false));
            method.invokeVirtualMethod(MethodDescriptor.ofMethod(fqn(), "endValue", void.class, ParserContext.class), method.getThis(), ctx.ctx);
            method.returnValue(method.load(true));
        }
        creator.close();
    }

    void generateCollection() {
        creator = ClassCreator.builder().classOutput(classOutput)
                .className(className)
                .interfaces(QsonParser.class).build();
        MethodCreator staticConstructor = creator.getMethodCreator(CLINIT, void.class);
        staticConstructor.setModifiers(ACC_STATIC);
        collectionField(staticConstructor, null, targetType, targetGenericType, "collection");
        staticConstructor.returnValue(null);
        MethodCreator startState = creator.getMethodCreator("startState", ParserState.class);
        Class collectionParser;
        if (Map.class.isAssignableFrom(targetType)) {
            collectionParser = MapParser.class;
        } else if (List.class.isAssignableFrom(targetType)) {
            collectionParser = ListParser.class;
        } else if (Set.class.isAssignableFrom(targetType)) {
            collectionParser = SetParser.class;
        } else {
            throw new QsonException("Unsupported collection type: " + targetType.getName());
        }
        ResultHandle collection = startState.readStaticField(FieldDescriptor.of(className, "collection", collectionParser));
        ResultHandle result = startState.invokeVirtualMethod(MethodDescriptor.ofMethod(collectionParser, "startState", ParserState.class), collection);
        startState.returnValue(result);
        creator.close();
    }

    void generateEnum() {
        creator = ClassCreator.builder().classOutput(classOutput)
                .className(className)
                .superClass(EnumParser.class).build();

        FieldCreator PARSER = creator.getFieldCreator("PARSER", fqn()).setModifiers(ACC_STATIC | ACC_PUBLIC);


        MethodCreator staticConstructor = creator.getMethodCreator(CLINIT, void.class);
        staticConstructor.setModifiers(ACC_STATIC);
        ResultHandle instance = staticConstructor.newInstance(MethodDescriptor.ofConstructor(fqn()));
        staticConstructor.writeStaticField(PARSER.getFieldDescriptor(), instance);
        staticConstructor.returnValue(null);


        MethodCreator method = creator.getMethodCreator("endStringValue", void.class, ParserContext.class);
        _ParserContext ctx = new _ParserContext(method.getMethodParam(0));
        ResultHandle str = ctx.popToken(method);
        ResultHandle e = method.invokeStaticMethod(MethodDescriptor.ofMethod(targetType, "valueOf", targetType, String.class), str);
        ctx.pushTarget(method, e);
        method.returnValue(null);
        creator.close();
    }

    void generate() {
        creator = ClassCreator.builder().classOutput(classOutput)
                .className(className)
                .superClass(ObjectParser.class).build();

        staticInitializer();
        beginObject();
        key();

        creator.close();
    }

    private void beginObject() {
        MethodCreator method = creator.getMethodCreator("beginObject", void.class, ParserContext.class);
        _ParserContext ctx = new _ParserContext(method.getMethodParam(0));
        ResultHandle instance = method.newInstance(MethodDescriptor.ofConstructor(targetType));
        ctx.pushTarget(method, instance);
        method.returnValue(null);
    }

    private void staticInitializer() {
        FieldCreator PARSER = creator.getFieldCreator("PARSER", fqn()).setModifiers(ACC_STATIC | ACC_PUBLIC);


        MethodCreator staticConstructor = creator.getMethodCreator(CLINIT, void.class);
        staticConstructor.setModifiers(ACC_STATIC);
        ResultHandle instance = staticConstructor.newInstance(MethodDescriptor.ofConstructor(fqn()));
        staticConstructor.writeStaticField(PARSER.getFieldDescriptor(), instance);

        processDateProperties(staticConstructor);
        for (PropertyMapping ref : properties) {
            collectionField(staticConstructor, ref);
            MethodCreator method = propertyEndMethod(ref);
            propertyEndFunction(ref, staticConstructor, method);
        }
        if (anyMethod != null) {
            anySetterFunction(staticConstructor);
        }
        staticConstructor.returnValue(null);
    }

    private ResultHandle allocateDateUtilParser(BytecodeCreator scope, QsonDate.Format format, String pattern) {
        if (pattern != null) {
            return scope.newInstance(MethodDescriptor.ofConstructor(DateUtilStringParser.class, String.class), scope.load(pattern));
        } else if (format == QsonDate.Format.MILLISECONDS) {
            return scope.readStaticField(FieldDescriptor.of(DateUtilNumberParser.class, "MILLIS_UTC", DateTimeNumberParser.class));
        } else if (format == QsonDate.Format.SECONDS) {
            return scope.readStaticField(FieldDescriptor.of(DateUtilNumberParser.class, "SECONDS_UTC", DateTimeNumberParser.class));
        } else if (format == QsonDate.Format.ISO_8601_OFFSET_DATE_TIME) {
            return scope.readStaticField(FieldDescriptor.of(DateUtilStringParser.class, "ISO_8601_OFFSET_DATE_TIME", DateUtilStringParser.class));
        } else if (format == QsonDate.Format.RFC_1123_DATE_TIME) {
            return scope.readStaticField(FieldDescriptor.of(DateUtilStringParser.class, "RFC_1123_DATE_TIME", DateUtilStringParser.class));
        } else {
            throw new QsonException("Unsupported");
        }
    }

    private ResultHandle allocateOffsetDateTimeParser(BytecodeCreator scope, QsonDate.Format format, String pattern) {
        if (pattern != null) {
            return scope.newInstance(MethodDescriptor.ofConstructor(OffsetDateTimeStringParser.class, String.class), scope.load(pattern));
        } else if (format == QsonDate.Format.MILLISECONDS) {
            return scope.readStaticField(FieldDescriptor.of(OffsetDateTimeNumberParser.class, "MILLIS_UTC", DateTimeNumberParser.class));
        } else if (format == QsonDate.Format.SECONDS) {
            return scope.readStaticField(FieldDescriptor.of(OffsetDateTimeNumberParser.class, "SECONDS_UTC", DateTimeNumberParser.class));
        } else if (format == QsonDate.Format.ISO_8601_OFFSET_DATE_TIME) {
            return scope.readStaticField(FieldDescriptor.of(OffsetDateTimeStringParser.class, "ISO_8601_OFFSET_DATE_TIME", OffsetDateTimeStringParser.class));
        } else if (format == QsonDate.Format.RFC_1123_DATE_TIME) {
            return scope.readStaticField(FieldDescriptor.of(OffsetDateTimeStringParser.class, "RFC_1123_DATE_TIME", OffsetDateTimeStringParser.class));
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
                        defaultDateUtil = creator.getFieldCreator(DEFAULT_DATE_UTIL, ValueParser.class).setModifiers(ACC_STATIC | ACC_PRIVATE | ACC_FINAL);
                        staticConstructor.writeStaticField(defaultDateUtil.getFieldDescriptor(), allocateDateUtilParser(staticConstructor, generator.getDateFormat(), null));
                    }
                } else {
                    FieldCreator dateProperty = creator.getFieldCreator(getPropertyDateParser(ref), ValueParser.class).setModifiers(ACC_STATIC | ACC_PRIVATE | ACC_FINAL);
                    staticConstructor.writeStaticField(dateProperty.getFieldDescriptor(), allocateDateUtilParser(staticConstructor, ref.getDateFormat(), ref.getDatePattern()));
                }
            } else if (Types.typeContainsType(ref.genericType, OffsetDateTime.class)) {
                if (ref.dateFormat == null) {
                    if (defaultOffsetDateTime == null && !generator.hasMappingFor(OffsetDateTime.class)) {
                        defaultOffsetDateTime = creator.getFieldCreator(DEFAULT_OFFSET_DATE_TIME, ValueParser.class).setModifiers(ACC_STATIC | ACC_PRIVATE | ACC_FINAL);
                        staticConstructor.writeStaticField(defaultOffsetDateTime.getFieldDescriptor(), allocateOffsetDateTimeParser(staticConstructor, generator.getDateFormat(), null));
                    }
                } else {
                    FieldCreator dateProperty = creator.getFieldCreator(getPropertyDateParser(ref), ValueParser.class).setModifiers(ACC_STATIC | ACC_PRIVATE | ACC_FINAL);
                    staticConstructor.writeStaticField(dateProperty.getFieldDescriptor(), allocateOffsetDateTimeParser(staticConstructor, ref.getDateFormat(), ref.getDatePattern()));
                }
            }
        }
    }

    private String getPropertyDateParser(PropertyMapping ref) {
        return ref.getPropertyName() + "_dateParser";
    }


    private void collectionField(MethodCreator staticConstructor, PropertyMapping ref) {
        Type genericType = ref.genericType;
        Class type = ref.type;
        String property = ref.propertyName;
        collectionField(staticConstructor, ref, type, genericType, property);

    }

    private void collectionField(MethodCreator staticConstructor, PropertyMapping ref, Class type, Type genericType, String property) {
        if (genericType instanceof ParameterizedType) {
            if (Map.class.isAssignableFrom(type)) {
                ParameterizedType pt = (ParameterizedType) genericType;
                Type keyType = pt.getActualTypeArguments()[0];
                Class keyClass = Types.getRawType(keyType);
                Type valueType = pt.getActualTypeArguments()[1];
                Class valueClass = Types.getRawType(valueType);

                if (Map.class.isAssignableFrom(valueClass) && !valueClass.equals(Map.class)) throw new QsonException("Must use java.util.Map for: " + property);
                if (List.class.isAssignableFrom(valueClass) && !valueClass.equals(List.class)) throw new QsonException("Must use java.util.List for: " + property);
                if (Set.class.isAssignableFrom(valueClass) && !valueClass.equals(Set.class)) throw new QsonException("Must use java.util.Set for: " + property);

                if (valueClass.equals(Map.class) || valueClass.equals(List.class) || valueClass.equals(Set.class)) {
                    collectionField(staticConstructor, ref, valueClass, valueType, property + "_n");
                }

                ResultHandle keyContextValue = contextValue(keyClass, keyType, staticConstructor);
                ResultHandle valueContextValue = contextValue(valueClass, valueType, staticConstructor);
                ResultHandle valueState = collectionValueState(ref, valueClass, valueType, staticConstructor, property);
                ResultHandle continueValueState = continueValueState(ref, valueClass, valueType, staticConstructor, property);
                FieldCreator mapParser = creator.getFieldCreator(property, MapParser.class).setModifiers(ACC_STATIC | ACC_PRIVATE | ACC_FINAL);
                ResultHandle instance = staticConstructor.newInstance(MethodDescriptor.ofConstructor(MapParser.class, ContextValue.class, ContextValue.class, ParserState.class, ParserState.class),
                        keyContextValue, valueContextValue, valueState, continueValueState);
                staticConstructor.writeStaticField(mapParser.getFieldDescriptor(), instance);
            } else if (List.class.isAssignableFrom(type)) {
                ParameterizedType pt = (ParameterizedType) genericType;
                Type valueType = pt.getActualTypeArguments()[0];
                Class valueClass = Types.getRawType(valueType);

                if (Map.class.isAssignableFrom(valueClass) && !valueClass.equals(Map.class)) throw new QsonException("Must use java.util.Map for property: " + property);
                if (List.class.isAssignableFrom(valueClass) && !valueClass.equals(List.class)) throw new QsonException("Must use java.util.List for property: " + property);
                if (Set.class.isAssignableFrom(valueClass) && !valueClass.equals(Set.class)) throw new QsonException("Must use java.util.Set for property: " + property);

                if (valueClass.equals(Map.class) || valueClass.equals(List.class) || valueClass.equals(Set.class)) {
                    collectionField(staticConstructor, ref, valueClass, valueType, property + "_n");
                }


                ResultHandle valueContextValue = contextValue(valueClass, valueType, staticConstructor);
                ResultHandle valueState = collectionValueState(ref, valueClass, valueType, staticConstructor, property);
                FieldCreator collectionParser = creator.getFieldCreator(property, ListParser.class).setModifiers(ACC_STATIC | ACC_PRIVATE | ACC_FINAL);
                ResultHandle instance = staticConstructor.newInstance(MethodDescriptor.ofConstructor(ListParser.class, ContextValue.class, ParserState.class),
                        valueContextValue, valueState);
                staticConstructor.writeStaticField(collectionParser.getFieldDescriptor(), instance);

            } else if (Set.class.isAssignableFrom(type)) {
                ParameterizedType pt = (ParameterizedType) genericType;
                Type valueType = pt.getActualTypeArguments()[0];
                Class valueClass = Types.getRawType(valueType);

                if (Map.class.isAssignableFrom(valueClass) && !valueClass.equals(Map.class)) throw new QsonException("Must use java.util.Map for property: " + property);
                if (List.class.isAssignableFrom(valueClass) && !valueClass.equals(List.class)) throw new QsonException("Must use java.util.List for property: " + property);
                if (Set.class.isAssignableFrom(valueClass) && !valueClass.equals(Set.class)) throw new QsonException("Must use java.util.Set for property: " + property);

                if (valueClass.equals(Map.class) || valueClass.equals(List.class) || valueClass.equals(Set.class)) {
                    collectionField(staticConstructor, ref, valueClass, valueType, property + "_n");
                }

                ResultHandle valueContextValue = contextValue(valueClass, valueType, staticConstructor);
                ResultHandle valueState = collectionValueState(ref, valueClass, valueType, staticConstructor, property);
                FieldCreator collectionParser = creator.getFieldCreator(property, SetParser.class).setModifiers(ACC_STATIC | ACC_PRIVATE | ACC_FINAL);
                ResultHandle instance = staticConstructor.newInstance(MethodDescriptor.ofConstructor(SetParser.class, ContextValue.class, ParserState.class),
                        valueContextValue, valueState);
                staticConstructor.writeStaticField(collectionParser.getFieldDescriptor(), instance);

            } else {
                // ignore we don't need a special parser
            }
        }
    }

    private ResultHandle contextValue(Class type, Type genericType, BytecodeCreator scope) {
        if (type.equals(String.class)) {
            return scope.readStaticField(FieldDescriptor.of(ContextValue.class, "STRING_VALUE", ContextValue.class));
        } else if (type.equals(short.class) || type.equals(Short.class)) {
            return scope.readStaticField(FieldDescriptor.of(ContextValue.class, "SHORT_VALUE", ContextValue.class));
        } else if (type.equals(byte.class) || type.equals(Byte.class)) {
            return scope.readStaticField(FieldDescriptor.of(ContextValue.class, "BYTE_VALUE", ContextValue.class));
        } else if (type.equals(int.class) || type.equals(Integer.class)) {
            return scope.readStaticField(FieldDescriptor.of(ContextValue.class, "INT_VALUE", ContextValue.class));
        } else if (type.equals(long.class) || type.equals(Long.class)) {
            return scope.readStaticField(FieldDescriptor.of(ContextValue.class, "LONG_VALUE", ContextValue.class));
        } else if (type.equals(float.class) || type.equals(Float.class)) {
            return scope.readStaticField(FieldDescriptor.of(ContextValue.class, "FLOAT_VALUE", ContextValue.class));
        } else if (type.equals(double.class) || type.equals(Double.class)) {
            return scope.readStaticField(FieldDescriptor.of(ContextValue.class, "DOUBLE_VALUE", ContextValue.class));
        } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            return scope.readStaticField(FieldDescriptor.of(ContextValue.class, "BOOLEAN_VALUE", ContextValue.class));
        } else if (type.equals(char.class) || type.equals(Character.class)) {
            return scope.readStaticField(FieldDescriptor.of(ContextValue.class, "CHAR_VALUE", ContextValue.class));
        } else if (type.equals(BigDecimal.class)) {
            return scope.readStaticField(FieldDescriptor.of(ContextValue.class, "BIGDECIMAL_VALUE", ContextValue.class));
        } else {
            return scope.readStaticField(FieldDescriptor.of(ContextValue.class, "OBJECT_VALUE", ContextValue.class));
        }
    }

    private ResultHandle collectionValueState(PropertyMapping ref, Class type, Type genericType, BytecodeCreator scope, String property) {
        if (type.equals(String.class)
                || type.equals(char.class) || type.equals(Character.class)
        ) {
            FieldDescriptor parserField = FieldDescriptor.of(ObjectParser.class, "PARSER", ObjectParser.class);
            ResultHandle PARSER = scope.readStaticField(parserField);
            return scope.readInstanceField(FieldDescriptor.of(ObjectParser.class, "startStringValue", ParserState.class), PARSER);
        } else if (type.equals(short.class) || type.equals(Short.class)
                || type.equals(byte.class) || type.equals(Byte.class)
                || type.equals(int.class) || type.equals(Integer.class)
                || type.equals(long.class) || type.equals(Long.class)
        ) {
            FieldDescriptor parserField = FieldDescriptor.of(ObjectParser.class, "PARSER", ObjectParser.class);
            ResultHandle PARSER = scope.readStaticField(parserField);
            return scope.readInstanceField(FieldDescriptor.of(ObjectParser.class, "startIntegerValue", ParserState.class), PARSER);
        } else if (type.equals(float.class) || type.equals(Float.class)
                || type.equals(double.class) || type.equals(Double.class)
                || type.equals(BigDecimal.class)
        ) {
            FieldDescriptor parserField = FieldDescriptor.of(ObjectParser.class, "PARSER", ObjectParser.class);
            ResultHandle PARSER = scope.readStaticField(parserField);
            return scope.readInstanceField(FieldDescriptor.of(ObjectParser.class, "startNumberValue", ParserState.class), PARSER);
        } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            FieldDescriptor parserField = FieldDescriptor.of(ObjectParser.class, "PARSER", ObjectParser.class);
            ResultHandle PARSER = scope.readStaticField(parserField);
            return scope.readInstanceField(FieldDescriptor.of(ObjectParser.class, "startBooleanValue", ParserState.class), PARSER);
        } else if (type.equals(Map.class)) {
            if (genericType instanceof ParameterizedType) {
                FieldDescriptor parserField = FieldDescriptor.of(fqn(), property + "_n", MapParser.class);
                ResultHandle PARSER = scope.readStaticField(parserField);
                return scope.readInstanceField(FieldDescriptor.of(MapParser.class, "start", ParserState.class), PARSER);
            } else {
                FieldDescriptor parserField = FieldDescriptor.of(GenericParser.class, "PARSER", GenericParser.class);
                ResultHandle PARSER = scope.readStaticField(parserField);
                return scope.readInstanceField(FieldDescriptor.of(GenericParser.class, "startObject", ParserState.class), PARSER);
            }
        } else if (type.equals(List.class)) {
            if (genericType instanceof ParameterizedType) {
                FieldDescriptor parserField = FieldDescriptor.of(fqn(), property + "_n", ListParser.class);
                ResultHandle PARSER = scope.readStaticField(parserField);
                return scope.readInstanceField(FieldDescriptor.of(ListParser.class, "start", ParserState.class), PARSER);
            } else {
                FieldDescriptor parserField = FieldDescriptor.of(GenericParser.class, "PARSER", GenericParser.class);
                ResultHandle PARSER = scope.readStaticField(parserField);
                return scope.readInstanceField(FieldDescriptor.of(GenericParser.class, "startList", ParserState.class), PARSER);
            }
        } else if (type.equals(Set.class)) {
            if (genericType instanceof ParameterizedType) {
                FieldDescriptor parserField = FieldDescriptor.of(fqn(), property + "_n", SetParser.class);
                ResultHandle PARSER = scope.readStaticField(parserField);
                return scope.readInstanceField(FieldDescriptor.of(SetParser.class, "start", ParserState.class), PARSER);
            } else {
                FieldDescriptor parserField = FieldDescriptor.of(GenericParser.class, "PARSER", GenericSetParser.class);
                ResultHandle PARSER = scope.readStaticField(parserField);
                return scope.readInstanceField(FieldDescriptor.of(GenericSetParser.class, "startList", ParserState.class), PARSER);
            }
        } else if (type.equals(OffsetDateTime.class) && !generator.hasMappingFor(type)) {
            ResultHandle PARSER = getOffsetDateTimeParser(ref, scope);
            return scope.readInstanceField(FieldDescriptor.of(ValueParser.class, "start", ParserState.class), PARSER);
        } else if (type.equals(Date.class) && !generator.hasMappingFor(type)) {
            ResultHandle PARSER = getDateUtilParser(ref, scope);
            return scope.readInstanceField(FieldDescriptor.of(ValueParser.class, "start", ParserState.class), PARSER);
        } else {
            FieldDescriptor parserField = FieldDescriptor.of(fqn(type), "PARSER", fqn(type));
            ResultHandle PARSER = scope.readStaticField(parserField);
            ClassMapping mapping = generator.mappingFor(type);
            if (mapping.isValue()) {
                return scope.readInstanceField(FieldDescriptor.of(ValueParser.class, "start", ParserState.class), PARSER);
            } else {

                return scope.readInstanceField(FieldDescriptor.of(ObjectParser.class, "start", ParserState.class), PARSER);
            }
         }
    }

    private ResultHandle getDateUtilParser(PropertyMapping ref, BytecodeCreator scope) {
        String field = DEFAULT_DATE_UTIL;
        if (ref != null && ref.getDateFormat() != null) {
            field = getPropertyDateParser(ref);
        }
        FieldDescriptor parserField = FieldDescriptor.of(fqn(), field, ValueParser.class);
        return scope.readStaticField(parserField);
    }

    private ResultHandle continueValueState(PropertyMapping ref, Class type, Type genericType, BytecodeCreator scope, String property) {
        if (type.equals(String.class)
                || type.equals(char.class) || type.equals(Character.class)
        ) {
            FieldDescriptor parserField = FieldDescriptor.of(ObjectParser.class, "PARSER", ObjectParser.class);
            ResultHandle PARSER = scope.readStaticField(parserField);
            return scope.readInstanceField(FieldDescriptor.of(ObjectParser.class, "continueStartStringValue", ParserState.class), PARSER);
        } else if (type.equals(short.class) || type.equals(Short.class)
                || type.equals(byte.class) || type.equals(Byte.class)
                || type.equals(int.class) || type.equals(Integer.class)
                || type.equals(long.class) || type.equals(Long.class)
        ) {
            FieldDescriptor parserField = FieldDescriptor.of(ObjectParser.class, "PARSER", ObjectParser.class);
            ResultHandle PARSER = scope.readStaticField(parserField);
            return scope.readInstanceField(FieldDescriptor.of(ObjectParser.class, "continueStartIntegerValue", ParserState.class), PARSER);
        } else if (type.equals(float.class) || type.equals(Float.class)
                || type.equals(double.class) || type.equals(Double.class)
                || type.equals(BigDecimal.class)
        ) {
            FieldDescriptor parserField = FieldDescriptor.of(ObjectParser.class, "PARSER", ObjectParser.class);
            ResultHandle PARSER = scope.readStaticField(parserField);
            return scope.readInstanceField(FieldDescriptor.of(ObjectParser.class, "continueStartNumberValue", ParserState.class), PARSER);
        } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            FieldDescriptor parserField = FieldDescriptor.of(ObjectParser.class, "PARSER", ObjectParser.class);
            ResultHandle PARSER = scope.readStaticField(parserField);
            return scope.readInstanceField(FieldDescriptor.of(ObjectParser.class, "continueStartBooleanValue", ParserState.class), PARSER);
        } else if (type.equals(Map.class)) {
            if (genericType instanceof ParameterizedType) {
                FieldDescriptor parserField = FieldDescriptor.of(fqn(), property + "_n", MapParser.class);
                ResultHandle PARSER = scope.readStaticField(parserField);
                return scope.readInstanceField(FieldDescriptor.of(MapParser.class, "continueStart", ParserState.class), PARSER);
            } else {
                FieldDescriptor parserField = FieldDescriptor.of(GenericParser.class, "PARSER", GenericParser.class);
                ResultHandle PARSER = scope.readStaticField(parserField);
                return scope.readInstanceField(FieldDescriptor.of(GenericParser.class, "continueStartObject", ParserState.class), PARSER);
            }
        } else if (type.equals(List.class)) {
            if (genericType instanceof ParameterizedType) {
                FieldDescriptor parserField = FieldDescriptor.of(fqn(), property + "_n", ListParser.class);
                ResultHandle PARSER = scope.readStaticField(parserField);
                return scope.readInstanceField(FieldDescriptor.of(ListParser.class, "continueStart", ParserState.class), PARSER);
            } else {
                FieldDescriptor parserField = FieldDescriptor.of(GenericParser.class, "PARSER", GenericParser.class);
                ResultHandle PARSER = scope.readStaticField(parserField);
                return scope.readInstanceField(FieldDescriptor.of(GenericParser.class, "continueStartList", ParserState.class), PARSER);
            }
        } else if (type.equals(Set.class)) {
            if (genericType instanceof ParameterizedType) {
                FieldDescriptor parserField = FieldDescriptor.of(fqn(), property + "_n", SetParser.class);
                ResultHandle PARSER = scope.readStaticField(parserField);
                return scope.readInstanceField(FieldDescriptor.of(SetParser.class, "continueStart", ParserState.class), PARSER);
            } else {
                FieldDescriptor parserField = FieldDescriptor.of(GenericParser.class, "PARSER", GenericSetParser.class);
                ResultHandle PARSER = scope.readStaticField(parserField);
                return scope.readInstanceField(FieldDescriptor.of(GenericSetParser.class, "continueStartList", ParserState.class), PARSER);
            }
        } else if (type.equals(OffsetDateTime.class) && !generator.hasMappingFor(type)) {
            ResultHandle PARSER = getOffsetDateTimeParser(ref, scope);
            return scope.readInstanceField(FieldDescriptor.of(ValueParser.class, "continueStart", ParserState.class), PARSER);
        } else if (type.equals(Date.class) && !generator.hasMappingFor(type)) {
            ResultHandle PARSER = getDateUtilParser(ref, scope);
            return scope.readInstanceField(FieldDescriptor.of(ValueParser.class, "continueStart", ParserState.class), PARSER);
        } else {
            FieldDescriptor parserField = FieldDescriptor.of(fqn(type), "PARSER", fqn(type));
            ResultHandle PARSER = scope.readStaticField(parserField);
            ClassMapping mapping = generator.mappingFor(type);
            if (mapping.isValue()) {
                return scope.readInstanceField(FieldDescriptor.of(ValueParser.class, "continueStart", ParserState.class), PARSER);
            } else {

                return scope.readInstanceField(FieldDescriptor.of(ObjectParser.class, "continueStart", ParserState.class), PARSER);
            }
        }
    }

    private ResultHandle getOffsetDateTimeParser(PropertyMapping ref, BytecodeCreator scope) {
        String field = DEFAULT_OFFSET_DATE_TIME;
        if (ref != null && ref.getDateFormat() != null) {
            field = getPropertyDateParser(ref);
        }
        FieldDescriptor parserField = FieldDescriptor.of(fqn(), field, ValueParser.class);
        return scope.readStaticField(parserField);
    }

    private void anySetterFunction(MethodCreator staticConstructor) {
        String endName = QSON_ANY_SETTER;
        FieldCreator endField = creator.getFieldCreator(endName, AnySetter.class).setModifiers(ACC_STATIC | ACC_PRIVATE);
        FunctionCreator endFunction = staticConstructor.createFunction(AnySetter.class);
        BytecodeCreator ebc = endFunction.getBytecode();
        AssignableResultHandle target = ebc.createVariable(targetType);
        ebc.assign(target, ebc.getMethodParam(0));
        ebc.invokeVirtualMethod(MethodDescriptor.ofMethod(anyMethod), target, ebc.getMethodParam(1), ebc.getMethodParam(2));
        ebc.returnValue(null);
        staticConstructor.writeStaticField(endField.getFieldDescriptor(), endFunction.getInstance());
    }

    private void propertyEndFunction(PropertyMapping setter, MethodCreator staticConstructor, MethodCreator method) {
        String endName = endProperty(setter);
        FieldCreator endField = creator.getFieldCreator(endName, ParserState.class).setModifiers(ACC_STATIC | ACC_PRIVATE);
        FunctionCreator endFunction = staticConstructor.createFunction(ParserState.class);
        BytecodeCreator ebc = endFunction.getBytecode();
        _ParserContext ctx = new _ParserContext(ebc.getMethodParam(0));
        ctx.popState(ebc);
        ebc.invokeStaticMethod(method.getMethodDescriptor(), ctx.ctx);
        ebc.returnValue(ebc.load(true));
        staticConstructor.writeStaticField(endField.getFieldDescriptor(), endFunction.getInstance());
    }

    private MethodCreator propertyEndMethod(PropertyMapping setter) {
        String endName = endProperty(setter);
        MethodCreator method = creator.getMethodCreator(endName, void.class, ParserContext.class);
        method.setModifiers(ACC_STATIC | ACC_FINAL | ACC_PUBLIC);
        _ParserContext ctx = new _ParserContext(method.getMethodParam(0));
        ResultHandle popSetter = popSetterValue(ctx, setter, method);
        AssignableResultHandle target = method.createVariable(targetType);
        method.assign(target, ctx.target(method));
        MethodDescriptor set = MethodDescriptor.ofMethod(setter.setter);
        method.invokeVirtualMethod(set, target, popSetter);
        method.returnValue(null);
        return method;
    }

    private ResultHandle popSetterValue(_ParserContext ctx, PropertyMapping setter, BytecodeCreator scope) {
        Class type = setter.type;
        return popValue(ctx, scope, type);
    }

    private ResultHandle popValue(_ParserContext ctx, BytecodeCreator scope, Class type) {
        if (type.equals(String.class)) {
            return ctx.popToken(scope);
        } else if (type.equals(short.class)) {
            return ctx.popShortToken(scope);
        } else if (type.equals(Short.class)) {
            return ctx.popShortObjectToken(scope);
        } else if (type.equals(byte.class)) {
            return ctx.popByteToken(scope);
        } else if (type.equals(Byte.class)) {
            return ctx.popByteObjectToken(scope);
        } else if (type.equals(int.class)) {
            return ctx.popIntToken(scope);
        } else if (type.equals(Integer.class)) {
            return ctx.popIntObjectToken(scope);
        } else if (type.equals(long.class)) {
            return ctx.popLongToken(scope);
        } else if (type.equals(Long.class)) {
            return ctx.popLongObjectToken(scope);
        } else if (type.equals(float.class)) {
            return ctx.popFloatToken(scope);
        } else if (type.equals(Float.class)) {
            return ctx.popFloatObjectToken(scope);
        } else if (type.equals(double.class)) {
            return ctx.popDoubleToken(scope);
        } else if (type.equals(Double.class)) {
            return ctx.popDoubleObjectToken(scope);
        } else if (type.equals(boolean.class)) {
            return ctx.popBooleanToken(scope);
        } else if (type.equals(Boolean.class)) {
            return ctx.popBooleanObjectToken(scope);
        } else {
            return ctx.popTarget(scope);
        }
    }

    private String endProperty(PropertyMapping setter) {
        return setter.propertyName + "End";
    }

    private void key() {
        MethodCreator method = creator.getMethodCreator("key", boolean.class, ParserContext.class);
        _ParserContext ctx = new _ParserContext(method.getMethodParam(0));

        BytecodeCreator scope = method.createScope();
        BytecodeCreator ifTrue = scope.ifIntegerEqual(ctx.skipToQuote(scope), scope.load((int)0)).trueBranch();
        ctx.pushState(ifTrue, ifTrue.readInstanceField(FieldDescriptor.of(fqn(), "continueKey", ParserState.class), ifTrue.getThis()));
        ifTrue.returnValue(ifTrue.load(false));
        ctx.endToken(method);
        ResultHandle stateIndex = ctx.stateIndex(method);

        chooseField(method, ctx, stateIndex, properties, 0);

        if (anyMethod == null) {
            ResultHandle result = method.invokeVirtualMethod(MethodDescriptor.ofMethod(BaseParser.class, "skipValue", boolean.class, ParserContext.class),
                    method.readStaticField(FieldDescriptor.of(BaseParser.class, "PARSER", BaseParser.class)), ctx.ctx);
            method.returnValue(result);
        } else {
            ResultHandle result = ctx.handleAny(method, method.readStaticField(FieldDescriptor.of(fqn(), QSON_ANY_SETTER, AnySetter.class)));
            method.returnValue(result);
        }
    }

    private void chooseField(BytecodeCreator scope, _ParserContext ctx, ResultHandle stateIndex, List<PropertyMapping> setters, int offset) {
        if (setters.size() == 1) {
            PropertyMapping setter = setters.get(0);
            compareToken(scope, ctx, stateIndex, offset, setter);
            return;
        }
        ResultHandle c = ctx.tokenCharAt(scope, offset);
        for (int i = 0; i < setters.size(); i++) {
            PropertyMapping setter = setters.get(i);
            if (offset >= setter.jsonName.length()) {
                compareToken(scope, ctx, stateIndex, offset, setter);
                continue;
            }
            char ch = setter.jsonName.charAt(offset);
            List<PropertyMapping> sameChars = new ArrayList<>();
            sameChars.add(setter);
            BytecodeCreator ifScope = scope.createScope();
            BranchResult branchResult = ifScope.ifIntegerEqual(c, ifScope.load(ch));
            for (i = i + 1; i < setters.size(); i++) {
                PropertyMapping next = setters.get(i);
                if (offset < next.jsonName.length() && next.jsonName.charAt(offset) == ch) {
                    sameChars.add(next);
                } else {
                    i--;
                    break;
                }
            }
            chooseField(branchResult.trueBranch(), ctx, stateIndex, sameChars, offset + 1);
            scope = branchResult.falseBranch();
        }

    }

    private void compareToken(BytecodeCreator scope, _ParserContext ctx, ResultHandle stateIndex, int offset, PropertyMapping setter) {
        BytecodeCreator ifScope = scope.createScope();
        ResultHandle check = ctx.compareToken(ifScope, ifScope.load(offset), ifScope.load(setter.jsonName.substring(offset)));
        matchHandler(ctx, stateIndex, setter, ifScope.ifNonZero(check).trueBranch());
    }

    private void matchHandler(_ParserContext ctx, ResultHandle stateIndex, PropertyMapping setter, BytecodeCreator scope) {
        BytecodeCreator ifScope = scope.createScope();
        MethodDescriptor valueSeparator = valueSeparator();
        ResultHandle passed = ifScope.invokeVirtualMethod(valueSeparator, scope.getThis(), ctx.ctx);
        ifScope = ifScope.ifZero(passed).trueBranch();
        ctx.pushState(ifScope,
                continueState(setter, ifScope),
                stateIndex);
        ctx.pushState(ifScope,
                ifScope.readStaticField(FieldDescriptor.of(fqn(), endProperty(setter), ParserState.class)), stateIndex);
        ifScope.returnValue(ifScope.load(false));

        ifScope = scope.createScope();
        passed = callStartState(ctx, setter, ifScope);
        ifScope = ifScope.ifZero(passed).trueBranch();
        ctx.pushState(ifScope,
                ifScope.readStaticField(FieldDescriptor.of(fqn(), endProperty(setter), ParserState.class)),
                        stateIndex);
        ifScope.returnValue(ifScope.load(false));

        scope.invokeStaticMethod(MethodDescriptor.ofMethod(fqn(), endProperty(setter), void.class, ParserContext.class), ctx.ctx);
        scope.returnValue(scope.load(true));
    }

    private ResultHandle continueState(PropertyMapping setter, BytecodeCreator scope) {
        Class type = setter.type;
        Type genericType = setter.genericType;
        if (type.equals(String.class)
                || type.equals(char.class) || type.equals(Character.class)
        ) {
            FieldDescriptor parserField = FieldDescriptor.of(ObjectParser.class, "PARSER", ObjectParser.class);
            ResultHandle PARSER = scope.readStaticField(parserField);
            return scope.readInstanceField(FieldDescriptor.of(ObjectParser.class, "continueStartStringValue", ParserState.class), PARSER);
        } else if (type.equals(short.class) || type.equals(Short.class)
                || type.equals(byte.class) || type.equals(Byte.class)
                || type.equals(int.class) || type.equals(Integer.class)
                || type.equals(long.class) || type.equals(Long.class)
        ) {
            FieldDescriptor parserField = FieldDescriptor.of(ObjectParser.class, "PARSER", ObjectParser.class);
            ResultHandle PARSER = scope.readStaticField(parserField);
            return scope.readInstanceField(FieldDescriptor.of(ObjectParser.class, "continueStartIntegerValue", ParserState.class), PARSER);
        } else if (type.equals(float.class) || type.equals(Float.class)
                || type.equals(double.class) || type.equals(Double.class)
                || type.equals(BigDecimal.class)
        ) {
            FieldDescriptor parserField = FieldDescriptor.of(ObjectParser.class, "PARSER", ObjectParser.class);
            ResultHandle PARSER = scope.readStaticField(parserField);
            return scope.readInstanceField(FieldDescriptor.of(ObjectParser.class, "continueStartNumberValue", ParserState.class), PARSER);
        } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            FieldDescriptor parserField = FieldDescriptor.of(ObjectParser.class, "PARSER", ObjectParser.class);
            ResultHandle PARSER = scope.readStaticField(parserField);
            return scope.readInstanceField(FieldDescriptor.of(ObjectParser.class, "continueStartBooleanValue", ParserState.class), PARSER);
        } else if (List.class.isAssignableFrom(setter.type)) {
            if (!List.class.equals(setter.type)) throw new QsonException("Cannot use concrete list.  Must use java.util.List for property: " + setter.propertyName);
            if (setter.genericType instanceof ParameterizedType) {
                // continue is on static field
                FieldDescriptor mapFieldDesc = FieldDescriptor.of(fqn(), setter.propertyName, ListParser.class);
                ResultHandle mapField = scope.readStaticField(mapFieldDesc);
                return scope.readInstanceField(FieldDescriptor.of(ListParser.class, "continueStart", ParserState.class), mapField);
            } else {
                FieldDescriptor parserField = FieldDescriptor.of(GenericParser.class, "PARSER", GenericParser.class);
                ResultHandle PARSER = scope.readStaticField(parserField);
                return scope.readInstanceField(FieldDescriptor.of(GenericParser.class, "continueStartObject", ParserState.class), PARSER);
            }
        } else if (Set.class.isAssignableFrom(setter.type)) {
            if (!Set.class.equals(setter.type)) throw new QsonException("Cannot use concrete set.  Must use java.util.Set for property: " + setter.propertyName);
            if (setter.genericType instanceof ParameterizedType) {
                // continue is on static field
                FieldDescriptor mapFieldDesc = FieldDescriptor.of(fqn(), setter.propertyName, SetParser.class);
                ResultHandle mapField = scope.readStaticField(mapFieldDesc);
                return scope.readInstanceField(FieldDescriptor.of(SetParser.class, "continueStart", ParserState.class), mapField);
            } else {
                FieldDescriptor parserField = FieldDescriptor.of(GenericSetParser.class, "PARSER", GenericParser.class);
                ResultHandle PARSER = scope.readStaticField(parserField);
                return scope.readInstanceField(FieldDescriptor.of(GenericSetParser.class, "continueStartObject", ParserState.class), PARSER);
            }
        } else if (Map.class.isAssignableFrom(setter.type)) {
            if (!Map.class.equals(setter.type)) throw new QsonException("Cannot use concrete map.  Must use java.util.Map for property: " + setter.propertyName);
            if (setter.genericType instanceof ParameterizedType) {
                FieldDescriptor mapFieldDesc = FieldDescriptor.of(fqn(), setter.propertyName, MapParser.class);
                ResultHandle mapField = scope.readStaticField(mapFieldDesc);
                return scope.readInstanceField(FieldDescriptor.of(MapParser.class, "continueStart", ParserState.class), mapField);
            } else {
                FieldDescriptor parserField = FieldDescriptor.of(GenericParser.class, "PARSER", GenericParser.class);
                ResultHandle PARSER = scope.readStaticField(parserField);
                return scope.readInstanceField(FieldDescriptor.of(GenericParser.class, "continueStartObject", ParserState.class), PARSER);
            }
        } else if (setter.type.equals(Object.class)) {
            FieldDescriptor parserField = FieldDescriptor.of(GenericParser.class, "PARSER", GenericParser.class);
            ResultHandle PARSER = scope.readStaticField(parserField);
            return scope.readInstanceField(FieldDescriptor.of(GenericParser.class, "continueStart", ParserState.class), PARSER);
        } else if (type.equals(OffsetDateTime.class) && !generator.hasMappingFor(type)) {
            ResultHandle PARSER = getOffsetDateTimeParser(setter, scope);
            return scope.readInstanceField(FieldDescriptor.of(ValueParser.class, "continueStart", ParserState.class), PARSER);
        } else if (type.equals(Date.class) && !generator.hasMappingFor(type)) {
            ResultHandle PARSER = getDateUtilParser(setter, scope);
            return scope.readInstanceField(FieldDescriptor.of(ValueParser.class, "continueStart", ParserState.class), PARSER);
        } else {
            FieldDescriptor parserField = FieldDescriptor.of(fqn(type), "PARSER", fqn(type));
            ResultHandle PARSER = scope.readStaticField(parserField);
            ClassMapping mapping = generator.mappingFor(type);
            if (mapping.isValue()) {
                return scope.readInstanceField(FieldDescriptor.of(ValueParser.class, "continueStart", ParserState.class), PARSER);
            } else {

                return scope.readInstanceField(FieldDescriptor.of(ObjectParser.class, "continueStart", ParserState.class), PARSER);
            }
        }
    }

    private ResultHandle callStartState(_ParserContext ctx, PropertyMapping setter, BytecodeCreator scope) {
        Class type = setter.type;
        Type genericType = setter.genericType;
        if (type.equals(String.class)
                || type.equals(char.class) || type.equals(Character.class)) {
            MethodDescriptor descriptor = MethodDescriptor.ofMethod(fqn(), "startStringValue", boolean.class.getName(), ParserContext.class.getName());
            return scope.invokeVirtualMethod(descriptor, scope.getThis(), ctx.ctx);
        } else if (type.equals(short.class) || type.equals(Short.class)
                || type.equals(byte.class) || type.equals(Byte.class)
                || type.equals(int.class) || type.equals(Integer.class)
                || type.equals(long.class) || type.equals(Long.class)
        ) {
            MethodDescriptor descriptor = MethodDescriptor.ofMethod(fqn(), "startIntegerValue", boolean.class.getName(), ParserContext.class.getName());
            return scope.invokeVirtualMethod(descriptor, scope.getThis(), ctx.ctx);
        } else if (type.equals(float.class) || type.equals(Float.class)
                || type.equals(double.class) || type.equals(Double.class)
                || type.equals(BigDecimal.class)
        ) {
            MethodDescriptor descriptor = MethodDescriptor.ofMethod(fqn(), "startNumberValue", boolean.class.getName(), ParserContext.class.getName());
            return scope.invokeVirtualMethod(descriptor, scope.getThis(), ctx.ctx);
        } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            MethodDescriptor descriptor = MethodDescriptor.ofMethod(fqn(), "startBooleanValue", boolean.class.getName(), ParserContext.class.getName());
            return scope.invokeVirtualMethod(descriptor, scope.getThis(), ctx.ctx);
        } else if (List.class.isAssignableFrom(setter.type)) {
            if (!List.class.equals(setter.type)) throw new QsonException("Cannot use concrete list.  Must use java.util.List for property: " + setter.propertyName);
            if (setter.genericType instanceof ParameterizedType) {
                // invoke static field for property
                MethodDescriptor descriptor = MethodDescriptor.ofMethod(ListParser.class, "start", boolean.class, ParserContext.class);
                return scope.invokeVirtualMethod(descriptor,
                        scope.readStaticField(FieldDescriptor.of(fqn(), setter.propertyName, ListParser.class)),
                        ctx.ctx);
            } else {
                FieldDescriptor parserField = FieldDescriptor.of(GenericParser.class, "PARSER", GenericParser.class);
                ResultHandle PARSER = scope.readStaticField(parserField);
                MethodDescriptor descriptor = MethodDescriptor.ofMethod(GenericParser.class, "startList", boolean.class, ParserContext.class);
                return scope.invokeVirtualMethod(descriptor, PARSER, ctx.ctx);
            }
        } else if (Set.class.isAssignableFrom(setter.type)) {
            if (!Set.class.equals(setter.type)) throw new QsonException("Cannot use concrete set.  Must use java.util.Set for property: " + setter.propertyName);
            if (setter.genericType instanceof ParameterizedType) {
                // invoke static field for property
                MethodDescriptor descriptor = MethodDescriptor.ofMethod(SetParser.class, "start", boolean.class, ParserContext.class);
                return scope.invokeVirtualMethod(descriptor,
                        scope.readStaticField(FieldDescriptor.of(fqn(), setter.propertyName, SetParser.class)),
                        ctx.ctx);
            } else {
                FieldDescriptor parserField = FieldDescriptor.of(GenericSetParser.class, "PARSER", GenericParser.class);
                ResultHandle PARSER = scope.readStaticField(parserField);
                MethodDescriptor descriptor = MethodDescriptor.ofMethod(GenericSetParser.class, "startList", boolean.class, ParserContext.class);
                return scope.invokeVirtualMethod(descriptor, PARSER, ctx.ctx);
            }
        } else if (Map.class.isAssignableFrom(setter.type)) {
            if (!Map.class.equals(setter.type)) throw new QsonException("Cannot use concrete map.  Must use java.util.Map for property: " + setter.propertyName);
            if (setter.genericType instanceof ParameterizedType) {
                // invoke static field for property
                MethodDescriptor descriptor = MethodDescriptor.ofMethod(MapParser.class, "start", boolean.class, ParserContext.class);
                return scope.invokeVirtualMethod(descriptor,
                        scope.readStaticField(FieldDescriptor.of(fqn(), setter.propertyName, MapParser.class)),
                        ctx.ctx);
            } else {
                FieldDescriptor parserField = FieldDescriptor.of(GenericParser.class, "PARSER", GenericParser.class);
                ResultHandle PARSER = scope.readStaticField(parserField);
                MethodDescriptor descriptor = MethodDescriptor.ofMethod(GenericParser.class, "startObject", boolean.class, ParserContext.class);
                return scope.invokeVirtualMethod(descriptor, PARSER, ctx.ctx);
            }
        } else if (setter.type.equals(Object.class)) {
            FieldDescriptor parserField = FieldDescriptor.of(GenericParser.class, "PARSER", GenericParser.class);
            ResultHandle PARSER = scope.readStaticField(parserField);
            MethodDescriptor descriptor = MethodDescriptor.ofMethod(GenericParser.class, "start", boolean.class, ParserContext.class);
            return scope.invokeVirtualMethod(descriptor, PARSER, ctx.ctx);
        } else if (type.equals(OffsetDateTime.class) && !generator.hasMappingFor(type)) {
            ResultHandle PARSER = getOffsetDateTimeParser(setter, scope);
            MethodDescriptor descriptor = MethodDescriptor.ofMethod(ValueParser.class, "start", boolean.class.getName(), ParserContext.class.getName());
            return scope.invokeVirtualMethod(descriptor, PARSER, ctx.ctx);
        } else if (type.equals(Date.class) && !generator.hasMappingFor(type)) {
            ResultHandle PARSER = getDateUtilParser(setter, scope);
            MethodDescriptor descriptor = MethodDescriptor.ofMethod(ValueParser.class, "start", boolean.class.getName(), ParserContext.class.getName());
            return scope.invokeVirtualMethod(descriptor, PARSER, ctx.ctx);
        } else {
            FieldDescriptor parserField = FieldDescriptor.of(fqn(type), "PARSER", fqn(type));
            ResultHandle PARSER = scope.readStaticField(parserField);
            MethodDescriptor descriptor = MethodDescriptor.ofMethod(fqn(type), "start", boolean.class.getName(), ParserContext.class.getName());
            return scope.invokeVirtualMethod(descriptor, PARSER, ctx.ctx);
        }
    }


    private MethodDescriptor valueSeparator() {
        return MethodDescriptor.ofMethod(fqn(), "valueSeparator", boolean.class.getName(), ParserContext.class.getName());
    }

    private String fqn() {
        return className;
    }

    static class _ParserContext {
        ResultHandle ctx;

        public _ParserContext(ResultHandle ctx) {
            this.ctx = ctx;
        }

        public ResultHandle handleAny(BytecodeCreator scope, ResultHandle setter) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "handleAny", boolean.class, AnySetter.class), ctx, setter);

        }

        public ResultHandle consume(BytecodeCreator scope) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "consume", int.class), ctx);
        }

        public void clearToken(BytecodeCreator scope) {
            scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "clearToken", void.class), ctx);
        }

        public void popState(BytecodeCreator scope) {
            scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "popState", void.class), ctx);
        }

        public void endToken(BytecodeCreator scope) {
            scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "endToken", void.class), ctx);
        }

        public ResultHandle compareToken(BytecodeCreator scope, ResultHandle index, ResultHandle str) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "compareToken", boolean.class, int.class, String.class), ctx, index, str);
        }

        public ResultHandle tokenCharAt(BytecodeCreator scope, int index) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "tokenCharAt", int.class, int.class), ctx, scope.load(index));
        }


        public ResultHandle popToken(BytecodeCreator scope) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "popToken", String.class), ctx);
        }


        public ResultHandle popBooleanToken(BytecodeCreator scope) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "popBooleanToken", boolean.class), ctx);
        }

        public ResultHandle popBooleanObjectToken(BytecodeCreator scope) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "popBooleanObjectToken", Boolean.class), ctx);
        }

        public ResultHandle popIntToken(BytecodeCreator scope) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "popIntToken", int.class), ctx);
        }

        public ResultHandle popIntObjectToken(BytecodeCreator scope) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "popIntObjectToken", Integer.class), ctx);
        }

        public ResultHandle popShortToken(BytecodeCreator scope) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "popShortToken", short.class), ctx);
        }

        public ResultHandle popShortObjectToken(BytecodeCreator scope) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "popShortObjectToken", Short.class), ctx);
        }

        public ResultHandle popByteToken(BytecodeCreator scope) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "popByteToken", byte.class), ctx);
        }

        public ResultHandle popByteObjectToken(BytecodeCreator scope) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "popByteObjectToken", Byte.class), ctx);
        }

        public ResultHandle popLongToken(BytecodeCreator scope) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "popLongToken", long.class), ctx);
        }

        public ResultHandle popLongObjectToken(BytecodeCreator scope) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "popLongObjectToken", Long.class), ctx);
        }

        public ResultHandle popFloatToken(BytecodeCreator scope) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "popFloatToken", float.class), ctx);
        }

        public ResultHandle popFloatObjectToken(BytecodeCreator scope) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "popFloatObjectToken", Float.class), ctx);
        }

        public ResultHandle popDoubleToken(BytecodeCreator scope) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "popDoubleToken", double.class), ctx);
        }

        public ResultHandle popDoubleObjectToken(BytecodeCreator scope) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "popDoubleObjectToken", Double.class), ctx);
        }

        public ResultHandle target(BytecodeCreator scope) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "target", Object.class), ctx);
        }

        public void pushTarget(BytecodeCreator scope, ResultHandle obj) {
            scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "pushTarget", void.class, Object.class), ctx, obj);
        }

        public void pushState(BytecodeCreator scope, ResultHandle func, ResultHandle index) {
            scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "pushState", void.class, ParserState.class, int.class), ctx, func, index);
        }

        public void pushState(BytecodeCreator scope, ResultHandle func) {
            scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "pushState", void.class, ParserState.class), ctx, func);
        }

        public ResultHandle popTarget(BytecodeCreator scope) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "popTarget", Object.class), ctx);
        }

        public ResultHandle stateIndex(BytecodeCreator scope) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "stateIndex", int.class), ctx);
        }

        public ResultHandle skipToQuote(BytecodeCreator scope) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "skipToQuote", int.class), ctx);
        }


    }


}
