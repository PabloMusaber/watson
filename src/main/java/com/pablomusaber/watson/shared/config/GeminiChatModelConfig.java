package com.pablomusaber.watson.shared.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Watson's Embabel-native LLM wiring (embabel-agent-starter-gemini) doesn't expose a plain
 * Spring AI ChatClient bean, but MemoryExtractionService needs one for non-agentic async calls.
 * Reuses the same Gemini OpenAI-compatible endpoint and API key as the Embabel agent platform.
 */
@Configuration(proxyBeanMethods = false)
public class GeminiChatModelConfig {

    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai";
    private static final String DEFAULT_MODEL = "gemini-2.5-flash";
    private static final String DEFAULT_COMPLETIONS_PATH = "/chat/completions";

    @Bean
    @ConditionalOnMissingBean(ChatClient.class)
    ChatClient geminiChatClient(
            @Value("${embabel.agent.platform.models.gemini.api-key:#{null}}") String propsApiKey,
            @Value("${GEMINI_API_KEY:#{null}}") String envApiKey,
            @Value("${embabel.models.default-llm:#{null}}") String defaultLlm,
            ObjectProvider<ObservationRegistry> observationRegistry) {

        String apiKey = requireText(
                firstNonBlank(propsApiKey, envApiKey),
                "Gemini API key required: set GEMINI_API_KEY env var or embabel.agent.platform.models.gemini.api-key");

        String model = firstNonBlank(defaultLlm, DEFAULT_MODEL);

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(DEFAULT_BASE_URL)
                .apiKey(apiKey)
                .completionsPath(DEFAULT_COMPLETIONS_PATH)
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(0.0)
                .extraBody(Map.of("thinkingConfig", Map.of("thinkingBudget", 0)))
                .build();

        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .build();

        return ChatClient.builder(chatModel).build();
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (StringUtils.hasText(v)) return v.trim();
        }
        return null;
    }

    private static String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) throw new IllegalStateException(message);
        return value.trim();
    }
}
