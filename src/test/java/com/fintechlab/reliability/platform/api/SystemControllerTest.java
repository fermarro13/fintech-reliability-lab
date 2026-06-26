package com.fintechlab.reliability.platform.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SystemControllerTest {

    @Test
    void statusReturnsServiceMetadata() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-26T15:00:00Z"), ZoneOffset.UTC);
        SystemController controller = new SystemController(clock, "fintech-reliability-lab");

        SystemController.SystemStatus status = controller.status();

        assertThat(status.service()).isEqualTo("fintech-reliability-lab");
        assertThat(status.status()).isEqualTo("UP");
        assertThat(status.observedAt()).isEqualTo(Instant.parse("2026-06-26T15:00:00Z"));
    }
}
