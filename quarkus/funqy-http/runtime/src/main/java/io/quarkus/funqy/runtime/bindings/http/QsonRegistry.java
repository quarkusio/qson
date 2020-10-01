package io.quarkus.funqy.runtime.bindings.http;

import io.quarkus.runtime.annotations.Recorder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Recorder
public class QsonRegistry {

    public static Map<String, Class> READERS = new ConcurrentHashMap<>();
    public static Map<String, Class> WRITERS = new ConcurrentHashMap<>();

    public void registerReader(String name, Class clazz) {
        READERS.put(name, clazz);
    }
    public void registerWriter(String name, Class clazz) {
        WRITERS.put(name, clazz);
    }
}
