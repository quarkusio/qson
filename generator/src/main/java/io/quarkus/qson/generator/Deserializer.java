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
import io.quarkus.qson.Types;
import io.quarkus.qson.desserializer.BaseParser;
import io.quarkus.qson.desserializer.BooleanParser;
import io.quarkus.qson.desserializer.ByteParser;
import io.quarkus.qson.desserializer.DoubleParser;
import io.quarkus.qson.desserializer.FloatParser;
import io.quarkus.qson.desserializer.IntegerParser;
import io.quarkus.qson.desserializer.JsonParser;
import io.quarkus.qson.desserializer.LongParser;
import io.quarkus.qson.desserializer.ParserContext;
import io.quarkus.qson.desserializer.ContextValue;
import io.quarkus.qson.desserializer.GenericParser;
import io.quarkus.qson.desserializer.GenericSetParser;
import io.quarkus.qson.desserializer.ListParser;
import io.quarkus.qson.desserializer.MapParser;
import io.quarkus.qson.desserializer.ObjectParser;
import io.quarkus.qson.desserializer.ParserState;
import io.quarkus.qson.desserializer.SetParser;
import io.quarkus.qson.desserializer.ShortParser;
import io.quarkus.qson.desserializer.StringParser;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

public class Deserializer {

    // constructor
    public static final String INIT = "<init>";
    // static initializer
    public static final String CLINIT = "<clinit>";

    public static Builder create(Class targetType) {
        return new Builder().type(targetType);
    }
    public static Builder create(Class targetType, Type generic) {
        return new Builder().type(targetType).generic(generic);
    }

    public static class Builder {
        Class targetType;
        Type targetGenericType;
        ClassOutput output;
        String className;
        Map<Class, Type> referenced = new HashMap<>();

        private Builder() {
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

        public String className() {
            return className;
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

        public Map<Class, Type> referenced() {
            return referenced;
        }
        public Builder generate() {
            if (targetGenericType == null) targetGenericType = targetType;
            if (int.class.equals(targetType)
                    || Integer.class.equals(targetType)) {
                className = IntegerParser.class.getName();
                return this;
            }
            if (short.class.equals(targetType)
                    || Short.class.equals(targetType)) {
                className = ShortParser.class.getName();
                return this;
            }
            if (long.class.equals(targetType)
                    || Long.class.equals(targetType)) {
                className = LongParser.class.getName();
                return this;
            }
            if (byte.class.equals(targetType)
                    || Byte.class.equals(targetType)) {
                className = ByteParser.class.getName();
                return this;
            }
            if (float.class.equals(targetType)
                    || Float.class.equals(targetType)
            ) {
                className = FloatParser.class.getName();
                return this;
            }
            if (double.class.equals(targetType)
                    || Double.class.equals(targetType)
            ) {
                className = DoubleParser.class.getName();
                return this;
            }
            if (boolean.class.equals(targetType)
                    || Boolean.class.equals(targetType)
            ) {
                className = BooleanParser.class.getName();
                return this;
            }
            if (String.class.equals(targetType)) {
                className = StringParser.class.getName();
                return this;
            }
            if (Map.class.equals(targetType)
                || List.class.equals(targetType)
                || Set.class.equals(targetType)) {
                if (targetGenericType instanceof ParameterizedType) {
                    if (className == null) {
                        className = Util.generatedClassName(targetGenericType);
                        className += "__Parser";
                    }
                    Deserializer deserializer = new Deserializer(output, className, targetType, targetGenericType);
                    deserializer.generateCollection();
                    referenced = deserializer.referenced;
                } else {
                    className = GenericParser.class.getName();
                }
                return this;
            }
            Deserializer deserializer = new Deserializer(output, targetType, targetGenericType);
            deserializer.generate();
            referenced = deserializer.referenced;
            className = fqn(targetType, targetGenericType);
            return this;
        }
    }

    ClassCreator creator;

    final Class targetType;
    final Type targetGenericType;
    List<Setter> setters = new LinkedList<>();
    Map<Class, Type> referenced = new HashMap<>();
    final ClassOutput classOutput;
    final String className;

    public static String name(Class clz, Type genericType) {
        return clz.getSimpleName() + "__Parser";
    }

    public static String fqn(Class clz, Type genericType) {
        return clz.getName() + "__Parser";
    }

    Deserializer(ClassOutput classOutput, Class targetType, Type targetGenericType) {
        this(classOutput, fqn(targetType, targetGenericType), targetType, targetGenericType);
    }

    Deserializer(ClassOutput classOutput, String className, Class targetType, Type targetGenericType) {
        this.targetType = targetType;
        this.targetGenericType = targetGenericType;
        this.classOutput = classOutput;
        this.className = className;
    }

    void generateCollection() {
        creator = ClassCreator.builder().classOutput(classOutput)
                .className(className)
                .interfaces(JsonParser.class).build();
        MethodCreator staticConstructor = creator.getMethodCreator(CLINIT, void.class);
        staticConstructor.setModifiers(ACC_STATIC);
        collectionField(staticConstructor, targetType, targetGenericType, "collection");
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
            throw new RuntimeException("Unsupported collection type: " + targetType.getName());
        }
        ResultHandle collection = startState.readStaticField(FieldDescriptor.of(className, "collection", collectionParser));
        ResultHandle result = startState.invokeVirtualMethod(MethodDescriptor.ofMethod(collectionParser, "startState", ParserState.class), collection);
        startState.returnValue(result);
        creator.close();
    }

