package io.quarkus.qson.generator;

import io.quarkus.qson.GenericType;
import io.quarkus.qson.Types;
import io.quarkus.qson.desserializer.ByteArrayParserContext;
import io.quarkus.qson.desserializer.JsonParser;
import io.quarkus.qson.serializer.ByteArrayByteWriter;
import io.quarkus.qson.serializer.JsonByteWriter;
import io.quarkus.qson.serializer.JsonWriter;
import io.quarkus.qson.serializer.ObjectWriter;
import io.quarkus.qson.serializer.OutputStreamByteWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
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

    /**
     * Generate a parser for specific class and caches it for future lookup.
     *
     * @param clz
     * @return
     */
    public JsonParser parserFor(Class clz) {
       return parserFor(clz, clz);
    }

    /**
     * Generate a parser for the specified GenericType.  Useful to bypass type erasure.
     *
     * For example:
     * <pre>
     *  GenericType&lt;List&lt;Foo&gt;&gt; fooListType = new GenericType&lt;List&lt;Foo&gt;&gt;() {};
     *  JsonParser parser = mapper.parserFor(fooListType);
     * </pre>
     *
     * @param type
     * @return
     */
    public JsonParser parserFor(GenericType type) {
        return parserFor(type.getRawType(), type.getType());
    }

    /**
     * Generate parser based on class and generic type parameters.
     *
     * @param clz
     * @param genericType
     * @return
     */
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

    /**
     * Deserialize a complete byte buffer into the specified type.
     *
     * @param fullBuffer
     * @param type
     * @param genericType
     * @param <T>
     * @return
     */
    public <T> T read(byte[] fullBuffer, Class<T> type, Type genericType) {
        JsonParser parser = parserFor(type, genericType);
        ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
        return ctx.finish(fullBuffer);
    }

    /**
     * Deserialize a complete byte buffer into the specified type.
     *
     * @param fullBuffer
     * @param type
     * @param <T>
     * @return
     */
    public <T> T read(byte[] fullBuffer, Class<T> type) {
        return read(fullBuffer, type, type);
    }

    /**
     * Deserialize a complete byte buffer into the specified type.
     *
     * @param fullBuffer
     * @param type
     * @param <T>
     * @return
     */
    public <T> T read(byte[] fullBuffer, GenericType<T> type) {
        return (T)read(fullBuffer, type.getRawType(), type.getType());
    }

    /**
     * Deserialize a complete json string into the specified type
     *
     * @param json
     * @param type
     * @param genericType
     * @param <T>
     * @return
     */
    public <T> T read(String json, Class<T> type, Type genericType) {
        return read(json.getBytes(JsonByteWriter.UTF8), type, genericType);
    }

    /**
     * Deserialize a complete json string into the specified type
     *
     * @param json
     * @param type
     * @param <T>
     * @return
     */
    public <T> T read(String json, Class<T> type) {
        return read(json, type, type);
    }

    /**
     * Deserialize a complete json string into the specified type
     *
     * @param json
     * @param type
     * @param <T>
     * @return
     */
    public <T> T read(String json, GenericType<T> type) {
        return (T)read(json, type.getRawType(), type.getType());
    }

    /**
     * Deserialize the specified type from an InputStream
     *
     * @param is
     * @param type
     * @param genericType
     * @param <T>
     * @return
     * @throws IOException
     */
    public <T> T read(InputStream is, Class<T> type, Type genericType) throws IOException {
        JsonParser parser = parserFor(type, genericType);
        ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
        return ctx.finish(is);
    }

    /**
     * Deserialize the specified type from an InputStream
     *
     * @param is
     * @param type
     * @param <T>
     * @return
     * @throws IOException
     */
    public <T> T read(InputStream is, Class<T> type) throws IOException {
        return read(is, type, type);
    }

    /**
     * Deserialize the specified type from an InputStream
     *
     * @param is
     * @param type
     * @param <T>
     * @return
     * @throws IOException
     */
    public <T> T read(InputStream is, GenericType<T> type) throws IOException {
        return (T)read(is, type.getRawType(), type.getType());
    }

    private String generateDeserializers(Class clz, Type genericType) {
        String key = key(clz, genericType);
        if (generatedDeserializers.containsKey(key)) return generatedDeserializers.get(key);
        Deserializer.Builder builder = Deserializer.create(clz, genericType).output(cl).generate();
        generatedDeserializers.put(key, builder.className());
        for (Map.Entry<Type, Class> entry : builder.referenced().entrySet()) {
            String refKey = key(entry.getValue(), entry.getKey());
            if (generatedDeserializers.containsKey(refKey)) continue;
            generateDeserializers(entry.getValue(), entry.getKey());
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

    /**
     * Generates writers for passed in GenericTypes and cacahes them for future lookup
     *
     * @param types
     */
    public void writersFor(GenericType... types) {
        for (GenericType type : types) writerFor(type);

    }

    /**
     * Create ObjectWriter for specified type
     *
     *
     * @param clz
     * @return
     */
    public ObjectWriter writerFor(Class clz) {
        return writerFor(clz, clz);
    }

    /**
     * Create ObjectWriter for specified type
     *
     * @param type
     * @return
     */
    public ObjectWriter writerFor(GenericType type) {
        return writerFor(type.getRawType(), type.getType());
    }

    /**
     * Create ObjectWriter for specified type
     *
     * @param clz
     * @param genericType
     * @return
     */
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

    /**
     * Write target object to OutputStream.  Uses UTF-8 encoding.
     *
     * @param type
     * @param genericType
     * @param target
     * @param stream
     */
    public void writeStream(Class type, Type genericType, Object target, OutputStream stream) {
        ObjectWriter objectWriter = writerFor(type, genericType);
        OutputStreamByteWriter writer = new OutputStreamByteWriter(stream);
        JsonWriter jsonWriter = new JsonByteWriter(writer);
        objectWriter.write(jsonWriter, target);
    }

    /**
     * Write target object to OutputStream.  Uses UTF-8 encoding.
     *
     * @param type
     * @param target
     * @param stream
     */
    public void writeStream(Class type, Object target, OutputStream stream) {
        writeStream(type, type, target, stream);
    }

    /**
     * Write target object to OutputStream.  Uses UTF-8 encoding.
     *
     * @param type
     * @param target
     * @param stream
     */
    public void writeStream(GenericType type, Object target, OutputStream stream) {
        writeStream(type.getRawType(), type.getType(), target, stream);
    }

    /**
     * Write target object to OutputStream.  Uses UTF-8 encoding.
     * Note, because of type erasure, any generic type information will not be used to generate
     * a parser for target object.
     *
     * @param target
     * @param stream
     */
    public void writeStream(Object target, OutputStream stream) {
        writeStream(target.getClass(), target.getClass(), stream);
    }

    /**
     * Serialize target object to a byte array. Uses UTF-8 encoding.
     *
     * @param type
     * @param genericType
     * @param target
     * @return
     */
    public byte[] writeBytes(Class type, Type genericType, Object target) {
        ObjectWriter objectWriter = writerFor(type, genericType);
        ByteArrayByteWriter writer = new ByteArrayByteWriter();
        JsonByteWriter jsonWriter = new JsonByteWriter(writer);
        objectWriter.write(jsonWriter, target);
        return writer.getBytes();
    }

    /**
     * Serialize target object to a byte array. Uses UTF-8 encoding.
     *
     * @param type
     * @param target
     * @return
     */
    public byte[] writeBytes(Class type, Object target) {
        return writeBytes(type, type, target);
    }

    /**
     * Serialize target object to a byte array. Uses UTF-8 encoding.
     *
     * @param type
     * @param target
     * @return
     */
    public byte[] writeBytes(GenericType type, Object target) {
        return writeBytes(type.getRawType(), type.getType(), target);
    }

    /**
     * Serialize target object to a byte array. Uses UTF-8 encoding.
     *
     * Note, because of type erasure, any generic type information will not be used to generate
     * a parser for target object.
     *
     * @param target
     * @return
     */
    public byte[] writeBytes(Object target) {
        return writeBytes(target.getClass(), target);
    }

    /**
     * Serialize target to a json string. Uses UTF-8 encoding.
     *
     * @param type
     * @param genericType
     * @param target
     * @return
     */
    public String writeString(Class type, Type genericType, Object target) {
        try {
            return new String(writeBytes(type, genericType, target), JsonByteWriter.UTF8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Serialize target to a json string. Uses UTF-8 encoding.
     *
     * @param type
     * @param target
     * @return
     */
    public String writeString(Class type, Object target) {
        return writeString(type, type, target);
    }

    /**
     * Serialize target to a json string. Uses UTF-8 encoding.
     *
     * @param type
     * @param target
     * @return
     */
    public String writeString(GenericType type, Object target) {
        return writeString(type.getRawType(), type.getType(), target);
    }

    /**
     * Serialize target to a json string. Uses UTF-8 encoding.
     *
     * @param target
     * @return
     */
    public String writeString(Object target) {
        return writeString(target.getClass(), target);
    }

    private String generateSerializers(Class clz, Type genericType) {
        String key = key(clz, genericType);
        if (generatedSerializers.containsKey(key)) return generatedSerializers.get(key);
        Serializer.Builder builder = Serializer.create(clz, genericType).output(cl).generate();
        generatedSerializers.put(key, builder.className());
        for (Map.Entry<Type, Class> entry : builder.referenced().entrySet()) {
            String refKey = key(entry.getValue(), entry.getKey());
            if (generatedSerializers.containsKey(refKey)) continue;
            generateSerializers(entry.getValue(), entry.getKey());
        }
        return builder.className();
    }

    private String key(Class clz, Type genericType) {
        return genericType == null ? Types.typename(clz) : Types.typename(genericType);
    }
}
