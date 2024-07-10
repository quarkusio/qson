package io.quarkus.qson.resteasy.reactive;

import io.quarkus.qson.resteasy.QsonResteasyUtil;
import io.quarkus.qson.runtime.QuarkusQsonRegistry;
import io.quarkus.qson.writer.ByteArrayJsonWriter;
import io.quarkus.qson.writer.QsonObjectWriter;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class QsonMessageBodyWriter implements ServerMessageBodyWriter<Object> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target, MediaType mediaType) {
        return QsonResteasyUtil.isWriteable(genericType);
    }

    @Override
    public void writeResponse(Object o, Type genericType, ServerRequestContext context) throws WebApplicationException, IOException {
        OutputStream outputStream = context.getOrCreateOutputStream();
        writeResponse(o, genericType, outputStream);
        outputStream.close();
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return QsonResteasyUtil.isWriteable(genericType);
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        writeResponse(o, genericType, entityStream);
    }

    private void writeResponse(Object o, Type genericType, OutputStream outputStream) throws IOException {
        QsonObjectWriter objectWriter = QuarkusQsonRegistry.getWriter(genericType);
        if (objectWriter == null) {
            throw new IOException("Failed to find QSON writer for: " + genericType.getTypeName());
        }
        ByteArrayJsonWriter jsonWriter = new ByteArrayJsonWriter();
        objectWriter.write(jsonWriter, o);
        outputStream.write(jsonWriter.getBuffer(), 0, jsonWriter.size());
        outputStream.close();
    }
}
