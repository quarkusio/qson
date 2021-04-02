package io.quarkus.qson.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.builder.BuildException;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.qson.Qson;
import io.quarkus.qson.QsonIgnore;
import io.quarkus.qson.QsonProperty;
import io.quarkus.qson.generator.Generator;
import io.quarkus.qson.runtime.QuarkusQsonMapper;
import io.quarkus.qson.util.Types;
import io.quarkus.qson.generator.Deserializer;
import io.quarkus.qson.generator.Serializer;
import io.quarkus.qson.runtime.QuarkusQsonRegistry;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

public class QsonBuildStep {
    public static final DotName QSON = DotName.createSimple(Qson.class.getName());
    public static final DotName QSON_PROPERTY = DotName.createSimple(QsonProperty.class.getName());
    public static final DotName QSON_IGNORE = DotName.createSimple(QsonIgnore.class.getName());

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClass(QuarkusQsonMapper.class)
                .build();
    }

    @BuildStep
    void scan(BuildProducer<QsonBuildItem> qson,
              CombinedIndexBuildItem combinedIndex) throws Exception {
        Collection<AnnotationInstance> annotations = combinedIndex.getIndex().getAnnotations(QSON);
        Set<String> classes = new HashSet<>();
        registerQson(qson, annotations, classes);
        annotations = combinedIndex.getIndex().getAnnotations(QSON_PROPERTY);
        register(qson, annotations, classes);
        annotations = combinedIndex.getIndex().getAnnotations(QSON_IGNORE);
        register(qson, annotations, classes);
    }

    private void registerQson(BuildProducer<QsonBuildItem> qson, Collection<AnnotationInstance> annotations, Set<String> classes) throws BuildException, ClassNotFoundException {
        for (AnnotationInstance ai : annotations) {
            ClassInfo ci = ai.target().asClass();
            String className = ci.name().toString();
            if (!Modifier.isPublic(ci.flags()) || Modifier.isInterface(ci.flags())) {
                throw new BuildException("@Qson annnotation can only be placed on public classes: " + className, Collections.emptyList());
            }
            if (Modifier.isAbstract(ci.flags())) {
                throw new BuildException("@Qson annnotation cannot be placed on an abstract class: " + className, Collections.emptyList());

            }
            AnnotationValue generateParser = ai.value("generateParser");
            AnnotationValue generateWriter = ai.value("generateWriter");
            boolean parser = generateParser == null || generateParser.asBoolean();
            boolean writer = generateWriter == null || generateWriter.asBoolean();
            if (!classes.contains(className)) {
                Class clz = Thread.currentThread().getContextClassLoader().loadClass(className);
                qson.produce(new QsonBuildItem(clz, parser, writer));
                classes.add(className);
            }
        }
    }

    private void register(BuildProducer<QsonBuildItem> qson, Collection<AnnotationInstance> annotations, Set<String> classes) throws BuildException, ClassNotFoundException {
        for (AnnotationInstance ai : annotations) {
            ClassInfo ci = ai.target().asClass();
            String className = ci.name().toString();
            if (!Modifier.isPublic(ci.flags()) || Modifier.isInterface(ci.flags())) {
                throw new BuildException("@Qson annnotation can only be placed on public classes: " + className, Collections.emptyList());
            }
            if (Modifier.isAbstract(ci.flags())) {
                throw new BuildException("@Qson annnotation cannot be placed on an abstract class: " + className, Collections.emptyList());

            }
            if (!classes.contains(className)) {
                Class clz = Thread.currentThread().getContextClassLoader().loadClass(className);
                qson.produce(new QsonBuildItem(clz, true, true));
                classes.add(className);
            }
        }
    }

    @BuildStep
    public GeneratedQsonClassesBuildItem generate(BuildProducer<GeneratedClassBuildItem> toGenerate,
                         List<QsonBuildItem> classes) {
        if (classes == null || classes.isEmpty()) return null;
        Set<Type> parsers = new HashSet<>();
        Set<Type> writers = new HashSet<>();

        // squeeze duplicate entries
        for (QsonBuildItem item : classes) {
            if(item.isGenerateParser()) parsers.add(item.getGenericType());
            if(item.isGenerateWriter()) writers.add(item.getGenericType());
        }

        Map<String, String> generatedParsers = new HashMap<>();
        Map<String, String> generatedWriters = new HashMap<>();

        GeneratedClassGizmoAdaptor adaptor = new GeneratedClassGizmoAdaptor(toGenerate);

        generateParsers(parsers, generatedParsers, adaptor);
        generateWriters(writers, generatedWriters, adaptor);

        return new GeneratedQsonClassesBuildItem(generatedParsers, generatedWriters);
    }

    public void generateParsers(Set<Type> parsers, Map<String, String> generatedParsers, GeneratedClassGizmoAdaptor adaptor) {
        for (Type entry : parsers) {
            String key = Types.typename(entry);
            if (generatedParsers.containsKey(key)) continue;
            Generator generator = new Generator();
            Deserializer.Builder builder = generator.deserializer(entry);
            builder.output(adaptor).generate();
            generatedParsers.put(key, builder.className());
            generateParsers(builder.referenced(), generatedParsers, adaptor);
        }
    }
    public void generateWriters(Set<Type> writers, Map<String, String> generatedWriters, GeneratedClassGizmoAdaptor adaptor) {
        for (Type entry : writers) {
            String key = Types.typename(entry);
            if (generatedWriters.containsKey(key)) continue;
            Generator generator = new Generator();
            Serializer.Builder builder = generator.serializer(entry);
            builder.output(adaptor).generate();
            generatedWriters.put(key, builder.className());
            generateWriters(builder.referenced(), generatedWriters, adaptor);
        }
    }

    @BuildStep()
    @Record(STATIC_INIT)
    public QsonCompletedBuildItem staticInit(QuarkusQsonRegistry registry,
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
