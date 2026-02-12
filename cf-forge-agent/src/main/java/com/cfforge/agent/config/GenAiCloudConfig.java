package com.cfforge.agent.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

@Configuration
@Profile("cloud")
public class GenAiCloudConfig {

    private static final Logger log = LoggerFactory.getLogger(GenAiCloudConfig.class);

    private static final String DEFAULT_CHAT_MODEL = "openai/gpt-oss-20b";
    private static final String DEFAULT_EMBEDDING_MODEL = "nomic-embed-text-v2-moe-v1032";

    @Bean
    @Primary
    public ChatModel genAiChatModel(ToolCallingManager toolCallingManager) {
        GenAiCredentials creds = parseGenAiCredentials();
        if (creds == null) {
            throw new IllegalStateException("No GenAI service binding found in VCAP_SERVICES");
        }

        String chatModelName = discoverChatModelName(creds);
        String openAiBase = creds.apiBase + "/openai";
        log.info("Creating ChatModel with GenAI service: model={}, apiBase={}", chatModelName, openAiBase);

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(openAiBase)
                .apiKey(creds.apiKey)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(chatModelName)
                        .temperature(0.2)
                        .build())
                .toolCallingManager(toolCallingManager)
                .build();
    }

    @Bean
    @Primary
    public EmbeddingModel genAiEmbeddingModel() {
        GenAiCredentials creds = parseGenAiCredentials();
        if (creds == null) {
            throw new IllegalStateException("No GenAI service binding found in VCAP_SERVICES");
        }

        String embeddingModelName = discoverEmbeddingModelName(creds);
        String openAiBase = creds.apiBase + "/openai";
        log.info("Creating EmbeddingModel with GenAI service: model={}, apiBase={}", embeddingModelName, openAiBase);

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(openAiBase)
                .apiKey(creds.apiKey)
                .build();

        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(embeddingModelName)
                .build();

        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options);
    }

    private String discoverChatModelName(GenAiCredentials creds) {
        if (creds.configUrl != null) {
            try {
                String json = RestClient.builder().build()
                        .get()
                        .uri(creds.configUrl)
                        .header("Authorization", "Bearer " + creds.apiKey)
                        .retrieve()
                        .body(String.class);

                JsonNode config = new ObjectMapper().readTree(json);
                JsonNode models = config.path("advertisedModels");
                if (models.isArray()) {
                    for (JsonNode model : models) {
                        JsonNode caps = model.path("capabilities");
                        if (caps.isArray()) {
                            for (JsonNode cap : caps) {
                                if ("CHAT".equals(cap.asText())) {
                                    String name = model.path("name").asText();
                                    log.info("Discovered chat model from config_url: {}", name);
                                    return name;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to discover models from config_url {}: {}", creds.configUrl, e.getMessage());
            }
        }
        log.info("Using default chat model: {}", DEFAULT_CHAT_MODEL);
        return DEFAULT_CHAT_MODEL;
    }

    private String discoverEmbeddingModelName(GenAiCredentials creds) {
        if (creds.configUrl != null) {
            try {
                String json = RestClient.builder().build()
                        .get()
                        .uri(creds.configUrl)
                        .header("Authorization", "Bearer " + creds.apiKey)
                        .retrieve()
                        .body(String.class);

                JsonNode config = new ObjectMapper().readTree(json);
                JsonNode models = config.path("advertisedModels");
                if (models.isArray()) {
                    for (JsonNode model : models) {
                        JsonNode caps = model.path("capabilities");
                        if (caps.isArray()) {
                            for (JsonNode cap : caps) {
                                if ("EMBEDDING".equals(cap.asText())) {
                                    String name = model.path("name").asText();
                                    log.info("Discovered embedding model from config_url: {}", name);
                                    return name;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to discover embedding models from config_url {}: {}", creds.configUrl, e.getMessage());
            }
        }
        log.info("Using default embedding model: {}", DEFAULT_EMBEDDING_MODEL);
        return DEFAULT_EMBEDDING_MODEL;
    }

    private GenAiCredentials parseGenAiCredentials() {
        String vcapServices = System.getenv("VCAP_SERVICES");
        if (vcapServices == null || vcapServices.isEmpty()) {
            log.warn("VCAP_SERVICES environment variable not found");
            return null;
        }

        try {
            JsonNode vcap = new ObjectMapper().readTree(vcapServices);
            JsonNode genaiBindings = vcap.path("genai");
            if (!genaiBindings.isArray() || genaiBindings.isEmpty()) {
                log.warn("No 'genai' service bindings found in VCAP_SERVICES");
                return null;
            }

            JsonNode creds = genaiBindings.get(0).path("credentials");
            JsonNode endpoint = creds.path("endpoint");

            if (!endpoint.isMissingNode()) {
                String apiBase = endpoint.path("api_base").asText(null);
                String apiKey = endpoint.path("api_key").asText(null);
                String configUrl = endpoint.path("config_url").asText(null);

                if (apiBase != null && apiKey != null) {
                    log.info("Parsed GenAI credentials: apiBase={}, configUrl={}", apiBase, configUrl);
                    return new GenAiCredentials(apiBase, apiKey, configUrl);
                }
            }

            // Fallback: flat credentials
            String apiBase = creds.path("api_base").asText(null);
            String apiKey = creds.path("api_key").asText(null);
            if (apiBase != null && apiKey != null) {
                return new GenAiCredentials(apiBase, apiKey, null);
            }

            log.warn("GenAI credentials missing api_base or api_key");
            return null;
        } catch (Exception e) {
            log.error("Failed to parse VCAP_SERVICES: {}", e.getMessage(), e);
            return null;
        }
    }

    private record GenAiCredentials(String apiBase, String apiKey, String configUrl) {}
}
