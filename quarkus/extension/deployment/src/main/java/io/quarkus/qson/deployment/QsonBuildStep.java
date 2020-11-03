package io.quarkus.qson.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.qson.generator.Deserializer;
import io.quarkus.qson.generator.Serializer;
import io.quarkus.qson.runtime.QuarkusQsonRegistry;
import io.quarkus.qson.runtime.RegistryRecorder;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

public class QsonBuildStep {

    @BuildStep
    public GeneratedQsonClassesBuildItem generate(BuildProducer<GeneratedClassBuildItem> toGenerate,
                         List<QsonBuildItem> classes) {
        if (classes == null || classes.isEmpty()) return null;
        Map<Class, Type> parsers = new HashMap<>();
        Map<Class, Type> writers = new HashMap<>();

        // squeeze duplicate entries
        for (QsonBuildItem item : classes) {
            if(item.isGenerateParser()) parsers.put(item.getType(), item.getGenericType());
            if(item.isGenerateWriter()) writers.put(item.getType(), item.getGenericType());
        }

        Map<String, String> generatedParsers = new HashMap<>();
        Map<String, String> generatedWriters = new HashMap<>();

        GeneratedClassGizmoAdaptor adaptor = new GeneratedClassGizmoAdaptor(toGenerate);

        generateParsers(parsers, generatedParsers, adaptor);
        generateWriters(writers, generatedWriters, adaptor);

        return new GeneratedQsonClassesBuildItem(generatedParsers, generatedWriters);
    }

    public void generateParsers(Map<Class, Type> parsers, Map<String, String> generatedParsers, GeneratedClassGizmoAdaptor adaptor) {
        for (Map.Entry<Class, Type> entry : parsers.entrySet()) {
            String key = entry.getValue() == null ? entry.getKey().getTypeName() : entry.getValue().getTypeName();
            if (generatedParsers.containsKey(key)) continue;
            Deserializer.Builder builder = Deserializer.create(entry.getKey(), entry.getValue());
            builder.output(adaptor).generate();
            generatedParsers.put(key, builder.className());
            generateParsers(builder.referenced(), generatedParsers, adaptor);
        }
    }
    public void generateWriters(Map<Class, Type> parsers, Map<String, String> generatedWriters, GeneratedClassGizmoAdaptor adaptor) {
        for (Map.Entry<Class, Type> entry : parsers.entrySet()) {
            String key = entry.getValue() == null ? entry.getKey().getTypeName() : entry.getValue().getTypeName();
            if (generatedWriters.containsKey(key)) continue;
            Serializer.Builder builder = Serializer.create(entry.getKey(), entry.getValue());
            builder.output(adaptor).generate();
            generatedWriters.put(key, builder.className());
            generateWriters(builder.referenced(), generatedWriters, adaptor);
        }
    }

    @BuildStep
    AdditionalBeanBuildItem producer() {
        return AdditionalBeanBuildItem.unremovableOf(QuarkusQsonRegistry.class);
    }


    @BuildStep()
    @Record(STATIC_INIT)
    public QsonCompletedBuildItem staticInit(RegistryRecorder binding,
                                             RecorderContext context,
                                             BeanContainerBuildItem beanContainer, // dependency
                           GeneratedQsonClassesBuildItem generated) {
        if (generated == null) return new QsonCompletedBuildItem();

        for (Map.Entry<String, String> entry : generated.getGeneratedParsers().entrySet()) {
            binding.registerParser(entry.getKey(), context.newInstance(entry.getValue()));
        }
        for (Map.Entry<String, String> entry : generated.getGeneratedWriters().entrySet()) {
            binding.registerWriter(entry.getKey(), context.newInstance(entry.getValue()));
        }

        return new QsonCompletedBuildItem();
    }

}
