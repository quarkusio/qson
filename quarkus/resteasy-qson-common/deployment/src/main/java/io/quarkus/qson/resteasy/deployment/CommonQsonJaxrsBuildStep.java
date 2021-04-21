package io.quarkus.qson.resteasy.deployment;

import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.qson.deployment.QsonGeneratorBuildItem;
import io.quarkus.qson.deployment.QuarkusQsonGeneratorImpl;
import io.quarkus.qson.util.Types;
import io.quarkus.qson.deployment.QsonBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class CommonQsonJaxrsBuildStep {

    @BuildStep
    public void findQsonClasses(BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
                                QsonGeneratorBuildItem genItem,
                                BuildProducer<QsonBuildItem> qson
                                            ) throws Exception {
        IndexView index = beanArchiveIndexBuildItem.getIndex();
        QuarkusQsonGeneratorImpl generator = genItem.getGenerator();
        for (AnnotationInstance ai : index.getAnnotations(DotNames.GET)) {
            MethodInfo method = ai.target().asMethod();
            registerProducesJson(index, generator, method);
        }

        Set<AnnotationInstance> methods = new HashSet<>();
        methods.addAll(index.getAnnotations(DotNames.POST));
        methods.addAll(index.getAnnotations(DotNames.PUT));
        methods.addAll(index.getAnnotations(DotNames.DELETE));
        methods.addAll(index.getAnnotations(DotNames.PATCH));
        for (AnnotationInstance ai : methods) {
            MethodInfo method = ai.target().asMethod();
            registerProducesJson(index, generator, method);
            registerConsumesJson(index, generator, method);
        }
        qson.produce(new QsonBuildItem());
    }

    private void registerProducesJson(IndexView index, QuarkusQsonGeneratorImpl generator, MethodInfo method) throws Exception {
        if (method.hasAnnotation(DotNames.PRODUCES)) {
            AnnotationInstance produces = method.annotation(DotNames.PRODUCES);
            if (isJsonMediaType(produces)) {
                Method m = findMethod(method);
                if (m == null) {
                    throw new RuntimeException("Bad logic to turn MethodInfo to Method");
                }
                if (void.class.equals(m.getReturnType()) || Response.class.equals(m.getReturnType())) return;
                java.lang.reflect.Type genericType = m.getGenericReturnType();
                register(index, generator, method, genericType, false, true);
            }
        } else {
            AnnotationInstance produces = method.declaringClass().classAnnotation(DotNames.PRODUCES);
            if (isJsonMediaType(produces)) {
                Method m = findMethod(method);
                if (m == null) {
                    throw new RuntimeException("Bad logic to turn MethodInfo to Method");
                }
                if (void.class.equals(m.getReturnType()) || Response.class.equals(m.getReturnType())) return;
                java.lang.reflect.Type genericType = m.getGenericReturnType();
                register(index, generator, method, genericType, false, true);
            }
        }
    }

    private void register(IndexView index, QuarkusQsonGeneratorImpl generator, MethodInfo method, java.lang.reflect.Type genericType, boolean parser, boolean writer) throws ClassNotFoundException {
        if (Types.containsTypeVariable(genericType)) {
            for (ClassInfo ci : index.getAllKnownSubclasses(method.declaringClass().name())) {
                Class sub = Thread.currentThread().getContextClassLoader().loadClass(ci.name().toString());
                genericType = TypeUtil.resolveTypeVariables(sub, genericType);
                if (!Types.containsTypeVariable(genericType)) {
                    generator.register(genericType, parser, writer);
                }
            }
        } else {
            generator.register(genericType, parser, writer);
        }
    }

    private void registerConsumesJson(IndexView index, QuarkusQsonGeneratorImpl generator, MethodInfo method) throws Exception {
        if (method.hasAnnotation(DotNames.CONSUMES)) {
            AnnotationInstance consumes = method.annotation(DotNames.CONSUMES);
            if (isJsonMediaType(consumes)) {
                Method m = findMethod(method);
                if (m == null) {
                    throw new RuntimeException("Bad logic to turn MethodInfo to Method");
                }
                int idx = findBodyParameter(m);
                if (idx < 0) return;
                java.lang.reflect.Type genericType = m.getGenericParameterTypes()[idx];
                register(index, generator, method, genericType, true, false);
            }
        } else {
            AnnotationInstance consumes = method.declaringClass().classAnnotation(DotNames.CONSUMES);
            if (isJsonMediaType(consumes)) {
                Method m = findMethod(method);
                if (m == null) {
                    throw new RuntimeException("Bad logic to turn MethodInfo to Method");
                }
                int idx = findBodyParameter(m);
                if (idx < 0) return;
                java.lang.reflect.Type genericType = m.getGenericParameterTypes()[idx];
                register(index, generator, method, genericType, true, false);
            }
        }
    }

    private int findBodyParameter(Method m) {
        for (int i = 0; i < m.getParameterCount(); i++) {
            Annotation[] anns = m.getParameterAnnotations()[i];
            boolean isBody = true;
            for (Annotation ann : anns) {
                if (ann.annotationType().getName().endsWith("Param")
                    || ann.annotationType().equals(Context.class)) {
                    isBody = false;
                    break;
                }
            }
            if (isBody) return i;
        }
        return -1;
    }

    private Method findMethod(MethodInfo method) throws Exception {
        ClassLoader tcl = Thread.currentThread().getContextClassLoader();
        Class declaring = tcl.loadClass(method.declaringClass().toString());
        if (method.parameters().isEmpty()) {
            return declaring.getDeclaredMethod(method.name());
        }
        List<Class> params = new LinkedList<>();
        for (Type type : method.parameters()) {
            params.add(tcl.loadClass(type.name().toString()));
        }
        return declaring.getDeclaredMethod(method.name(), params.toArray(new Class[params.size()]));
    }

    private boolean isJsonMediaType(AnnotationInstance annotationInstance) {
        if (annotationInstance == null) return false;
        final AnnotationValue annotationValue = annotationInstance.value();
        if (annotationValue == null) {
            return false;
        }

        List<String> mediaTypes = Collections.emptyList();
        if (annotationValue.kind() == AnnotationValue.Kind.ARRAY) {
            mediaTypes = Arrays.asList(annotationValue.asStringArray());
        } else if (annotationValue.kind() == AnnotationValue.Kind.STRING) {
            mediaTypes = Collections.singletonList(annotationValue.asString());
        }
        return mediaTypes.contains(MediaType.APPLICATION_JSON)
                || mediaTypes.contains(MediaType.APPLICATION_JSON_PATCH_JSON);
    }
}
