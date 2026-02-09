package com.cfforge.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrgQuota(String guid, String name, Apps apps, Services services, Routes routes) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Apps(Integer total_memory_in_mb, Integer per_process_memory_in_mb, Integer total_instances) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Services(Integer total_service_instances, Integer total_service_keys) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Routes(Integer total_routes) {}
}
