package io.quarkus.qson.generator;

import io.quarkus.qson.GenericType;
import io.quarkus.qson.QsonException;
import io.quarkus.qson.parser.QsonParser;
import io.quarkus.qson.util.Types;
import io.quarkus.qson.writer.JsonByteWriter;
import io.quarkus.qson.writer.QsonObjectWriter;

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
public class QsonMapper extends Generator implements QsonGenerator {
    private ConcurrentHashMap<String, QsonParser> deserializers = new ConcurrentHashMap<>();
    private Map<String, String> generatedDeserializers = new HashMap<>();
    private ConcurrentHashMap<String, QsonObjectWriter> serializers = new ConcurrentHashMap<>();
    private Map<String, String> generatedSerializers = new HashMap<>();
    private final GizmoClassLoader cl;

    /**
     * Uses Thread.currentThread().getContextClassLoader() as parent classloader for bytecode generation
     *
     */
    public QsonMapper() {
        this(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Parent classloader for the custom class loader that loads generated parser/writer bytecode
     *
     * @param parent
     */
    public QsonMapper(ClassLoader parent) {
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
    public QsonParser parserFor(GenericType type) {
        return parserFor(type.getType());
    }

    /**
     * Generate parser based on class and generic type parameters.
     *
     * @param genericType
     * @return
     */
    public QsonParser parserFor(Type genericType) {
        String key = key(genericType);
        QsonParser parser = deserializers.get(key);
        if (parser != null) return parser;
        synchronized(deserializers) {
            parser = deserializers.get(key);
            if (parser != null) return parser;
            String className = generateDeserializers(genericType);
            try {
                Class deserializer = cl.loadClass(className);
                parser = (QsonParser) deserializer.newInstance();
                deserializers.put(key, parser);
            } catch (Throwable e) {
                throw new QsonException(e);
            }
        }
        return parser;
    }

    /**
     * Deserialize a complete byte buffer into the specified type.
     *
     * @param fullBuffer
     * @param genericType
     * @param <T>
     * @return
     */
    public <T> T read(byte[] fullBuffer, Type genericType) {
        QsonParser parser = parserFor(genericType);
        return parser.read(fullBuffer);
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
        return (T)read(fullBuffer, type.getType());
    }

    /**
     * Deserialize a complete json string into the specified type
     *
     * @param json
     * @param genericType
     * @param <T>
     * @return
     */
    public <T> T read(String json, Type genericType) {
        return read(json.getBytes(JsonByteWriter.UTF8), genericType);
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
        return (T)read(json, type.getType());
    }

    /**
     * Deserialize the specified type from an InputStream
     *
     * @param is
     * @param genericType
     * @param <T>
     * @return
     * @throws IOException
     */
    public <T> T read(InputStream is, Type genericType) throws IOException {
        QsonParser parser = parserFor(genericType);
        return parser.read(is);
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
        return (T)read(is, type.getType());
    }

    private String generateDeserializers(Type genericType) {
        String key = key(genericType);
        if (generatedDeserializers.containsKey(key)) return generatedDeserializers.get(key);
        ParserGenerator.Builder builder = parser(genericType).output(cl).generate();
        generatedDeserializers.put(key, builder.className());
        for (Type entry : builder.referenced()) {
            String refKey = key(entry);
            if (generatedDeserializers.containsKey(refKey)) continue;
            generateDeserializers(entry);
        }
        return builder.className();
    }

    /**
     * Generates writers for passed in classes and caches them for future lookup
     *
     * @param classes
     */
    public void writersFor(Type... classes) {
        for (Type clz : classes) writerFor(clz);
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
     * @param type
     * @return
     */
    public QsonObjectWriter writerFor(GenericType type) {
        return writerFor(type.getType());
    }

    /**
     * Create ObjectWriter for specified type
     *
     * @param genericType
     * @return
     */
    public QsonObjectWriter writerFor(Type genericType) {
        String key = key(genericType);
        QsonObjectWriter writer = serializers.get(key);
        if (writer != null) return writer;
        synchronized(serializers) {
            writer = serializers.get(key);
            if (writer != null) return writer;
            String className = generateSerializers(genericType);
            try {
                Class serializer = cl.loadClass(className);
                writer = (QsonObjectWriter) serializer.newInstance();
                serializers.put(key, writer);
            } catch (Throwable e) {
                throw new QsonException(e);
            }
        }
        return writer;
    }

    /**
     * Write target object to OutputStream.  Uses UTF-8 encoding.
     *
     * @param genericType
     * @param target
     * @param stream
     */
    public void writeStream(Type genericType, Object target, OutputStream stream) {
        QsonObjectWriter objectWriter = writerFor(genericType);
        objectWriter.writeStream(stream, target);
    }

    /**
     * Write target object to OutputStream.  Uses UTF-8 encoding.
     *
     * @param type
     * @param target
     * @param stream
     */
    public void writeStream(GenericType type, Object target, OutputStream stream) {
        writeStream(type.getType(), target, stream);
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
        writeStream(target.getClass(), target, stream);
    }

    /**
     * Serialize target object to a byte array. Uses UTF-8 encoding.
     *
     * @param genericType
     * @param target
     * @return
     */
    public byte[] writeBytes(Type genericType, Object target) {
        QsonObjectWriter objectWriter = writerFor(genericType);
        return objectWriter.writeBytes(target);
    }

    /**
     * Serialize target object to a byte array. Uses UTF-8 encoding.
     *
     * @param type
     * @param target
     * @return
     */
    public byte[] writeBytes(GenericType type, Object target) {
        return writeBytes(type.getType(), target);
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
     * @param genericType
     * @param target
     * @return
     */
    public String writeString(Type genericType, Object target) {
        QsonObjectWriter objectWriter = writerFor(genericType);
        return objectWriter.writeString(target);
    }

    /**
     * Serialize target to a json string. Uses UTF-8 encoding.
     *
     * @param type
     * @param target
     * @return
     */
    public String writeString(GenericType type, Object target) {
        return writeString(type.getType(), target);
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

    private String generateSerializers(Type genericType) {
        String key = key(genericType);
        if (generatedSerializers.containsKey(key)) return generatedSerializers.get(key);
        WriterGenerator.Builder builder = writer(genericType).output(cl).generate();
        generatedSerializers.put(key, builder.className());
        for (Type entry : builder.referenced()) {
            String refKey = key(entry);
            if (generatedSerializers.containsKey(refKey)) continue;
            generateSerializers(entry);
        }
        return builder.className();
    }

    private String key(Type genericType) {
        return Types.typename(genericType);
    }
}
