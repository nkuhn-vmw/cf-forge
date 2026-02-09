package com.cfforge.admin.service;

import com.cfforge.common.dto.MetricEvent;
import com.cfforge.common.entity.MetricSnapshot;
import com.cfforge.common.enums.MetricGranularity;
import com.cfforge.common.repository.MetricSnapshotRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
public class MetricAggregationService {

    private final MetricSnapshotRepository snapshotRepo;
    private final MeterRegistry meterRegistry;
    private final StringRedisTemplate redis;

    public MetricAggregationService(MetricSnapshotRepository snapshotRepo,
                                     MeterRegistry meterRegistry,
                                     StringRedisTemplate redis) {
        this.snapshotRepo = snapshotRepo;
        this.meterRegistry = meterRegistry;
        this.redis = redis;
    }

    public void record(MetricEvent event) {
        // Increment Redis real-time counter
        redis.opsForValue().increment("metrics:" + event.type() + ":count");
        String hourKey = "metrics:hourly:" + Instant.now().truncatedTo(ChronoUnit.HOURS).toString();
        redis.opsForHash().increment(hourKey, event.type(), 1);

        // Update Micrometer counter
        meterRegistry.counter("cfforge.metric.events", "type", event.type(), "source", event.source())
            .increment();

        if (event.durationMs() != null) {
            meterRegistry.timer("cfforge.metric.duration", "type", event.type())
                .record(java.time.Duration.ofMillis(event.durationMs()));
        }

        log.debug("Recorded metric event: type={}, source={}, success={}", event.type(), event.source(), event.success());
    }

    public void rollUpHourly(Instant hour) {
        Instant start = hour.truncatedTo(ChronoUnit.HOURS);
        Instant end = start.plus(1, ChronoUnit.HOURS);

        String hourKey = "metrics:hourly:" + start.toString();
        var entries = redis.opsForHash().entries(hourKey);

        entries.forEach((type, count) -> {
            snapshotRepo.save(MetricSnapshot.builder()
                .metricName(type.toString())
                .granularity(MetricGranularity.HOURLY)
                .periodStart(start)
                .periodEnd(end)
                .count(Long.parseLong(count.toString()))
                .build());
        });
    }
}
