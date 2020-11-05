package io.quarkus.funqy.runtime.bindings.http;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.funqy.runtime.FunctionConstructor;
import io.quarkus.funqy.runtime.FunctionInvoker;
import io.quarkus.funqy.runtime.FunctionRecorder;
import io.quarkus.funqy.runtime.query.QueryObjectMapper;
import io.quarkus.funqy.runtime.query.QueryReader;
import io.quarkus.qson.Types;
import io.quarkus.qson.desserializer.JsonParser;
import io.quarkus.qson.runtime.RegistryRecorder;
import io.quarkus.qson.serializer.ObjectWriter;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Provides the runtime methods to bootstrap Quarkus Funq
 */
@Recorder
public class FunqyHttpBindingRecorder {
    private static QueryObjectMapper queryMapper;

    public void init() {
        queryMapper = new QueryObjectMapper();
        for (FunctionInvoker invoker : FunctionRecorder.registry.invokers()) {
            try {
                if (invoker.hasInput()) {
                    JsonParser reader = RegistryRecorder.parsers.get(Types.typename(invoker.getInputGenericType()));
                    if (reader == null) {
                        throw new RuntimeException("Unable to find JsonParser for invoker:" + invoker.getName());
                    }
                    QueryReader queryReader = queryMapper.readerFor(invoker.getInputType(), invoker.getInputGenericType());
                    invoker.getBindingContext().put(JsonParser.class.getName(), reader);
                    invoker.getBindingContext().put(QueryReader.class.getName(), queryReader);
                }
                if (invoker.hasOutput()) {
                    Type genericType = invoker.getMethod().getGenericReturnType();
                    if (Uni.class.isAssignableFrom(invoker.getMethod().getReturnType())) {
                        ParameterizedType pt = (ParameterizedType)invoker.getMethod().getGenericReturnType();
                        genericType = pt.getActualTypeArguments()[0];
                    }
                    String typeName = Types.typename(genericType);
                    ObjectWriter writer = RegistryRecorder.writers.get(typeName);
                    if (writer == null) {
                        throw new RuntimeException("Unable to find ObjectWriter for invoker:" + invoker.getName());
                    }
                    invoker.getBindingContext().put(ObjectWriter.class.getName(), writer);
                }
            } catch (Exception e) {
               throw new RuntimeException (e);
            }
        }
    }

    public Handler<RoutingContext> start(String contextPath,
            Supplier<Vertx> vertx,
            ShutdownContext shutdown,
            BeanContainer beanContainer,
            Executor executor) {

        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                FunctionConstructor.CONTAINER = null;
            }
        });
        FunctionConstructor.CONTAINER = beanContainer;

        return new VertxRequestHandler(vertx.get(), beanContainer, contextPath, executor);
    }
}
