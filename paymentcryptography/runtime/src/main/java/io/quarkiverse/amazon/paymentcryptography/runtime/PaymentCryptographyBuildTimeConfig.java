package io.quarkiverse.amazon.paymentcryptography.runtime;

import io.quarkiverse.amazon.common.runtime.AsyncHttpClientBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.DevServicesBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.HasSdkBuildTimeConfig;
import io.quarkiverse.amazon.common.runtime.SyncHttpClientBuildTimeConfig;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * Amazon Payment Cryptography build time configuration
 */
@ConfigMapping(prefix = "quarkus.paymentcryptography")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface PaymentCryptographyBuildTimeConfig extends HasSdkBuildTimeConfig {

    /**
     * Sync HTTP transport configuration for Amazon Payment Cryptography client
     */
    SyncHttpClientBuildTimeConfig syncClient();

    /**
     * Async HTTP transport configuration for Amazon Payment Cryptography client
     */
    AsyncHttpClientBuildTimeConfig asyncClient();

    /**
     * Config for dev services
     */
    DevServicesBuildTimeConfig devservices();
}
