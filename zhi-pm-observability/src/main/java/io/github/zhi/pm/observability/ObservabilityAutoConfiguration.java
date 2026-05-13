package io.github.zhi.pm.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(MeterRegistry.class)
@EnableConfigurationProperties(ObservabilityProperties.class)
@ConditionalOnProperty(prefix = "realtime.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    GatewayMetrics gatewayMetrics(MeterRegistry registry) {
        return new GatewayMetrics(registry);
    }
}
