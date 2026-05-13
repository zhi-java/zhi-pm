package io.github.zhi.pm.autoconfigure;

import io.github.zhi.pm.core.registry.ConnectionRegistry;
import io.github.zhi.pm.core.send.MessageSender;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ReactiveWebSocketGatewayAutoConfigurationTest {
    private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ReactiveWebSocketGatewayAutoConfiguration.class));

    @Test
    void createsDefaultGatewayBeans() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(ConnectionRegistry.class)
                .hasSingleBean(MessageSender.class)
                .hasSingleBean(GatewayWebSocketHandler.class)
                .hasBean("realtimeWebSocketHandlerMapping"));
    }

    @Test
    void backsOffWhenDisabled() {
        contextRunner.withPropertyValues("realtime.websocket.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(GatewayWebSocketHandler.class));
    }
}
