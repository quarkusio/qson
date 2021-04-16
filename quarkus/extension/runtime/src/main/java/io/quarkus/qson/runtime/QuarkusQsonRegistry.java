package io.quarkus.qson.runtime;

import io.quarkus.qson.util.Types;
import io.quarkus.qson.parser.QsonParser;
import io.quarkus.qson.writer.QsonObjectWriter;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Recorder
public class QuarkusQsonRegistry {
    static final Map<String, QsonParser> parsers = new ConcurrentHashMap<>();
    static final Map<String, QsonObjectWriter> writers = new ConcurrentHashMap<>();

    static final Map<Type, QsonParser> typeParsers = new ConcurrentHashMap<>();
    static final Map<Type, QsonObjectWriter> typeWriters = new ConcurrentHashMap<>();

    public void clear() {
        parsers.clear();
        writers.clear();
        typeParsers.clear();
        typeWriters.clear();
    }

    public void registerParser(String key, RuntimeValue<QsonParser> parser) {
        parsers.put(key, parser.getValue());
    }
    public void registerWriter(String key, RuntimeValue<QsonObjectWriter> writer) {
        writers.put(key, writer.getValue());
    }

    public static QsonParser getParser(String key) {
        return parsers.get(key);
    }

    public static QsonObjectWriter getWriter(String key) {
        return writers.get(key);
    }
    public static QsonParser getParser(Type key) {
        QsonParser parser = typeParsers.get(key);
        if (parser != null) return parser;
        parser = parsers.get(Types.typename(key));
        if (parser == null) return null;
        typeParsers.putIfAbsent(key, parser);
        return parser;
    }

    public static QsonObjectWriter getWriter(Type key) {
        QsonObjectWriter writer = typeWriters.get(key);
        if (writer != null) return writer;
        writer = writers.get(Types.typename(key));
        if (writer == null) return null;
        typeWriters.putIfAbsent(key, writer);
        return writer;
    }
}
