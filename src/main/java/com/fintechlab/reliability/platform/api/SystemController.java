package com.fintechlab.reliability.platform.api;

import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    private final Clock clock;
    private final String serviceName;

    public SystemController(Clock clock, @Value("${spring.application.name}") String serviceName) {
        this.clock = clock;
        this.serviceName = serviceName;
    }

    @GetMapping("/status")
    public SystemStatus status() {
        return new SystemStatus(serviceName, "UP", Instant.now(clock));
    }

    public record SystemStatus(String service, String status, Instant observedAt) {
    }
}
