package org.sample;

import io.quarkus.gizmo.ClassOutput;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.HashMap;
import java.util.Map;

public class TestClassLoader extends ClassLoader implements ClassOutput {
    private final Map<String, byte[]> appClasses = new HashMap();

    public TestClassLoader(ClassLoader parent) {
        super(parent);
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
        if (System.getProperty("dumpClass") != null) {
            try {
                File dir = new File("target/test-classes/", name.substring(0, name.lastIndexOf("/")));
                dir.mkdirs();
                File output = new File("target/test-classes/", name + ".class");
                Files.write(output.toPath(), data, new OpenOption[0]);
            } catch (IOException var5) {
                throw new IllegalStateException("Cannot dump the class: " + name, var5);
            }
        }

        this.appClasses.put(name.replace('/', '.'), data);
    }

    public Writer getSourceWriter(String className) {
        File dir = new File("target/generated-test-sources/gizmo/", className.substring(0, className.lastIndexOf(47)));
        dir.mkdirs();
        File output = new File("target/generated-test-sources/gizmo/", className + ".zig");

        try {
            return Files.newBufferedWriter(output.toPath());
        } catch (IOException var5) {
            throw new IllegalStateException("Cannot write .zig file for " + className, var5);
        }
    }
}
