package io.quarkus.funqy.deployment.bindings.http;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.funqy.Context;
import io.quarkus.funqy.runtime.InputValueInjector;
import io.quarkus.funqy.runtime.ParameterInjector;
import io.quarkus.funqy.runtime.ValueInjector;
import io.quarkus.funqy.runtime.bindings.http.QsonRegistry;
import io.quarkus.qson.generator.Deserializer;
import io.quarkus.qson.generator.Serializer;
import io.quarkus.qson.generator.Types;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;


import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.funqy.deployment.FunctionBuildItem;
import io.quarkus.funqy.deployment.FunctionInitializedBuildItem;
import io.quarkus.funqy.runtime.bindings.http.FunqyHttpBindingRecorder;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class FunqyHttpBuildStep {
    private static final Logger log = Logger.getLogger(FunqyHttpBuildStep.class);
    public static final String FUNQY_HTTP_FEATURE = "funqy-qson";

    public final static class GeneratedReader extends MultiBuildItem {
        private String name;
        private String className;

        public GeneratedReader(String name, String className) {
            this.name = name;
            this.className = className;
        }

        public String getName() {
            return name;
        }

        public String getClassName() {
            return className;
        }
    }

    public final static class GeneratedWriter extends MultiBuildItem {
        private String name;
        private String className;

        public GeneratedWriter(String name, String className) {
            this.name = name;
            this.className = className;
        }

        public String getName() {
            return name;
        }

        public String getClassName() {
            return className;
        }
    }

    @BuildStep
    public void generateJsonClasses(BuildProducer<GeneratedClassBuildItem> additionalClasses,
                                    BuildProducer<GeneratedReader> generatedReaders,
                                    BuildProducer<GeneratedWriter> generatedWriters,
                                    Optional<FunctionInitializedBuildItem> hasFunctions,
                                    List<FunctionBuildItem> functions
                                    ) {
        if (!hasFunctions.isPresent() || hasFunctions.get() == null)
            return;
        GeneratedClassGizmoAdaptor adaptor = new GeneratedClassGizmoAdaptor(additionalClasses);
        for (FunctionBuildItem function : functions) {
            Class functionClass = null;
            try {
                functionClass = Thread.currentThread().getContextClassLoader().loadClass(function.getClassName());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to load function class: " + function.getClassName());
            }
            // todo, more efficient way of doing this after Qson converts to ClassInfo
            Method method = null;
            for (Method m : functionClass.getMethods()) {
                if (m.getName().equals(function.getMethodName())) {
                    method = m;
                    break;
                }
            }
            if (method.getParameterCount() > 0) {
                for (int i = 0; i < method.getParameterCount(); i++) {
                    Annotation[] annotations = method.getParameterAnnotations()[i];
                    if (isContext(annotations)) {
                        continue;
                    }
                    Type type = method.getGenericParameterTypes()[i];
                    Class clz = method.getParameterTypes()[i];
                    Deserializer.Builder builder = Deserializer.create(clz).generic(type).output(adaptor).generate();
                    generatedReaders.produce(new GeneratedReader(builder.keyName(), builder.className()));
                }
            }
            Class<?> returnType = method.getReturnType();
            if (returnType != null && !void.class.equals(returnType)) {
                Type type = method.getGenericReturnType();
                if (Uni.class.equals(returnType)) {
                    ParameterizedType pt = (ParameterizedType)type;
                    returnType = Types.getRawType(pt.getActualTypeArguments()[0]);
                    type = pt.getActualTypeArguments()[0];
                }
                if (!void.class.equals(returnType) && !Void.class.equals(returnType)) {
                    Serializer.Builder builder = Serializer.create(returnType).generic(type).output(adaptor).generate();
                    generatedWriters.produce(new GeneratedWriter(returnType.getName(), builder.className()));
                }
            }
        }
    }

    static boolean isContext(Annotation[] annotations) {
        for (Annotation a : annotations) {
            if (a.annotationType().equals(Context.class)) return true;
        }
        return false;
    }

    @BuildStep()
    @Record(STATIC_INIT)
    public void staticInit(FunqyHttpBindingRecorder binding,
            BeanContainerBuildItem beanContainer, // dependency
            Optional<FunctionInitializedBuildItem> hasFunctions,
            QsonRegistry registry,
            List<GeneratedReader> readers,
            List<GeneratedWriter> writers,
                           RecorderContext context,
            HttpBuildTimeConfig httpConfig) throws Exception {
        if (!hasFunctions.isPresent() || hasFunctions.get() == null)
            return;
        if (readers != null) {
            for (GeneratedReader reader : readers) {
                registry.registerReader(reader.getName(), context.newInstance(reader.getClassName()));
            }
        }
        if (writers != null) {
            for (GeneratedWriter writer : writers) {
                registry.registerWriter(writer.getName(), context.newInstance(writer.getClassName()));
            }
        }
        // The context path + the resources path
        String rootPath = httpConfig.rootPath;
        binding.init();
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void boot(ShutdownContextBuildItem shutdown,
            FunqyHttpBindingRecorder binding,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<RouteBuildItem> routes,
            CoreVertxBuildItem vertx,
            Optional<FunctionInitializedBuildItem> hasFunctions,
            List<FunctionBuildItem> functions,
            BeanContainerBuildItem beanContainer,
            HttpBuildTimeConfig httpConfig,
            ExecutorBuildItem executorBuildItem) throws Exception {

        if (!hasFunctions.isPresent() || hasFunctions.get() == null)
            return;
        feature.produce(new FeatureBuildItem(FUNQY_HTTP_FEATURE));

        String rootPath = httpConfig.rootPath;
        Handler<RoutingContext> handler = binding.start(rootPath,
                vertx.getVertx(),
                shutdown,
                beanContainer.getValue(),
                executorBuildItem.getExecutorProxy());

        for (FunctionBuildItem function : functions) {
            if (rootPath == null)
                rootPath = "/";
            else if (!rootPath.endsWith("/"))
                rootPath += "/";
            String name = function.getFunctionName() == null ? function.getMethodName() : function.getFunctionName();
            //String path = rootPath + name;
            String path = "/" + name;
            routes.produce(new RouteBuildItem(path, handler, false));
        }
    }
}
