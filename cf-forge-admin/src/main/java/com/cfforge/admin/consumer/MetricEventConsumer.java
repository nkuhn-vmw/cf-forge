package com.cfforge.admin.consumer;

import com.cfforge.admin.service.MetricAggregationService;
import com.cfforge.common.dto.MetricEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class MetricEventConsumer {

    private final MetricAggregationService aggregationService;

    public MetricEventConsumer(MetricAggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @Bean
    public Consumer<MetricEvent> metricEvent() {
        return aggregationService::record;
    }
}
