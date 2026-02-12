package com.cfforge.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ServiceOffering(
    String guid,
    String name,
    String description,
    boolean available,
    List<String> plans,
    List<String> tags
) {
    public ServiceOffering(String guid, String name, String description, boolean available) {
        this(guid, name, description, available, List.of(), List.of());
    }
}
