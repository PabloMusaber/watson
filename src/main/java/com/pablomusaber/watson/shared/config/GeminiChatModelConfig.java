package com.pablomusaber.watson.shared.config;

import com.google.genai.Client;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Watson's Embabel-native LLM wiring (embabel-agent-starter-google-genai) doesn't expose a plain
 * Spring AI ChatClient bean, but MemoryExtractionService needs one for non-agentic async calls.
 * Reuses the same Gemini native Google GenAI credentials as the Embabel agent platform.
 */
@Configuration(proxyBeanMethods = false)
public class GeminiChatModelConfig {

    private static final String DEFAULT_MODEL = "gemini-2.5-flash";

    @Bean
    @ConditionalOnMissingBean(ChatClient.class)
    ChatClient geminiChatClient(
            @Value("${embabel.agent.platform.models.googlegenai.api-key:#{null}}") String propsApiKey,
            @Value("${GOOGLE_API_KEY:#{null}}") String envApiKey,
            @Value("${embabel.models.default-llm:#{null}}") String defaultLlm,
            ObjectProvider<ObservationRegistry> observationRegistry) {

        String apiKey = requireText(
                firstNonBlank(propsApiKey, envApiKey),
                "Google GenAI API key required: set GOOGLE_API_KEY env var or embabel.agent.platform.models.googlegenai.api-key");

        String model = firstNonBlank(defaultLlm, DEFAULT_MODEL);

        Client genAiClient = Client.builder()
                .apiKey(apiKey)
                .build();

        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                .model(model)
                .temperature(0.0)
                .thinkingBudget(0)
                .build();

        ChatModel chatModel = GoogleGenAiChatModel.builder()
                .genAiClient(genAiClient)
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
