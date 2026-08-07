package com.pablomusaber.watson.shared.config;

import com.embabel.agent.spi.LlmService;
import com.embabel.common.ai.model.AiModel;
import com.embabel.common.ai.model.ModelProvider;
import com.embabel.common.ai.model.ModelSelectionCriteria;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Watson's Embabel-native LLM wiring doesn't expose a plain Spring AI ChatClient bean, but
 * MemoryExtractionService needs one for non-agentic async calls. Looks up the model registered
 * for the "memory-extraction" role through Embabel's ModelProvider, so it follows whichever
 * provider (Gemini or OpenRouter) that role is configured to use.
 */
@Configuration(proxyBeanMethods = false)
public class GeminiChatModelConfig {

    @Bean
    @ConditionalOnMissingBean(ChatClient.class)
    ChatClient memoryExtractionChatClient(ModelProvider modelProvider) {
        LlmService<?> llm = modelProvider.getLlm(ModelSelectionCriteria.byRole("memory-extraction"));
        if (!(llm instanceof AiModel<?> aiModel) || !(aiModel.getModel() instanceof ChatModel chatModel)) {
            throw new IllegalStateException(
                    "LLM registered for role 'memory-extraction' does not expose a Spring AI ChatModel: "
                            + llm.getName());
        }

        return ChatClient.builder(chatModel)
                .defaultOptions(ChatOptions.builder().temperature(0.0).build())
                .build();
    }
}