    void generate() {
        creator = ClassCreator.builder().classOutput(classOutput)
                .className(className)
                .superClass(ObjectParser.class).build();
        findSetters(targetType);

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

        for (Setter setter : setters) {
            collectionField(staticConstructor, setter);
            MethodCreator method = propertyEndMethod(setter);
            propertyEndFunction(setter, staticConstructor, method);
        }
        staticConstructor.returnValue(null);
    }

    private void collectionField(MethodCreator staticConstructor, Setter setter) {
        Type genericType = setter.genericType;
        Class type = setter.type;
        String property = setter.property;
        collectionField(staticConstructor, type, genericType, property);

    }

    private void collectionField(MethodCreator staticConstructor, Class type, Type genericType, String property) {
        if (genericType instanceof ParameterizedType) {
            if (Map.class.isAssignableFrom(type)) {
                ParameterizedType pt = (ParameterizedType) genericType;
                Type keyType = pt.getActualTypeArguments()[0];
                Class keyClass = Types.getRawType(keyType);
                Type valueType = pt.getActualTypeArguments()[1];
                Class valueClass = Types.getRawType(valueType);

                if (Map.class.isAssignableFrom(valueClass) && !valueClass.equals(Map.class)) throw new RuntimeException("Must use java.util.Map for: " + property);
                if (List.class.isAssignableFrom(valueClass) && !valueClass.equals(List.class)) throw new RuntimeException("Must use java.util.List for: " + property);
                if (Set.class.isAssignableFrom(valueClass) && !valueClass.equals(Set.class)) throw new RuntimeException("Must use java.util.Set for: " + property);

                if (valueClass.equals(Map.class) || valueClass.equals(List.class) || valueClass.equals(Set.class)) {
                    collectionField(staticConstructor, valueClass, valueType, property + "_n");
                }

                ResultHandle keyContextValue = contextValue(keyClass, keyType, staticConstructor);
                ResultHandle valueContextValue = contextValue(valueClass, valueType, staticConstructor);
                ResultHandle valueState = collectionValueState(valueClass, valueType, staticConstructor, property);
                ResultHandle continueValueState = continueValueState(valueClass, valueType, staticConstructor, property);
                FieldCreator mapParser = creator.getFieldCreator(property, MapParser.class).setModifiers(ACC_STATIC | ACC_PRIVATE | ACC_FINAL);
                ResultHandle instance = staticConstructor.newInstance(MethodDescriptor.ofConstructor(MapParser.class, ContextValue.class, ContextValue.class, ParserState.class, ParserState.class),
                        keyContextValue, valueContextValue, valueState, continueValueState);
                staticConstructor.writeStaticField(mapParser.getFieldDescriptor(), instance);
            } else if (List.class.isAssignableFrom(type)) {
                ParameterizedType pt = (ParameterizedType) genericType;
                Type valueType = pt.getActualTypeArguments()[0];
                Class valueClass = Types.getRawType(valueType);

                if (Map.class.isAssignableFrom(valueClass) && !valueClass.equals(Map.class)) throw new RuntimeException("Must use java.util.Map for property: " + property);
                if (List.class.isAssignableFrom(valueClass) && !valueClass.equals(List.class)) throw new RuntimeException("Must use java.util.List for property: " + property);
                if (Set.class.isAssignableFrom(valueClass) && !valueClass.equals(Set.class)) throw new RuntimeException("Must use java.util.Set for property: " + property);

                if (valueClass.equals(Map.class) || valueClass.equals(List.class) || valueClass.equals(Set.class)) {
                    collectionField(staticConstructor, valueClass, valueType, property + "_n");
                }


                ResultHandle valueContextValue = contextValue(valueClass, valueType, staticConstructor);
                ResultHandle valueState = collectionValueState(valueClass, valueType, staticConstructor, property);
                FieldCreator collectionParser = creator.getFieldCreator(property, ListParser.class).setModifiers(ACC_STATIC | ACC_PRIVATE | ACC_FINAL);
                ResultHandle instance = staticConstructor.newInstance(MethodDescriptor.ofConstructor(ListParser.class, ContextValue.class, ParserState.class),
                        valueContextValue, valueState);
                staticConstructor.writeStaticField(collectionParser.getFieldDescriptor(), instance);

            } else if (Set.class.isAssignableFrom(type)) {
                ParameterizedType pt = (ParameterizedType) genericType;
                Type valueType = pt.getActualTypeArguments()[0];
                Class valueClass = Types.getRawType(valueType);

                if (Map.class.isAssignableFrom(valueClass) && !valueClass.equals(Map.class)) throw new RuntimeException("Must use java.util.Map for property: " + property);
                if (List.class.isAssignableFrom(valueClass) && !valueClass.equals(List.class)) throw new RuntimeException("Must use java.util.List for property: " + property);
                if (Set.class.isAssignableFrom(valueClass) && !valueClass.equals(Set.class)) throw new RuntimeException("Must use java.util.Set for property: " + property);

                if (valueClass.equals(Map.class) || valueClass.equals(List.class) || valueClass.equals(Set.class)) {
                    collectionField(staticConstructor, valueClass, valueType, property + "_n");
                }

                ResultHandle valueContextValue = contextValue(valueClass, valueType, staticConstructor);
                ResultHandle valueState = collectionValueState(valueClass, valueType, staticConstructor, property);
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
        } else if (type.equals(OffsetDateTime.class)) {
            return scope.readStaticField(FieldDescriptor.of(ContextValue.class, "OFFSET_DATETIME_VALUE", ContextValue.class));
        } else if (type.equals(BigDecimal.class)) {
            return scope.readStaticField(FieldDescriptor.of(ContextValue.class, "BIGDECIMAL_VALUE", ContextValue.class));
        } else {
            return scope.readStaticField(FieldDescriptor.of(ContextValue.class, "OBJECT_VALUE", ContextValue.class));
        }
    }

    private ResultHandle collectionValueState(Class type, Type genericType, BytecodeCreator scope, String property) {
        if (type.equals(String.class)
                || type.equals(char.class) || type.equals(Character.class)
                || type.equals(OffsetDateTime.class)
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
        } else {
            FieldDescriptor parserField = FieldDescriptor.of(fqn(type, genericType), "PARSER", fqn(type, genericType));
            ResultHandle PARSER = scope.readStaticField(parserField);
            return scope.readInstanceField(FieldDescriptor.of(ObjectParser.class, "start", ParserState.class), PARSER);
        }
    }

    private ResultHandle continueValueState(Class type, Type genericType, BytecodeCreator scope, String property) {
        if (type.equals(String.class)
                || type.equals(char.class) || type.equals(Character.class)
                || type.equals(OffsetDateTime.class)
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
        } else {
            FieldDescriptor parserField = FieldDescriptor.of(fqn(type, genericType), "PARSER", fqn(type, genericType));
            ResultHandle PARSER = scope.readStaticField(parserField);
            return scope.readInstanceField(FieldDescriptor.of(ObjectParser.class, "continueStart", ParserState.class), PARSER);
        }
    }

    private void propertyEndFunction(Setter setter, MethodCreator staticConstructor, MethodCreator method) {
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

    private MethodCreator propertyEndMethod(Setter setter) {
        String endName = endProperty(setter);
        MethodCreator method = creator.getMethodCreator(endName, void.class, ParserContext.class);
        method.setModifiers(ACC_STATIC | ACC_FINAL | ACC_PUBLIC);
        _ParserContext ctx = new _ParserContext(method.getMethodParam(0));
        ResultHandle popSetter = popSetterValue(ctx, setter, method);
        AssignableResultHandle target = method.createVariable(targetType);
        method.assign(target, ctx.target(method));
        MethodDescriptor set = MethodDescriptor.ofMethod(setter.method);
        method.invokeVirtualMethod(set, target, popSetter);
        method.returnValue(null);
        return method;
    }

    private ResultHandle popSetterValue(_ParserContext ctx, Setter setter, BytecodeCreator scope) {
        if (setter.type.equals(String.class)) {
            return ctx.popToken(scope);
        } else if (setter.type.equals(short.class) || setter.type.equals(Short.class)) {
            return ctx.popShortToken(scope);
        } else if (setter.type.equals(byte.class) || setter.type.equals(Byte.class)) {
            return ctx.popByteToken(scope);
        } else if (setter.type.equals(int.class) || setter.type.equals(Integer.class)) {
            return ctx.popIntToken(scope);
        } else if (setter.type.equals(long.class) || setter.type.equals(Long.class)) {
            return ctx.popLongToken(scope);
        } else if (setter.type.equals(float.class) || setter.type.equals(Float.class)) {
            return ctx.popFloatToken(scope);
        } else if (setter.type.equals(double.class) || setter.type.equals(Double.class)) {
            return ctx.popDoubleToken(scope);
        } else if (setter.type.equals(boolean.class) || setter.type.equals(Boolean.class)) {
            return ctx.popBooleanToken(scope);
        } else {
            return ctx.popTarget(scope);
        }
    }

    private String endProperty(Setter setter) {
        return setter.property + "End";
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

        chooseField(method, ctx, stateIndex, setters, 0);

        ResultHandle result = method.invokeVirtualMethod(MethodDescriptor.ofMethod(BaseParser.class, "skipValue", boolean.class, ParserContext.class),
                method.readStaticField(FieldDescriptor.of(BaseParser.class, "PARSER", BaseParser.class)), ctx.ctx);
        method.returnValue(result);
    }

    private void chooseField(BytecodeCreator scope, _ParserContext ctx, ResultHandle stateIndex, List<Setter> setters, int offset) {
        if (setters.size() == 1) {
            Setter setter = setters.get(0);
            compareToken(scope, ctx, stateIndex, offset, setter);
            return;
        }
        ResultHandle c = ctx.tokenCharAt(scope, offset);
        for (int i = 0; i < setters.size(); i++) {
            Setter setter = setters.get(i);
            if (offset >= setter.name.length()) {
                compareToken(scope, ctx, stateIndex, offset, setter);
                continue;
            }
            char ch = setter.name.charAt(offset);
            List<Setter> sameChars = new ArrayList<>();
            sameChars.add(setter);
            BytecodeCreator ifScope = scope.createScope();
            BranchResult branchResult = ifScope.ifIntegerEqual(c, ifScope.load(ch));
            for (i = i + 1; i < setters.size(); i++) {
                Setter next = setters.get(i);
                if (offset < next.name.length() && next.name.charAt(offset) == ch) {
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

    private void compareToken(BytecodeCreator scope, _ParserContext ctx, ResultHandle stateIndex, int offset, Setter setter) {
        BytecodeCreator ifScope = scope.createScope();
        ResultHandle check = ctx.compareToken(ifScope, ifScope.load(offset), ifScope.load(setter.name.substring(offset)));
        matchHandler(ctx, stateIndex, setter, ifScope.ifNonZero(check).trueBranch());
    }

    private void matchHandler(_ParserContext ctx, ResultHandle stateIndex, Setter setter, BytecodeCreator scope) {
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

    private ResultHandle continueState(Setter setter, BytecodeCreator scope) {
        Class type = setter.type;
        Type genericType = setter.genericType;
        if (type.equals(String.class)
                || type.equals(char.class) || type.equals(Character.class)
                || type.equals(OffsetDateTime.class)
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
            if (!List.class.equals(setter.type)) throw new RuntimeException("Cannot use concrete list.  Must use java.util.List for property: " + setter.property);
            if (setter.genericType instanceof ParameterizedType) {
                // continue is on static field
                FieldDescriptor mapFieldDesc = FieldDescriptor.of(fqn(), setter.property, ListParser.class);
                ResultHandle mapField = scope.readStaticField(mapFieldDesc);
                return scope.readInstanceField(FieldDescriptor.of(ListParser.class, "continueStart", ParserState.class), mapField);
            } else {
                FieldDescriptor parserField = FieldDescriptor.of(GenericParser.class, "PARSER", GenericParser.class);
                ResultHandle PARSER = scope.readStaticField(parserField);
                return scope.readInstanceField(FieldDescriptor.of(GenericParser.class, "continueStartObject", ParserState.class), PARSER);
            }
        } else if (Set.class.isAssignableFrom(setter.type)) {
            if (!Set.class.equals(setter.type)) throw new RuntimeException("Cannot use concrete set.  Must use java.util.Set for property: " + setter.property);
            if (setter.genericType instanceof ParameterizedType) {
                // continue is on static field
                FieldDescriptor mapFieldDesc = FieldDescriptor.of(fqn(), setter.property, SetParser.class);
                ResultHandle mapField = scope.readStaticField(mapFieldDesc);
                return scope.readInstanceField(FieldDescriptor.of(SetParser.class, "continueStart", ParserState.class), mapField);
            } else {
                FieldDescriptor parserField = FieldDescriptor.of(GenericSetParser.class, "PARSER", GenericParser.class);
                ResultHandle PARSER = scope.readStaticField(parserField);
                return scope.readInstanceField(FieldDescriptor.of(GenericSetParser.class, "continueStartObject", ParserState.class), PARSER);
            }
        } else if (Map.class.isAssignableFrom(setter.type)) {
            if (!Map.class.equals(setter.type)) throw new RuntimeException("Cannot use concrete map.  Must use java.util.Map for property: " + setter.property);
            if (setter.genericType instanceof ParameterizedType) {
                FieldDescriptor mapFieldDesc = FieldDescriptor.of(fqn(), setter.property, MapParser.class);
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
        } else {
            // todo handle nested collections and maps
            FieldDescriptor parserField = FieldDescriptor.of(fqn(type, genericType), "PARSER", fqn(type, genericType));
            ResultHandle PARSER = scope.readStaticField(parserField);
            return scope.readInstanceField(FieldDescriptor.of(ObjectParser.class, "continueStart", ParserState.class), PARSER);
        }
    }

    private ResultHandle callStartState(_ParserContext ctx, Setter setter, BytecodeCreator scope) {
        Class type = setter.type;
        Type genericType = setter.genericType;
        if (type.equals(String.class)
                || type.equals(char.class) || type.equals(Character.class)
                || type.equals(OffsetDateTime.class)
        ) {
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
            if (!List.class.equals(setter.type)) throw new RuntimeException("Cannot use concrete list.  Must use java.util.List for property: " + setter.property);
            if (setter.genericType instanceof ParameterizedType) {
                // invoke static field for property
                MethodDescriptor descriptor = MethodDescriptor.ofMethod(ListParser.class, "start", boolean.class, ParserContext.class);
                return scope.invokeVirtualMethod(descriptor,
                        scope.readStaticField(FieldDescriptor.of(fqn(), setter.property, ListParser.class)),
                        ctx.ctx);
            } else {
                FieldDescriptor parserField = FieldDescriptor.of(GenericParser.class, "PARSER", GenericParser.class);
                ResultHandle PARSER = scope.readStaticField(parserField);
                MethodDescriptor descriptor = MethodDescriptor.ofMethod(GenericParser.class, "startList", boolean.class, ParserContext.class);
                return scope.invokeVirtualMethod(descriptor, PARSER, ctx.ctx);
            }
        } else if (Set.class.isAssignableFrom(setter.type)) {
            if (!Set.class.equals(setter.type)) throw new RuntimeException("Cannot use concrete set.  Must use java.util.Set for property: " + setter.property);
            if (setter.genericType instanceof ParameterizedType) {
                // invoke static field for property
                MethodDescriptor descriptor = MethodDescriptor.ofMethod(SetParser.class, "start", boolean.class, ParserContext.class);
                return scope.invokeVirtualMethod(descriptor,
                        scope.readStaticField(FieldDescriptor.of(fqn(), setter.property, SetParser.class)),
                        ctx.ctx);
            } else {
                FieldDescriptor parserField = FieldDescriptor.of(GenericSetParser.class, "PARSER", GenericParser.class);
                ResultHandle PARSER = scope.readStaticField(parserField);
                MethodDescriptor descriptor = MethodDescriptor.ofMethod(GenericSetParser.class, "startList", boolean.class, ParserContext.class);
                return scope.invokeVirtualMethod(descriptor, PARSER, ctx.ctx);
            }
        } else if (Map.class.isAssignableFrom(setter.type)) {
            if (!Map.class.equals(setter.type)) throw new RuntimeException("Cannot use concrete map.  Must use java.util.Map for property: " + setter.property);
            if (setter.genericType instanceof ParameterizedType) {
                // invoke static field for property
                MethodDescriptor descriptor = MethodDescriptor.ofMethod(MapParser.class, "start", boolean.class, ParserContext.class);
                return scope.invokeVirtualMethod(descriptor,
                        scope.readStaticField(FieldDescriptor.of(fqn(), setter.property, MapParser.class)),
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
        } else {
            FieldDescriptor parserField = FieldDescriptor.of(fqn(type, genericType), "PARSER", fqn(type, genericType));
            ResultHandle PARSER = scope.readStaticField(parserField);
            MethodDescriptor descriptor = MethodDescriptor.ofMethod(fqn(type, genericType), "start", boolean.class.getName(), ParserContext.class.getName());
            return scope.invokeVirtualMethod(descriptor, PARSER, ctx.ctx);
        }
    }


    private MethodDescriptor valueSeparator() {
        return MethodDescriptor.ofMethod(fqn(), "valueSeparator", boolean.class.getName(), ParserContext.class.getName());
    }

    private String fqn() {
        return className;
    }

    private void findSetters(Class clz) {
        for (Method m : clz.getMethods()) {
            if (!isSetter(m))
                continue;
            Class paramType = m.getParameterTypes()[0];
            Type paramGenericType = m.getGenericParameterTypes()[0];
            String name;
            if (m.getName().length() > 4) {
                name = Character.toLowerCase(m.getName().charAt(3)) + m.getName().substring(4);
            } else {
                name = m.getName().substring(3).toLowerCase();
            }
            setters.add(new Setter(name, m, paramType, paramGenericType));
            Util.addReference(referenced, paramType, paramGenericType);
        }
        Collections.sort(setters, (setter, t1) -> setter.name.compareTo(t1.name));
    }


    static boolean isSetter(Method m) {
        return !Modifier.isStatic(m.getModifiers()) && m.getName().startsWith("set") && m.getName().length() > "set".length()
                && m.getParameterCount() == 1;
    }

    class Setter {
        String name; // json key
        String property; // this is method name without set/get
        Method method;
        Class type;
        Type genericType;

        public Setter(String name, Method method, Class type, Type genericType) {
            this.name = name;
            this.property = name;
            this.method = method;
            this.type = type;
            this.genericType = genericType;
        }
    }


    static class _ParserContext {
        ResultHandle ctx;

        public _ParserContext(ResultHandle ctx) {
            this.ctx = ctx;
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

        public ResultHandle popIntToken(BytecodeCreator scope) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "popIntToken", int.class), ctx);
        }

        public ResultHandle popShortToken(BytecodeCreator scope) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "popShortToken", short.class), ctx);
        }

        public ResultHandle popByteToken(BytecodeCreator scope) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "popByteToken", byte.class), ctx);
        }

        public ResultHandle popLongToken(BytecodeCreator scope) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "popLongToken", long.class), ctx);
        }

        public ResultHandle popFloatToken(BytecodeCreator scope) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "popFloatToken", float.class), ctx);
        }

        public ResultHandle popDoubleToken(BytecodeCreator scope) {
            return scope.invokeInterfaceMethod(MethodDescriptor.ofMethod(ParserContext.class, "popDoubleToken", double.class), ctx);
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
