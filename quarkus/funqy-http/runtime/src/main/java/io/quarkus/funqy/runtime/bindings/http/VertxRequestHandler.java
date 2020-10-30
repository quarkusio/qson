package io.quarkus.funqy.runtime.bindings.http;

import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.funqy.runtime.FunctionInvoker;
import io.quarkus.funqy.runtime.FunctionRecorder;
import io.quarkus.funqy.runtime.RequestContextImpl;
import io.quarkus.funqy.runtime.query.QueryReader;
import io.quarkus.qson.desserializer.ByteArrayParserContext;
import io.quarkus.qson.desserializer.JsonParser;
import io.quarkus.qson.desserializer.VertxBufferParserContext;
import io.quarkus.qson.serializer.ByteArrayByteWriter;
import io.quarkus.qson.serializer.JsonByteWriter;
import io.quarkus.qson.serializer.ObjectWriter;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import org.jboss.logging.Logger;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class VertxRequestHandler implements Handler<RoutingContext> {
    private static final Logger log = Logger.getLogger("io.quarkus.funqy");

    protected final Vertx vertx;
    protected final String rootPath;
    protected final BeanContainer beanContainer;
    protected final CurrentIdentityAssociation association;
    protected final CurrentVertxRequest currentVertxRequest;
    protected final Executor executor;

    public VertxRequestHandler(Vertx vertx,
            BeanContainer beanContainer,
            String rootPath,
            Executor executor) {
        this.vertx = vertx;
        this.beanContainer = beanContainer;
        // make sure rootPath ends with "/" for easy parsing
        if (rootPath == null) {
            this.rootPath = "/";
        } else if (!rootPath.endsWith("/")) {
            this.rootPath = rootPath + "/";
        } else {
            this.rootPath = rootPath;
        }

        this.executor = executor;
        Instance<CurrentIdentityAssociation> association = CDI.current().select(CurrentIdentityAssociation.class);
        this.association = association.isResolvable() ? association.get() : null;
        currentVertxRequest = CDI.current().select(CurrentVertxRequest.class).get();
    }

    @Override
    public void handle(RoutingContext routingContext) {
        String path = routingContext.request().path();
        if (path == null) {
            routingContext.fail(404);
            return;
        }
        // expects rootPath to end with '/'
        if (!path.startsWith(rootPath)) {
            routingContext.fail(404);
            return;
        }

        path = path.substring(rootPath.length());

        FunctionInvoker invoker = FunctionRecorder.registry.matchInvoker(path);

        if (invoker == null) {
            routingContext.fail(404);
            return;
        }

        if (routingContext.request().method() == HttpMethod.GET) {
            Object input = null;
            if (invoker.hasInput()) {
                QueryReader reader = (QueryReader) invoker.getBindingContext().get(QueryReader.class.getName());
                try {
                    input = reader.readValue(routingContext.request().params().iterator());
                } catch (Exception e) {
                    log.error("Failed to unmarshal input", e);
                    routingContext.fail(400);
                    return;
                }
            }
            Object finalInput = input;
            executor.execute(() -> {
                dispatch(routingContext, invoker, finalInput);
            });
        } else if (routingContext.request().method() == HttpMethod.POST) {
            postBytes(routingContext, invoker);
        } else {
            routingContext.fail(405);
            log.error("Must be POST or GET for: " + invoker.getName());
        }
    }

    private void post(RoutingContext routingContext, FunctionInvoker invoker) {
        routingContext.request().bodyHandler(buff -> {
            Object input = null;
            if (buff.length() > 0) {
                JsonParser reader = (JsonParser) invoker.getBindingContext().get(JsonParser.class.getName());
                try {
                    VertxBufferParserContext ctx = new VertxBufferParserContext(reader.startState());
                    // todo handle integer case where parse returns false as it can't know its the end
                    ctx.parse(buff);
                    input = reader.getTarget(ctx);
                } catch (Exception e) {
                    log.error("Failed to unmarshal input", e);
                    routingContext.fail(400);
                    return;
                }
            }
            Object finalInput = input;
            executor.execute(() -> {
                dispatch(routingContext, invoker, finalInput);
            });
        });
    }

    private void postBytes(RoutingContext routingContext, FunctionInvoker invoker) {
        routingContext.request().bodyHandler(buff -> {
            Object input = null;
            if (buff.length() > 0) {
                JsonParser reader = (JsonParser) invoker.getBindingContext().get(JsonParser.class.getName());
                try {
                    byte[] bytes = buff.getBytes();
                    ByteArrayParserContext ctx = new ByteArrayParserContext(reader.startState());
                    // todo handle integer case where parse returns false as it can't know its the end
                    ctx.parse(bytes);
                    input = reader.getTarget(ctx);
                } catch (Exception e) {
                    log.error("Failed to unmarshal input", e);
                    routingContext.fail(400);
                    return;
                }
            }
            Object finalInput = input;
            executor.execute(() -> {
                dispatch(routingContext, invoker, finalInput);
            });
        });
    }

    private void dispatch(RoutingContext routingContext, FunctionInvoker invoker, Object input) {
        ManagedContext requestContext = beanContainer.requestContext();
        requestContext.activate();
        if (association != null) {
            ((Consumer<Uni<SecurityIdentity>>) association).accept(QuarkusHttpUser.getSecurityIdentity(routingContext, null));
        }
        currentVertxRequest.setCurrent(routingContext);
        try {
            FunqyRequestImpl funqyRequest = new FunqyRequestImpl(new RequestContextImpl(), input);
            FunqyResponseImpl funqyResponse = new FunqyResponseImpl();
            invoker.invoke(funqyRequest, funqyResponse);

            funqyResponse.getOutput().emitOn(executor).subscribe().with(
                    o -> {
                        if (invoker.hasOutput()) {
                            routingContext.response().setStatusCode(200);
                            routingContext.response().putHeader("Content-Type", "application/json");
                            ObjectWriter writer = (ObjectWriter) invoker.getBindingContext().get(ObjectWriter.class.getName());
                            try {
                                ByteArrayByteWriter byteWriter = new ByteArrayByteWriter();
                                JsonByteWriter jsonWriter = new JsonByteWriter(byteWriter);
                                writer.write(jsonWriter, o);

                                routingContext.response().end(Buffer.buffer(byteWriter.getBytes()));
                            } catch (Exception e) {
                                log.error("Failed to marshal", e);
                                routingContext.fail(400);
                            }
                        } else {
                            routingContext.response().setStatusCode(204);
                            routingContext.response().end();
                        }
                    },
                    t -> routingContext.fail(t));

        } catch (Exception e) {
            routingContext.fail(e);
        } finally {
            if (requestContext.isActive()) {
                requestContext.terminate();
            }
        }
    }
}
