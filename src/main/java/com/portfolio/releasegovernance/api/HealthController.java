package com.portfolio.releasegovernance.api;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
public class HealthController {
    @GetMapping("/health")
    public Map<String, Object> health() { return Map.of("status", "UP", "timestamp", Instant.now()); }
}
