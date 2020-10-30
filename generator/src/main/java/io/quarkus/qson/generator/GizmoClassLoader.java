package io.quarkus.qson.generator;

import io.quarkus.gizmo.ClassOutput;

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

class GizmoClassLoader extends ClassLoader implements ClassOutput {
    private final Map<String, byte[]> appClasses = new HashMap();
    private final boolean dump;

    public GizmoClassLoader(ClassLoader parent) {
        super(parent);
        dump = false;
    }

    public GizmoClassLoader(ClassLoader parent, boolean dump) {
        super(parent);
        this.dump = dump;
    }

    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> ex = this.findLoadedClass(name);
        if (ex != null) {
            return ex;
        } else {
            return this.appClasses.containsKey(name) ? this.findClass(name) : super.loadClass(name, resolve);
        }
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = (byte[])this.appClasses.get(name);
        if (bytes == null) {
            throw new ClassNotFoundException();
        } else {
            return this.defineClass(name, bytes, 0, bytes.length);
        }
    }

    public void write(String name, byte[] data) {
         this.appClasses.put(name.replace('/', '.'), data);
    }

    public Writer getSourceWriter(String className) {
        return Writer.nullWriter();
    }
}