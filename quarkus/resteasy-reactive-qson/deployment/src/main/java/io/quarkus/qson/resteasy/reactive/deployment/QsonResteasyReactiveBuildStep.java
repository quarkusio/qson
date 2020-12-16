package io.quarkus.qson.resteasy.reactive.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.qson.resteasy.reactive.QsonMessageBodyReader;
import io.quarkus.qson.resteasy.reactive.QsonMessageBodyWriter;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;

import javax.ws.rs.core.MediaType;
import java.util.Collections;

public class QsonResteasyReactiveBuildStep {

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem("resteasy-reactive-qson"));
    }

    @BuildStep
    void additionalProviders(BuildProducer<AdditionalBeanBuildItem> additionalBean,
                             BuildProducer<MessageBodyReaderBuildItem> additionalReaders,
                             BuildProducer<MessageBodyWriterBuildItem> additionalWriters) {
        // make these beans to they can get instantiated with the Quarkus CDI configured Jsonb object
        additionalBean.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(QsonMessageBodyReader.class.getName())
                .addBeanClass(QsonMessageBodyWriter.class.getName())
                .setUnremovable().build());

        additionalReaders
                .produce(new MessageBodyReaderBuildItem(QsonMessageBodyReader.class.getName(), Object.class.getName(),
                        Collections.singletonList(MediaType.APPLICATION_JSON)));
        additionalWriters
                .produce(new MessageBodyWriterBuildItem(QsonMessageBodyWriter.class.getName(), Object.class.getName(),
                        Collections.singletonList(MediaType.APPLICATION_JSON)));
    }
}
