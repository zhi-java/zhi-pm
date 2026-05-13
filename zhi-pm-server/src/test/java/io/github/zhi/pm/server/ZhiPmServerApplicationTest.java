package io.github.zhi.pm.server;

import io.github.zhi.pm.core.send.MessageSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ZhiPmServerApplicationTest {
    @Autowired MessageSender sender;

    @Test
    void contextLoadsWithGatewayStarter() {
        assertThat(sender).isNotNull();
    }
}
