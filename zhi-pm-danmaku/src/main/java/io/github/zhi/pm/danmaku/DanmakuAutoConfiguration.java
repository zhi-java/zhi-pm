package io.github.zhi.pm.danmaku;

import io.github.zhi.pm.core.danmaku.DanmakuService;
import io.github.zhi.pm.core.registry.ConnectionRegistry;
import io.github.zhi.pm.core.send.MessageSender;
import io.github.zhi.pm.danmaku.filter.ContentFilter;
import io.github.zhi.pm.danmaku.mute.InMemoryMuteService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(DanmakuService.class)
@EnableConfigurationProperties(DanmakuProperties.class)
@ConditionalOnProperty(prefix = "realtime.danmaku", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DanmakuAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ContentFilter contentFilter(DanmakuProperties properties) {
        return new ContentFilter(properties.getSensitiveWords());
    }

    @Bean
    @ConditionalOnMissingBean
    InMemoryMuteService danmakuMuteService() {
        return new InMemoryMuteService();
    }

    @Bean
    @ConditionalOnMissingBean(DanmakuService.class)
    DanmakuServiceImpl danmakuService(MessageSender sender, ConnectionRegistry registry,
                                       ContentFilter contentFilter, InMemoryMuteService muteService,
                                       DanmakuProperties properties) {
        return new DanmakuServiceImpl(sender, registry, contentFilter, muteService,
                properties.getMaxContentLength(),
                properties.getMaxMessagePerUserPerSecond(),
                properties.getMaxMessagePerRoomPerSecond());
    }
}
