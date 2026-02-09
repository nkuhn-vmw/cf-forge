package com.cfforge.agent.model;

public record ServiceRecommendation(
    String serviceName,
    String plan,
    String reason,
    String bindingName
) {}
