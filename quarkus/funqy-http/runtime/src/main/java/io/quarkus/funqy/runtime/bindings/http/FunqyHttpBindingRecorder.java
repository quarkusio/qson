package io.quarkus.funqy.runtime.bindings.http;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.funqy.runtime.FunctionConstructor;
import io.quarkus.funqy.runtime.FunctionInvoker;
import io.quarkus.funqy.runtime.FunctionRecorder;
import io.quarkus.funqy.runtime.query.QueryObjectMapper;
import io.quarkus.funqy.runtime.query.QueryReader;
import io.quarkus.qson.desserializer.JsonParser;
import io.quarkus.qson.serializer.ObjectWriter;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

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
                    JsonParser reader = QsonRegistry.READERS.get(invoker.getInputGenericType().getTypeName()).newInstance();
                    QueryReader queryReader = queryMapper.readerFor(invoker.getInputType(), invoker.getInputGenericType());
                    invoker.getBindingContext().put(JsonParser.class.getName(), reader);
                    invoker.getBindingContext().put(QueryReader.class.getName(), queryReader);
                }
                if (invoker.hasOutput()) {
                    String typeName = invoker.getOutputType().getTypeName();
                    Class<ObjectWriter> objectWriterClass = QsonRegistry.WRITERS.get(typeName);
                    if (objectWriterClass == null) {
                        throw new RuntimeException("Failed to find writer for: " + invoker.getName());
                    }
                    ObjectWriter writer = objectWriterClass.newInstance();
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
