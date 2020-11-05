package io.quarkus.qson.runtime;

import io.quarkus.qson.desserializer.JsonParser;
import io.quarkus.qson.serializer.ObjectWriter;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Recorder
public class RegistryRecorder {
    public static final Map<String, JsonParser> parsers = new ConcurrentHashMap<>();
    public static final Map<String, ObjectWriter> writers = new ConcurrentHashMap<>();

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
}
