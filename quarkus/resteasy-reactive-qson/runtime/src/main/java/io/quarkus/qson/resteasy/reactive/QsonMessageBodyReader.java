package io.quarkus.qson.resteasy.reactive;

import io.quarkus.qson.resteasy.QsonResteasyUtil;
import org.jboss.resteasy.reactive.common.providers.serialisers.AbstractJsonMessageBodyReader;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class QsonMessageBodyReader extends AbstractJsonMessageBodyReader {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return QsonResteasyUtil.isReadable(genericType) && super.isReadable(type, genericType, annotations, mediaType);
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        return QsonResteasyUtil.read(genericType, entityStream);
    }
}
