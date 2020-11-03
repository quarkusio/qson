package io.quarkus.qson.generator;

import io.quarkus.qson.GenericType;
import io.quarkus.qson.desserializer.JsonParser;
import io.quarkus.qson.serializer.ObjectWriter;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cannot be used with Graal!
 *
 * This will generate bytecode for serializers and deserializers and load them through a custom class loader
 *
 */
public class JsonMapper {
    private ConcurrentHashMap<String, JsonParser> deserializers = new ConcurrentHashMap<>();
    private Map<String, String> generatedDeserializers = new HashMap<>();
    private ConcurrentHashMap<String, ObjectWriter> serializers = new ConcurrentHashMap<>();
    private Map<String, String> generatedSerializers = new HashMap<>();
    private final GizmoClassLoader cl;

    /**
     * Uses Thread.currentThread().getContextClassLoader() as parent classloader for bytecode generation
     *
     */
    public JsonMapper() {
        this(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Parent classloader for the custom class loader that loads generated parser/writer bytecode
     *
     * @param parent
     */
    public JsonMapper(ClassLoader parent) {
        cl = new GizmoClassLoader(parent);
    }

    /**
     * Generates parsers for passed in list of classes and caches them for future lookup
     *
     * @param classes
     */
    public void parsersFor(Class... classes) {
        for (Class clz : classes) parserFor(clz);
    }

    public JsonParser parserFor(Class clz) {
       return parserFor(clz, clz);
    }

    public JsonParser parserFor(GenericType type) {
        return parserFor(type.getRawType(), type.getType());
    }

    public JsonParser parserFor(Class clz, Type genericType) {
        String key = key(clz, genericType);
        JsonParser parser = deserializers.get(key);
        if (parser != null) return parser;
        synchronized(deserializers) {
            parser = deserializers.get(key);
            if (parser != null) return parser;
            String className = generateDeserializers(clz, genericType);
            try {
                Class deserializer = cl.loadClass(className);
                parser = (JsonParser) deserializer.newInstance();
                deserializers.put(key, parser);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        return parser;
    }

    private String generateDeserializers(Class clz, Type genericType) {
        String key = key(clz, genericType);
        if (generatedDeserializers.containsKey(key)) return generatedDeserializers.get(key);
        Deserializer.Builder builder = Deserializer.create(clz, genericType).output(cl).generate();
        generatedDeserializers.put(key, builder.className());
        for (Map.Entry<Class, Type> entry : builder.referenced().entrySet()) {
            String refKey = key(entry.getKey(), entry.getValue());
            if (generatedDeserializers.containsKey(refKey)) continue;
            generateDeserializers(entry.getKey(), entry.getValue());
        }
        return builder.className();
    }

    /**
     * Generates writers for passed in classes and caches them for future lookup
     *
     * @param classes
     */
    public void writersFor(Class... classes) {
        for (Class clz : classes) writerFor(clz);
    }

    public void writersFor(GenericType... types) {
        for (GenericType type : types) writerFor(type);

    }

    public ObjectWriter writerFor(Class clz) {
        return writerFor(clz, clz);
    }

    public ObjectWriter writerFor(GenericType type) {
        return writerFor(type.getRawType(), type.getType());
    }

    public ObjectWriter writerFor(Class clz, Type genericType) {
        String key = key(clz, genericType);
        ObjectWriter writer = serializers.get(key);
        if (writer != null) return writer;
        synchronized(serializers) {
            writer = serializers.get(key);
            if (writer != null) return writer;
            String className = generateSerializers(clz, genericType);
            try {
                Class serializer = cl.loadClass(className);
                writer = (ObjectWriter) serializer.newInstance();
                serializers.put(key, writer);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        return writer;
    }

    private String generateSerializers(Class clz, Type genericType) {
        String key = key(clz, genericType);
        if (generatedSerializers.containsKey(key)) return generatedSerializers.get(key);
        Serializer.Builder builder = Serializer.create(clz, genericType).output(cl).generate();
        generatedSerializers.put(key, builder.className());
        for (Map.Entry<Class, Type> entry : builder.referenced().entrySet()) {
            String refKey = key(entry.getKey(), entry.getValue());
            if (generatedSerializers.containsKey(refKey)) continue;
            generateSerializers(entry.getKey(), entry.getValue());
        }
        return builder.className();
    }

    private String key(Class clz, Type genericType) {
        return genericType == null ? clz.getTypeName() : genericType.getTypeName();
    }
}
