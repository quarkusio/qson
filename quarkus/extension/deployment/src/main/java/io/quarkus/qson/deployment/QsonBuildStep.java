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
import io.quarkus.qson.QsonCustomWriter;
import io.quarkus.qson.QsonIgnore;
import io.quarkus.qson.QsonProperty;
import io.quarkus.qson.QsonTransformer;
import io.quarkus.qson.generator.Generator;
import io.quarkus.qson.runtime.QuarkusQsonGenerator;
import io.quarkus.qson.runtime.QuarkusQsonInitializer;
import io.quarkus.qson.runtime.QuarkusQsonMapper;
import io.quarkus.qson.util.Types;
import io.quarkus.qson.generator.ParserGenerator;
import io.quarkus.qson.generator.WriterGenerator;
import io.quarkus.qson.runtime.QuarkusQsonRegistry;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import java.lang.reflect.Method;
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
    public static final DotName QSON_TRANSFORMER = DotName.createSimple(QsonTransformer.class.getName());
    public static final DotName QSON_CUSTOM_WRITER = DotName.createSimple(QsonCustomWriter.class.getName());
    public static final DotName QUARKUS_QSON_INITIALIZER = DotName.createSimple(QuarkusQsonInitializer.class.getName());
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
    QsonGeneratorBuildItem publishGenerator() {
        return new QsonGeneratorBuildItem(new QuarkusQsonGeneratorImpl());
    }

    @BuildStep
    void scan(BuildProducer<QsonBuildItem> qson,
              QsonGeneratorBuildItem genItem,
              CombinedIndexBuildItem combinedIndex) throws Exception {
        Collection<AnnotationInstance> annotations = combinedIndex.getIndex().getAnnotations(QSON);
        Set<String> classes = new HashSet<>();
        registerQson(genItem.getGenerator(), annotations, classes);
        annotations = combinedIndex.getIndex().getAnnotations(QSON_PROPERTY);
        register(genItem.getGenerator(), annotations, classes);
        annotations = combinedIndex.getIndex().getAnnotations(QSON_IGNORE);
        register(genItem.getGenerator(), annotations, classes);
        registerTransformer(genItem.getGenerator(), combinedIndex);
        registerCustomWriter(genItem.getGenerator(), combinedIndex);
        qson.produce(new QsonBuildItem());
    }

    private void registerTransformer(QuarkusQsonGeneratorImpl generator, CombinedIndexBuildItem combinedIndex) throws Exception {
        Collection<AnnotationInstance> annotations = combinedIndex.getIndex().getAnnotations(QSON_TRANSFORMER);
        for (AnnotationInstance ai : annotations) {
            MethodInfo mi = ai.target().asMethod();
            ClassInfo dec = mi.declaringClass();
            Class declaring = Thread.currentThread().getContextClassLoader().loadClass(dec.name().toString());
            Method method = declaring.getMethod(mi.name());
            generator.overrideMappingFor(method.getReturnType()).transformer(declaring);
            generator.register(method.getReturnType(), true, false);
        }
    }

    private void registerCustomWriter(QuarkusQsonGeneratorImpl generator, CombinedIndexBuildItem combinedIndex) throws Exception {
        Collection<AnnotationInstance> annotations = combinedIndex.getIndex().getAnnotations(QSON_CUSTOM_WRITER);
        for (AnnotationInstance ai : annotations) {
            ClassInfo dec = ai.target().asClass();
            Class declaring = Thread.currentThread().getContextClassLoader().loadClass(dec.name().toString());
            QsonCustomWriter ann = (QsonCustomWriter)declaring.getAnnotation(QsonCustomWriter.class);
            generator.overrideMappingFor(ann.value()).customWriter(declaring);
            generator.register(ann.value(), false, true);
        }
    }

    private void registerQson(QuarkusQsonGeneratorImpl generator, Collection<AnnotationInstance> annotations, Set<String> classes) throws BuildException, ClassNotFoundException {
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
                generator.register(clz, parser, writer);
                classes.add(className);
            }
        }
    }

    private void register(QuarkusQsonGeneratorImpl generator, Collection<AnnotationInstance> annotations, Set<String> classes) throws BuildException, ClassNotFoundException {
        for (AnnotationInstance ai : annotations) {
            ClassInfo ci = null;
            if (ai.target().kind() == AnnotationTarget.Kind.CLASS) {
                ci = ai.target().asClass();
            } else if (ai.target().kind() == AnnotationTarget.Kind.METHOD) {
                ci = ai.target().asMethod().declaringClass();
            } else if (ai.target().kind() == AnnotationTarget.Kind.FIELD) {
                ci = ai.target().asField().declaringClass();
            } else {
                return;  // do not know what to do.
            }
            String className = ci.name().toString();
            if (!Modifier.isPublic(ci.flags()) || Modifier.isInterface(ci.flags())) {
                throw new BuildException("Qson mapped classes must be public classes: " + className, Collections.emptyList());
            }
            if (Modifier.isAbstract(ci.flags())) {
                throw new BuildException("Qson mapped classes cannot be abstract: " + className, Collections.emptyList());

            }
            if (!classes.contains(className)) {
                Class clz = Thread.currentThread().getContextClassLoader().loadClass(className);
                generator.register(clz, true, true);
                classes.add(className);
            }
        }
    }

    @BuildStep
    public GeneratedQsonClassesBuildItem generate(BuildProducer<GeneratedClassBuildItem> toGenerate,
                                                  QsonGeneratorBuildItem genItem,
                                                  List<QsonBuildItem> ignore,
                                                  CombinedIndexBuildItem combinedIndex) throws Exception {
        QuarkusQsonGeneratorImpl generator = genItem.getGenerator();

        Collection<AnnotationInstance> annotations = combinedIndex.getIndex().getAnnotations(QUARKUS_QSON_INITIALIZER);
        for (AnnotationInstance ai : annotations) {
            MethodInfo method = ai.target().asMethod();
            ClassInfo declaring = method.declaringClass();
            if (!Modifier.isPublic(method.flags())
                    || !Modifier.isStatic(method.flags())
                    || method.returnType().kind() != org.jboss.jandex.Type.Kind.VOID
                    || method.parameters().size() != 1
                    || method.parameters().get(0).kind() != org.jboss.jandex.Type.Kind.CLASS
            ) {
                throw new BuildException("Bad signature for @QuarkusQsonInitializer annotated method: " + method.toString(), Collections.emptyList());
            }
            Class initClass = Thread.currentThread().getContextClassLoader().loadClass(declaring.name().toString());
            Method m = initClass.getMethod(method.name(), QuarkusQsonGenerator.class);
            m.invoke(null, generator);
        }

        Map<String, String> generatedParsers = new HashMap<>();
        Map<String, String> generatedWriters = new HashMap<>();

        GeneratedClassGizmoAdaptor adaptor = new GeneratedClassGizmoAdaptor(toGenerate);

        generateParsers(generator, generator.getParsers(), generatedParsers, adaptor);
        generateWriters(generator, generator.getWriters(), generatedWriters, adaptor);

        return new GeneratedQsonClassesBuildItem(generatedParsers, generatedWriters);
    }

    public void generateParsers(QuarkusQsonGeneratorImpl generator, Set<Type> parsers, Map<String, String> generatedParsers, GeneratedClassGizmoAdaptor adaptor) {
        for (Type entry : parsers) {
            String key = Types.typename(entry);
            if (generatedParsers.containsKey(key)) continue;
            ParserGenerator.Builder builder = generator.parser(entry);
            builder.output(adaptor).generate();
            generatedParsers.put(key, builder.className());
            generateParsers(generator, builder.referenced(), generatedParsers, adaptor);
        }
    }
    public void generateWriters(QuarkusQsonGeneratorImpl generator, Set<Type> writers, Map<String, String> generatedWriters, GeneratedClassGizmoAdaptor adaptor) {
        for (Type entry : writers) {
            String key = Types.typename(entry);
            if (generatedWriters.containsKey(key)) continue;
            WriterGenerator.Builder builder = generator.writer(entry);
            builder.output(adaptor).generate();
            generatedWriters.put(key, builder.className());
            generateWriters(generator, builder.referenced(), generatedWriters, adaptor);
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
