package com.pablomusaber.watson.english_tutor;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pablomusaber.watson.english_tutor.domain.TutorAnalysis;
import com.pablomusaber.watson.shared.JsonUtils;
import com.pablomusaber.watson.shared.PromptLoader;
import com.pablomusaber.watson.shared.channel.Utterance;
import com.pablomusaber.watson.shared.memory.ConversationMessageStore;
import com.pablomusaber.watson.shared.memory.LongTermMemoryStore;
import com.pablomusaber.watson.shared.memory.MemoryExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Agent(description = "English tutor for a Spanish native speaker: corrects grammar and vocabulary mistakes and helps "
        + "practice English conversation. Use for messages written in English for practice, questions about English "
        + "grammar or vocabulary, or explicit requests for English-learning help.")
@RequiredArgsConstructor
public class EnglishTutorAgent {

    private static final String AGENT_NAME = EnglishTutorAgent.class.getSimpleName();
    private static final Duration HISTORY_WINDOW = Duration.ofMinutes(5);

    private final ConversationMessageStore history;
    private final LongTermMemoryStore memoryStore;
    private final MemoryExtractionService memoryExtractor;
    private final ObjectMapper mapper;
    private final PromptLoader promptLoader;

    @Value("classpath:english-tutor/prompts/system.st")
    private Resource systemPrompt;

    @Value("classpath:english-tutor/prompts/analyze-utterance.st")
    private Resource analyzeUtterancePrompt;

    @AchievesGoal(description = "English tutoring feedback given.")
    @Action
    public TutorAnalysis process(Utterance u, Ai ai) {
        String recentContext = history.window(AGENT_NAME, history.currentSessionId(u.channelId()), HISTORY_WINDOW);
        String memoryBlock = memoryStore.formatForPrompt();
        String prompt = promptLoader.render(analyzeUtterancePrompt, Map.of(
                "systemPrompt", promptLoader.load(systemPrompt),
                "memoryBlock", memoryBlock,
                "windowMinutes", HISTORY_WINDOW.toMinutes(),
                "recentContext", recentContext,
                "utterance", u.text()));
        String raw = ai.withDefaultLlm().generateText(prompt);
        TutorAnalysis analysis = parseAnalysis(raw);

        memoryExtractor.extract("User: \"%s\"\nTutor: \"%s\"".formatted(u.text(), analysis.responseText()));

        return analysis;
    }

    private TutorAnalysis parseAnalysis(String raw) {
        try {
            return mapper.readValue(JsonUtils.stripFences(raw), TutorAnalysis.class);
        } catch (JsonProcessingException e) {
            log.warn("analysis JSON parse failed; using raw text as response. raw={}", raw, e);
            return new TutorAnalysis(List.of(), List.of(), raw.trim());
        }
    }
}
