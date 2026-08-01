package com.pablomusaber.watson.financial_agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pablomusaber.watson.financial_agent.domain.FinancialResponse;
import com.pablomusaber.watson.shared.JsonUtils;
import com.pablomusaber.watson.shared.PromptLoader;
import com.pablomusaber.watson.shared.channel.Utterance;
import com.pablomusaber.watson.shared.memory.AgentConversationStore;
import com.pablomusaber.watson.shared.memory.LongTermMemoryStore;
import com.pablomusaber.watson.shared.memory.MemoryExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.util.Map;

@Slf4j
@Agent(description = "Personal financial assistant with live broker account access: answers portfolio holdings, "
        + "balances, market prices, and transaction history questions. Use for anything about money, investments, "
        + "stocks, or the user's broker account.")
@RequiredArgsConstructor
public class FinancialAgent {

    private static final String AGENT_NAME = "financial";

    private final AgentConversationStore conversations;
    private final LongTermMemoryStore memoryStore;
    private final MemoryExtractionService memoryExtractor;
    private final ObjectMapper mapper;
    private final PromptLoader promptLoader;

    @Value("classpath:financial-agent/prompts/system.st")
    private Resource systemPrompt;

    @Value("classpath:financial-agent/prompts/financial-response.st")
    private Resource financialResponsePrompt;

    // Tool group "ppi-broker" is registered via embabel.agent.platform.tools.includes.ppi-broker in application.yml.
    // withToolGroup makes all 9 ppi-broker MCP tools available to the LLM for this action.
    @AchievesGoal(description = "Financial query answered.")
    @Action
    public FinancialResponse answer(Utterance u, Ai ai) {
        if (u.text().trim().length() < 3) {
            return null;
        }

        String recentHistory = conversations.lastN(AGENT_NAME, 5);
        String memoryBlock = memoryStore.formatForPrompt();
        String prompt = buildPrompt(u.text(), recentHistory, memoryBlock);

        String raw = ai.withDefaultLlm().withToolGroup("ppi-broker").generateText(prompt);
        FinancialResponse response = parseResponse(raw);

        conversations.append(AGENT_NAME, u.text(), response.spokenResponse());
        memoryExtractor.extract("User: \"%s\"\nAgent: \"%s\"".formatted(u.text(), response.spokenResponse()));

        return response;
    }

    private String buildPrompt(String utterance, String history, String memoryBlock) {
        String historySection = history.isBlank() ? "(none)" : history;
        return promptLoader.render(financialResponsePrompt, Map.of(
                "systemPrompt", promptLoader.load(systemPrompt),
                "memoryBlock", memoryBlock,
                "recentConversation", historySection,
                "userQuery", utterance));
    }

    private FinancialResponse parseResponse(String raw) {
        try {
            return mapper.readValue(JsonUtils.stripFences(raw), FinancialResponse.class);
        } catch (JsonProcessingException e) {
            log.warn("financial response JSON parse failed; using raw text. raw={}", raw, e);
            return new FinancialResponse(raw.trim(), raw.trim());
        }
    }
}
