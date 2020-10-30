package io.quarkus.qson.generator;

import io.quarkus.qson.desserializer.JsonParser;
import io.quarkus.qson.serializer.ObjectWriter;

import java.lang.reflect.Type;
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
    private ConcurrentHashMap<String, ObjectWriter> serializers = new ConcurrentHashMap<>();
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

    public void parsersFor(Class... classes) {
        for (Class clz : classes) parserFor(clz);
    }

    public JsonParser parserFor(Class clz) {
       return parserFor(clz, clz);
    }

    public JsonParser parserFor(Class clz, Type genericType) {
        String key = key(clz, genericType);
        JsonParser parser = deserializers.get(key);
        if (parser != null) return parser;
        synchronized(deserializers) {
            parser = deserializers.get(key);
            if (parser != null) return parser;
            Set<String> generated = new HashSet<>();
            generateDeserializers(clz, genericType, generated);
            try {
                Class deserializer = cl.loadClass(Deserializer.fqn(clz, genericType));
                parser = (JsonParser) deserializer.newInstance();
                deserializers.put(key, parser);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        return parser;
    }

    private void generateDeserializers(Class clz, Type genericType, Set<String> generated) {
        String key = key(clz, genericType);
        Deserializer.Builder builder = Deserializer.create(clz, genericType).output(cl).generate();
        generated.add(key);
        for (Map.Entry<Class, Type> entry : builder.referenced().entrySet()) {
            String refKey = key(entry.getKey(), entry.getValue());
            if (generated.contains(refKey) || deserializers.containsKey(refKey)) continue;
            generateDeserializers(entry.getKey(), entry.getValue(), generated);
        }
    }

    public void writersFor(Class... classes) {
        for (Class clz : classes) writerFor(clz);
    }

    public ObjectWriter writerFor(Class clz) {
        return writerFor(clz, clz);
    }

    public ObjectWriter writerFor(Class clz, Type genericType) {
        String key = key(clz, genericType);
        ObjectWriter writer = serializers.get(key);
        if (writer != null) return writer;
        synchronized(deserializers) {
            writer = serializers.get(key);
            if (writer != null) return writer;
            Set<String> generated = new HashSet<>();
            generateSerializers(clz, genericType, generated);
            try {
                Class serializer = cl.loadClass(Serializer.fqn(clz, genericType));
                writer = (ObjectWriter) serializer.newInstance();
                serializers.put(key, writer);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        return writer;
    }

    private void generateSerializers(Class clz, Type genericType, Set<String> generated) {
        String key = key(clz, genericType);
        Serializer.Builder builder = Serializer.create(clz, genericType).output(cl).generate();
        generated.add(key);
        for (Map.Entry<Class, Type> entry : builder.referenced().entrySet()) {
            String refKey = key(entry.getKey(), entry.getValue());
            if (generated.contains(refKey) || serializers.containsKey(refKey)) continue;
            generateSerializers(entry.getKey(), entry.getValue(), generated);
        }
    }


    public String key(Class clz, Type genericType) {
        return genericType == null ? clz.getTypeName() : genericType.getTypeName();
    }
}
