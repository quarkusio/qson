package io.quarkus.qson.resteasy;

import io.quarkus.qson.deserializer.ByteArrayParserContext;
import io.quarkus.qson.deserializer.QsonParser;
import io.quarkus.qson.runtime.QuarkusQsonRegistry;
import io.quarkus.qson.serializer.QsonObjectWriter;
import io.quarkus.qson.serializer.OutputStreamJsonWriter;

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
public class QuarkusQsonProvider implements MessageBodyReader, MessageBodyWriter {
    @Override
    public boolean isReadable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return QuarkusQsonRegistry.getParser(genericType) != null;
    }

    @Override
    public Object readFrom(Class type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        QsonParser parser = QuarkusQsonRegistry.getParser(genericType);
        if (parser == null) {
            throw new IOException("Failed to find QSON parser for: " + genericType.getTypeName());
        }
        ByteArrayParserContext ctx = new ByteArrayParserContext(parser);
        return ctx.finish(entityStream);
    }

    @Override
    public boolean isWriteable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return QuarkusQsonRegistry.getWriter(genericType) != null;
    }

    @Override
    public void writeTo(Object o, Class type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        QsonObjectWriter objectWriter = QuarkusQsonRegistry.getWriter(genericType);
        if (objectWriter == null) {
            throw new IOException("Failed to find QSON writer for: " + genericType.getTypeName());
        }
        OutputStreamJsonWriter jsonWriter = new OutputStreamJsonWriter(entityStream);
        objectWriter.write(jsonWriter, o);
    }
}
