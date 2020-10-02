package io.quarkus.funqy.runtime.bindings.http;

import io.quarkus.qson.desserializer.ObjectParser;
import io.quarkus.qson.serializer.ObjectWriter;
import io.quarkus.runtime.annotations.Recorder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Recorder
public class QsonRegistry {

    public static Map<String, Class<ObjectParser>> READERS = new ConcurrentHashMap<>();
    public static Map<String, Class<ObjectWriter>> WRITERS = new ConcurrentHashMap<>();

    public void registerReader(String name, Class clazz) {
        READERS.put(name, clazz);
    }
    public void registerWriter(String name, Class clazz) {
        WRITERS.put(name, clazz);
    }
}
