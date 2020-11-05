package io.quarkus.qson.resteasy.deployment;

import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.qson.Types;
import io.quarkus.qson.deployment.QsonBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyDotNames;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
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

public class QsonJaxrsBuildStep {

    @BuildStep
    public void findQsonClasses(BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
                                BuildProducer<QsonBuildItem> qson
                                            ) throws Exception {
        IndexView index = beanArchiveIndexBuildItem.getIndex();
        for (AnnotationInstance ai : index.getAnnotations(ResteasyDotNames.GET)) {
            MethodInfo method = ai.target().asMethod();
            registerProducesJson(qson, method);
        }

        Set<AnnotationInstance> methods = new HashSet<>();
        methods.addAll(index.getAnnotations(ResteasyDotNames.POST));
        methods.addAll(index.getAnnotations(ResteasyDotNames.PUT));
        methods.addAll(index.getAnnotations(ResteasyDotNames.DELETE));
        methods.addAll(index.getAnnotations(ResteasyDotNames.PATCH));
        for (AnnotationInstance ai : methods) {
            MethodInfo method = ai.target().asMethod();
            registerProducesJson(qson, method);
            registerConsumesJson(qson, method);
        }


    }

    private void registerProducesJson(BuildProducer<QsonBuildItem> qson, MethodInfo method) throws Exception {
        if (method.hasAnnotation(ResteasyDotNames.PRODUCES)) {
            AnnotationInstance produces = method.annotation(ResteasyDotNames.PRODUCES);
            if (isJsonMediaType(produces)) {
                Method m = findMethod(method);
                if (m == null) {
                    throw new RuntimeException("Bad logic to turn MethodInfo to Method");
                }
                if (void.class.equals(m.getReturnType()) || Response.class.equals(m.getReturnType())) return;
                if (Types.containsTypeVariable(m.getGenericReturnType())) {
                    throw new RuntimeException("QSON + Resteasy cannot handle type variables: " + m.toString());
                }
                qson.produce(new QsonBuildItem(m.getReturnType(), m.getGenericReturnType(), false, true));
            }
        } else {
            AnnotationInstance produces = method.declaringClass().classAnnotation(ResteasyDotNames.PRODUCES);
            if (isJsonMediaType(produces)) {
                Method m = findMethod(method);
                if (m == null) {
                    throw new RuntimeException("Bad logic to turn MethodInfo to Method");
                }
                if (void.class.equals(m.getReturnType()) || Response.class.equals(m.getReturnType())) return;
                if (Types.containsTypeVariable(m.getGenericReturnType())) {
                    throw new RuntimeException("QSON + Resteasy cannot handle type variables: " + m.toString());
                }
                qson.produce(new QsonBuildItem(m.getReturnType(), m.getGenericReturnType(), false, true));
            }
        }
    }

    private void registerConsumesJson(BuildProducer<QsonBuildItem> qson, MethodInfo method) throws Exception {
        if (method.hasAnnotation(ResteasyDotNames.CONSUMES)) {
            AnnotationInstance produces = method.annotation(ResteasyDotNames.CONSUMES);
            if (isJsonMediaType(produces)) {
                Method m = findMethod(method);
                if (m == null) {
                    throw new RuntimeException("Bad logic to turn MethodInfo to Method");
                }
                int idx = findBodyParameter(m);
                if (idx < 0) return;
                java.lang.reflect.Type genericType = m.getGenericParameterTypes()[idx];
                if (Types.containsTypeVariable(genericType)) {
                    throw new RuntimeException("QSON + Resteasy cannot handle type variables: " + m.toString());
                }
                qson.produce(new QsonBuildItem(m.getParameterTypes()[idx], genericType, true, false));
            }
        } else {
            AnnotationInstance produces = method.declaringClass().classAnnotation(ResteasyDotNames.CONSUMES);
            if (isJsonMediaType(produces)) {
                Method m = findMethod(method);
                if (m == null) {
                    throw new RuntimeException("Bad logic to turn MethodInfo to Method");
                }
                int idx = findBodyParameter(m);
                if (idx < 0) return;
                java.lang.reflect.Type genericType = m.getGenericParameterTypes()[idx];
                if (Types.containsTypeVariable(genericType)) {
                    throw new RuntimeException("QSON + Resteasy cannot handle type variables: " + m.toString());
                }
                qson.produce(new QsonBuildItem(m.getParameterTypes()[idx], genericType, true, false));
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
