package io.quarkus.qson.generator;

import io.quarkus.gizmo.ClassOutput;

import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class MemoryClassOutput implements ClassOutput {
    Map<String, byte[]> bytecode = new HashMap<>();
    Map<String, StringWriter> sources = new HashMap<>();



    public Map<String, byte[]> getBytecode() {
        return bytecode;
    }

    public Map<String, StringWriter> getSources() {
        return sources;
    }

    @Override
    public void write(String name, byte[] data) {
        bytecode.put(name, data);
    }

    @Override
    public Writer getSourceWriter(String className) {
        StringWriter writer = new StringWriter();
        sources.put(className, writer);
        return writer;
    }
}
