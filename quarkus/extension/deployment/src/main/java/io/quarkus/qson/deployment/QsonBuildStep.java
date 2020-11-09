package io.quarkus.qson.deployment;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.builder.BuildException;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.qson.Qson;
import io.quarkus.qson.Types;
import io.quarkus.qson.generator.Deserializer;
import io.quarkus.qson.generator.Serializer;
import io.quarkus.qson.runtime.QsonRegistry;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

public class QsonBuildStep {
    public static final DotName QSON = DotName.createSimple(Qson.class.getName());

    @BuildStep
    void scan(BuildProducer<QsonBuildItem> qson,
              CombinedIndexBuildItem combinedIndex) throws Exception {
        Collection<AnnotationInstance> annotations = combinedIndex.getIndex().getAnnotations(QSON);
        for (AnnotationInstance ai : annotations) {
            ClassInfo ci = ai.target().asClass();
            if (!Modifier.isPublic(ci.flags()) || Modifier.isInterface(ci.flags())) {
                throw new BuildException("@Qson annnotation can only be placed on public classes: " + ci.name().toString(), Collections.emptyList());
            }
            if (Modifier.isAbstract(ci.flags())) {
                throw new BuildException("@Qson annnotation cannot be placed on an abstract class: " + ci.name().toString(), Collections.emptyList());

            }
            Class clz = Thread.currentThread().getContextClassLoader().loadClass(ci.name().toString());
            qson.produce(new QsonBuildItem(clz, clz, ai.value("generateParser").asBoolean(), ai.value("generateWriter").asBoolean()));
        }
    }

    @BuildStep
    public GeneratedQsonClassesBuildItem generate(BuildProducer<GeneratedClassBuildItem> toGenerate,
                         List<QsonBuildItem> classes) {
        if (classes == null || classes.isEmpty()) return null;
        Map<Type, Class> parsers = new HashMap<>();
        Map<Type, Class> writers = new HashMap<>();

        // squeeze duplicate entries
        for (QsonBuildItem item : classes) {
            if(item.isGenerateParser()) parsers.put(item.getGenericType(), item.getType());
            if(item.isGenerateWriter()) writers.put(item.getGenericType(), item.getType());
        }

        Map<String, String> generatedParsers = new HashMap<>();
        Map<String, String> generatedWriters = new HashMap<>();

        GeneratedClassGizmoAdaptor adaptor = new GeneratedClassGizmoAdaptor(toGenerate);

        generateParsers(parsers, generatedParsers, adaptor);
        generateWriters(writers, generatedWriters, adaptor);

        return new GeneratedQsonClassesBuildItem(generatedParsers, generatedWriters);
    }

    public void generateParsers(Map<Type, Class> parsers, Map<String, String> generatedParsers, GeneratedClassGizmoAdaptor adaptor) {
        for (Map.Entry<Type, Class> entry : parsers.entrySet()) {
            String key = Types.typename(entry.getKey());
            if (generatedParsers.containsKey(key)) continue;
            Deserializer.Builder builder = Deserializer.create(entry.getValue(), entry.getKey());
            builder.output(adaptor).generate();
            generatedParsers.put(key, builder.className());
            generateParsers(builder.referenced(), generatedParsers, adaptor);
        }
    }
    public void generateWriters(Map<Type, Class> parsers, Map<String, String> generatedWriters, GeneratedClassGizmoAdaptor adaptor) {
        for (Map.Entry<Type, Class> entry : parsers.entrySet()) {
            String key = Types.typename(entry.getKey());
            if (generatedWriters.containsKey(key)) continue;
            Serializer.Builder builder = Serializer.create(entry.getValue(), entry.getKey());
            builder.output(adaptor).generate();
            generatedWriters.put(key, builder.className());
            generateWriters(builder.referenced(), generatedWriters, adaptor);
        }
    }

    @BuildStep()
    @Record(STATIC_INIT)
    public QsonCompletedBuildItem staticInit(QsonRegistry registry,
                                             RecorderContext context,
                                             BeanContainerBuildItem beanContainer, // dependency
                                             GeneratedQsonClassesBuildItem generated) {
        registry.clear();  // not sure if we need this for redeploy?

        if (generated == null) return new QsonCompletedBuildItem();

        for (Map.Entry<String, String> entry : generated.getGeneratedParsers().entrySet()) {
            registry.registerParser(entry.getKey(), context.newInstance(entry.getValue()));
        }
        for (Map.Entry<String, String> entry : generated.getGeneratedWriters().entrySet()) {
            registry.registerWriter(entry.getKey(), context.newInstance(entry.getValue()));
        }

        return new QsonCompletedBuildItem();
    }

}
