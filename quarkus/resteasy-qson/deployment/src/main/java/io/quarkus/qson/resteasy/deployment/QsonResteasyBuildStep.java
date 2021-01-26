package io.quarkus.qson.resteasy.deployment;

import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class QsonResteasyBuildStep {

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem("resteasy-qson"));
    }

    @BuildStep
    void capabilities(BuildProducer<CapabilityBuildItem> capability) {
        capability.produce(new CapabilityBuildItem(Capability.RESTEASY_JSON));
    }
}
