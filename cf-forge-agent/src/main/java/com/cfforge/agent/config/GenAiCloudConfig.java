package com.cfforge.agent.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Configuration
@Profile("cloud")
public class GenAiCloudConfig {

    @Bean
    public static BeanFactoryPostProcessor genAiPropertySource(Environment env) {
        return beanFactory -> {
            try {
                String vcapServices = env.getProperty("VCAP_SERVICES", "{}");
                JsonNode vcap = new ObjectMapper().readTree(vcapServices);

                JsonNode genaiBindings = vcap.path("genai");
                if (genaiBindings.isEmpty() || genaiBindings.isMissingNode()) {
                    genaiBindings = new ObjectMapper().createArrayNode();
                    var userProvided = vcap.path("user-provided");
                    if (!userProvided.isMissingNode()) {
                        for (JsonNode svc : userProvided) {
                            if (svc.path("name").asText().contains("genai")) {
                                ((com.fasterxml.jackson.databind.node.ArrayNode) genaiBindings).add(svc);
                            }
                        }
                    }
                }

                if (!genaiBindings.isEmpty() && genaiBindings.isArray() && genaiBindings.size() > 0) {
                    JsonNode creds = genaiBindings.get(0).path("credentials");
                    System.setProperty("spring.ai.openai.base-url", creds.path("api_base").asText());
                    System.setProperty("spring.ai.openai.api-key", creds.path("api_key").asText());
                    System.setProperty("spring.ai.openai.chat.options.model", creds.path("model").asText());

                    if (creds.has("embedding_model")) {
                        System.setProperty("spring.ai.openai.embedding.options.model",
                            creds.path("embedding_model").asText());
                    }
                }
            } catch (Exception e) {
                // Log and continue - will use default properties
            }
        };
    }
}
