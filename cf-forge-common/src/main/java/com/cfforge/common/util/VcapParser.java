package com.cfforge.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

public class VcapParser {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Optional<JsonNode> getServiceCredentials(String vcapServices, String serviceName) {
        try {
            JsonNode vcap = mapper.readTree(vcapServices);
            for (var entry : (Iterable<java.util.Map.Entry<String, JsonNode>>) vcap::fields) {
                for (JsonNode binding : entry.getValue()) {
                    if (serviceName.equals(binding.path("name").asText())) {
                        return Optional.of(binding.path("credentials"));
                    }
                }
            }
        } catch (Exception e) {
            // Not on CF or invalid VCAP_SERVICES
        }
        return Optional.empty();
    }

    public static Optional<String> getCredential(String vcapServices, String serviceName, String key) {
        return getServiceCredentials(vcapServices, serviceName)
            .map(creds -> creds.path(key).asText(null));
    }
}
