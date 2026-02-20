package com.cfforge.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "cfforge.health")
public class HealthCheckProperties {

    private Map<String, String> components = new LinkedHashMap<>();

    public Map<String, String> getComponents() {
        return components;
    }

    public void setComponents(Map<String, String> components) {
        this.components = components;
    }
}
