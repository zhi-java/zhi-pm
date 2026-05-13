package io.github.zhi.pm.admin;

import io.github.zhi.pm.core.registry.ConnectionRegistry;
import io.github.zhi.pm.core.send.MessageSender;
import io.github.zhi.pm.observability.GatewayMetrics;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(GatewayMetrics.class)
@EnableConfigurationProperties(AdminProperties.class)
@ConditionalOnProperty(prefix = "realtime.admin", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    AdminService adminService(ConnectionRegistry registry, MessageSender sender, GatewayMetrics metrics) {
        return new AdminService(registry, sender, metrics);
    }

    @Bean
    @ConditionalOnMissingBean
    AdminController adminController(AdminService adminService) {
        return new AdminController(adminService);
    }
}
