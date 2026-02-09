package com.cfforge.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.util.Map;

public class ManifestUtils {

    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    public static String toYaml(Map<String, Object> manifest) {
        try {
            return yamlMapper.writeValueAsString(manifest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize manifest to YAML", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> fromYaml(String yaml) {
        try {
            return yamlMapper.readValue(yaml, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse manifest YAML", e);
        }
    }

    public static int parseMemory(String memory) {
        if (memory == null) return 1024;
        String upper = memory.toUpperCase().trim();
        if (upper.endsWith("G")) {
            return (int) (Double.parseDouble(upper.replace("G", "")) * 1024);
        } else if (upper.endsWith("M")) {
            return Integer.parseInt(upper.replace("M", ""));
        }
        return Integer.parseInt(upper);
    }
}
