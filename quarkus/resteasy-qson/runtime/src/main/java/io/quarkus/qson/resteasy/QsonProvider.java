package io.quarkus.qson.resteasy;

import io.quarkus.qson.Types;
import io.quarkus.qson.desserializer.ByteArrayParserContext;
import io.quarkus.qson.desserializer.JsonParser;
import io.quarkus.qson.runtime.RegistryRecorder;
import io.quarkus.qson.serializer.JsonByteWriter;
import io.quarkus.qson.serializer.JsonWriter;
import io.quarkus.qson.serializer.ObjectWriter;
import io.quarkus.qson.serializer.OutputStreamByteWriter;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@Consumes({"application/json", "application/*+json", "text/json"})
@Produces({"application/json", "application/*+json", "text/json"})
public class QsonProvider implements MessageBodyReader, MessageBodyWriter {
    @Override
    public boolean isReadable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return RegistryRecorder.getParser(Types.typename(genericType)) != null;
    }

    @Override
    public Object readFrom(Class type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        JsonParser parser = RegistryRecorder.parsers.get(Types.typename(genericType));
        if (parser == null) {
            throw new IOException("Failed to find QSON parser for: " + genericType.getTypeName());
        }
        ByteArrayParserContext ctx = new ByteArrayParserContext(parser.startState());
        final byte[] bytes = new byte[8192];
        int read ;
        do {
            read = entityStream.read(bytes);
            ctx.parse(bytes, read);
        } while (read >= 0);

        return ctx.target();
    }

    @Override
    public boolean isWriteable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return RegistryRecorder.getWriter(Types.typename(genericType)) != null;
    }

    @Override
    public void writeTo(Object o, Class type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        ObjectWriter objectWriter = RegistryRecorder.writers.get(Types.typename(genericType));
        if (objectWriter == null) {
            throw new IOException("Failed to find QSON writer for: " + genericType.getTypeName());
        }
        OutputStreamByteWriter writer = new OutputStreamByteWriter(entityStream);
        JsonWriter jsonWriter = new JsonByteWriter(writer);
        objectWriter.write(jsonWriter, o);
    }
}
