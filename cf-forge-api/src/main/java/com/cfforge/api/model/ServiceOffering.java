package com.cfforge.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ServiceOffering(String guid, String name, String description, boolean available) {}
