package com.cfforge.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cf.forge.sso")
public record SsoProperties(
    String clientId,
    String clientSecret,
    String issuerUri,
    String scopes
) {}
