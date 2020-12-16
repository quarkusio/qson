package io.quarkus.qson.resteasy.reactive;

import io.quarkus.qson.resteasy.QsonResteasyUtil;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
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
        QsonResteasyUtil.write(o, genericType, context.getOrCreateOutputStream());
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return QsonResteasyUtil.isWriteable(genericType);
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        QsonResteasyUtil.write(o, genericType, entityStream);
    }
}
