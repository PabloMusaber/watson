package com.pablomusaber.watson.shared.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pablomusaber.watson.shared.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryExtractionService {

    private final LongTermMemoryStore memoryStore;
    private final ObjectMapper mapper;
    private final ChatClient chatClient;

    @Async("memoryExtractionExecutor")
    public void extract(String context) {
        String prompt = """
                Given this exchange or context:
                %s

                Extract any facts about the user worth remembering long-term.
                If the user used any phrasing suggesting they want something remembered
                (e.g. "remember", "don't forget", "keep in mind", "save this"), always include that content.

                Return a JSON array only — no markdown, no prose.
                Each element: {"fact": "...", "category": "<relevant category>"}
                Return [] if nothing is worth remembering.
                """.formatted(context);
        try {
            String raw = chatClient.prompt().user(prompt).call().content();
            List<LongTermMemoryStore.MemoryCandidate> candidates = mapper.readValue(
                    JsonUtils.stripFences(raw),
                    new TypeReference<List<LongTermMemoryStore.MemoryCandidate>>() {});
            candidates.forEach(c -> memoryStore.saveOrUpdate(c.fact(), c.category()));
        } catch (Exception e) {
            log.warn("memory extraction failed; skipping. context={}", context, e);
        }
    }
}
