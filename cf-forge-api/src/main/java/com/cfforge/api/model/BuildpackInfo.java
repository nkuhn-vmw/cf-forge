package com.cfforge.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BuildpackInfo(String guid, String name, String stack, int position, boolean enabled) {}
