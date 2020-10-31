package io.quarkus.funqy.runtime.bindings.http;

import io.quarkus.qson.desserializer.JsonParser;
import io.quarkus.qson.desserializer.ObjectParser;
import io.quarkus.qson.serializer.ObjectWriter;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Recorder
public class QsonRegistry {

    public static Map<String, JsonParser> READERS = new ConcurrentHashMap<>();
    public static Map<String, ObjectWriter> WRITERS = new ConcurrentHashMap<>();

    public void registerReader(String name, RuntimeValue<JsonParser> parser) {
        READERS.put(name, parser.getValue());
    }
    public void registerWriter(String name, RuntimeValue<ObjectWriter> writer) {
        WRITERS.put(name, writer.getValue());
    }
}
