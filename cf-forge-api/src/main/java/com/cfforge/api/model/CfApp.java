package com.cfforge.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CfApp(String guid, String name, String state, Lifecycle lifecycle) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Lifecycle(String type) {}
}
