package com.pablomusaber.watson.knowledge_agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.pablomusaber.watson.shared.PromptLoader;
import com.pablomusaber.watson.shared.channel.Utterance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.util.Map;

@Slf4j
@Agent(description = "Accesses the user's personal Obsidian knowledge base (SecondBrain vault) to search, read, "
        + "create, and update notes. Use when the user asks to look something up in their notes, save or remember "
        + "something to their vault, or manage their knowledge base.")
@RequiredArgsConstructor
public class KnowledgeAgent {

    private final ObsidianCliWrapper obsidian;
    private final PromptLoader promptLoader;

    @Value("classpath:knowledge-agent/prompts/system.st")
    private Resource systemPrompt;

    @Value("classpath:knowledge-agent/prompts/handle.st")
    private Resource handlePrompt;

    @Action
    @AchievesGoal(description = "Knowledge base query handled.")
    public KnowledgeAgentResult handle(Utterance u, Ai ai) {
        log.info("KnowledgeAgent handling: {}", u.text());

        String prompt = promptLoader.render(handlePrompt, Map.of(
                "systemPrompt", promptLoader.load(systemPrompt),
                "utterance", u.text()));

        String result = ai.withLlmByRole("knowledge")
                .withToolObject(obsidian)
                .generateText(prompt);

        log.debug("KnowledgeAgent response length: {}", result.length());
        return new KnowledgeAgentResult(result);
    }
}
