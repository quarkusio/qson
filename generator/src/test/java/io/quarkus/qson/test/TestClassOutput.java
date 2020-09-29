package io.quarkus.qson.test;
import io.quarkus.gizmo.ClassOutput;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;

public class TestClassOutput implements ClassOutput {

    @Override
    public void write(String name, byte[] data) {
        try {
            File dir = new File("target/test-classes/", name.substring(0, name.lastIndexOf("/")));
            dir.mkdirs();
            File output = new File("target/test-classes/", name + ".class");
            Files.write(output.toPath(), data);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot dump the class: " + name, e);
        }
    }

    public Writer getSourceWriter(final String className) {
        File dir = new File("target/generated-test-sources/gizmo/", className.substring(0, className.lastIndexOf('/')));
        dir.mkdirs();
        File output = new File("target/generated-test-sources/gizmo/", className + ".zig");
        try {
            return Files.newBufferedWriter(output.toPath());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write .zig file for " + className, e);
        }
    }

}
