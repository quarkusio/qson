package io.quarkus.qson.resteasy.reactive;

import io.quarkus.qson.resteasy.QsonResteasyUtil;
import org.jboss.resteasy.reactive.server.providers.serialisers.json.AbstractJsonMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class QsonMessageBodyReader extends AbstractJsonMessageBodyReader {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo lazyMethod, MediaType mediaType) {
        return QsonResteasyUtil.isReadable(genericType) && super.isReadable(type, genericType, lazyMethod, mediaType);
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, MediaType mediaType, ServerRequestContext context) throws WebApplicationException, IOException {
        return QsonResteasyUtil.read(genericType, context.getInputStream());
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return QsonResteasyUtil.isReadable(genericType) && super.isReadable(type, genericType, annotations, mediaType);
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        return QsonResteasyUtil.read(genericType, entityStream);
    }
}
