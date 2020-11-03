package io.quarkus.qson.runtime;

import io.quarkus.qson.desserializer.JsonParser;
import io.quarkus.qson.serializer.ObjectWriter;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class QuarkusQsonRegistry {

    /**
     * Use a string lookup for the parser you are interested in.
     * Keys are generated from Type.getTypeName()
     *
     * @param key Type.getTypeName()
     * @return
     */
    public JsonParser getParser(String key) {
        return RegistryRecorder.parsers.get(key);
    }

    public ObjectWriter getWriter(String key) {
        return RegistryRecorder.writers.get(key);
    }
}
