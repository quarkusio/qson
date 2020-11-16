package io.quarkus.qson.runtime;

import io.quarkus.qson.util.Types;
import io.quarkus.qson.deserializer.JsonParser;
import io.quarkus.qson.serializer.ObjectWriter;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Recorder
public class QsonRegistry {
    static final Map<String, JsonParser> parsers = new ConcurrentHashMap<>();
    static final Map<String, ObjectWriter> writers = new ConcurrentHashMap<>();

    static final Map<Type, JsonParser> typeParsers = new ConcurrentHashMap<>();
    static final Map<Type, ObjectWriter> typeWriters = new ConcurrentHashMap<>();

    public void clear() {
        parsers.clear();
        writers.clear();
        typeParsers.clear();
        typeWriters.clear();
    }

    public void registerParser(String key, RuntimeValue<JsonParser> parser) {
        parsers.put(key, parser.getValue());
    }
    public void registerWriter(String key, RuntimeValue<ObjectWriter> writer) {
        writers.put(key, writer.getValue());
    }

    public static JsonParser getParser(String key) {
        return parsers.get(key);
    }

    public static ObjectWriter getWriter(String key) {
        return writers.get(key);
    }
    public static JsonParser getParser(Type key) {
        JsonParser parser = typeParsers.get(key);
        if (parser != null) return parser;
        parser = parsers.get(Types.typename(key));
        if (parser == null) return null;
        typeParsers.putIfAbsent(key, parser);
        return parser;
    }

    public static ObjectWriter getWriter(Type key) {
        ObjectWriter writer = typeWriters.get(key);
        if (writer != null) return writer;
        writer = writers.get(Types.typename(key));
        if (writer == null) return null;
        typeWriters.putIfAbsent(key, writer);
        return writer;
    }
}
