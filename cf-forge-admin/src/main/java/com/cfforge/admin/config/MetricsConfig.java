package com.cfforge.admin.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    private final MeterRegistry meterRegistry;

    public MetricsConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
}
