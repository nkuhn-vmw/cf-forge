package com.cfforge.common.events;

import com.cfforge.common.dto.MetricEvent;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class MetricEventPublisher {

    private final StreamBridge streamBridge;
    private final String sourceName;

    public MetricEventPublisher(StreamBridge streamBridge,
                                 org.springframework.core.env.Environment env) {
        this.streamBridge = streamBridge;
        this.sourceName = env.getProperty("spring.application.name", "unknown");
    }

    public void publish(String type, UUID userId, UUID projectId,
                        Integer durationMs, boolean success, String errorMessage,
                        Map<String, String> dimensions) {
        var event = new MetricEvent(
            type, sourceName, userId, projectId,
            durationMs, success, errorMessage,
            dimensions, Instant.now()
        );
        streamBridge.send("metricEvent-out-0", event);
    }

    public void publishSuccess(String type, UUID userId, UUID projectId, Integer durationMs) {
        publish(type, userId, projectId, durationMs, true, null, Map.of());
    }

    public void publishFailure(String type, UUID userId, UUID projectId, String error) {
        publish(type, userId, projectId, null, false, error, Map.of());
    }
}
