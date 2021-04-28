package io.quarkus.qson.deployment;

import io.quarkus.qson.QsonDate;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

import java.util.Optional;

/**
 * Qson configuration
 */
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class QsonBuildTimeConfig {

    /**
     * Global default date format for parsing and writing
     */
    @ConfigItem(defaultValue = "ISO_8601_OFFSET_DATE_TIME")
    Optional<QsonDate.Format> dateFormat;
}
