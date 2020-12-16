package io.quarkus.qson.resteasy;

import io.quarkus.qson.deserializer.ByteArrayParserContext;
import io.quarkus.qson.deserializer.QsonParser;
import io.quarkus.qson.runtime.QuarkusQsonRegistry;
import io.quarkus.qson.serializer.OutputStreamJsonWriter;
import io.quarkus.qson.serializer.QsonObjectWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

public final class QsonResteasyUtil {

    private QsonResteasyUtil() {
    }

    public static boolean isReadable(Type genericType) {
        return QuarkusQsonRegistry.getParser(genericType) != null;
    }

    public static Object read(Type genericType, InputStream entityStream) throws IOException {
        QsonParser parser = QuarkusQsonRegistry.getParser(genericType);
        if (parser == null) {
            throw new IOException("Failed to find QSON parser for: " + genericType.getTypeName());
        }
        ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
        return ctx.finish(entityStream);
    }

    public static boolean isWriteable(Type genericType) {
        return QuarkusQsonRegistry.getWriter(genericType) != null;
    }

    public static void write(Object o, Type genericType, OutputStream entityStream) throws IOException {
        QsonObjectWriter objectWriter = QuarkusQsonRegistry.getWriter(genericType);
        if (objectWriter == null) {
            throw new IOException("Failed to find QSON writer for: " + genericType.getTypeName());
        }
        OutputStreamJsonWriter jsonWriter = new OutputStreamJsonWriter(entityStream);
        objectWriter.write(jsonWriter, o);
    }
}
