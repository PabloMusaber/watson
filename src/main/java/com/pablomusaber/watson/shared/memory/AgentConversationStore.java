package com.pablomusaber.watson.shared.memory;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentConversationStore {

    private final AgentConversationRepository repository;

    public void append(String agent, String utterance, String response) {
        repository.save(new AgentConversationHistory(agent, Instant.now().toString(), utterance, response));
    }

    /** Chronological "[ts] utterance -> response" lines within the given window, oldest first. */
    public String window(String agent, Duration duration) {
        String cutoff = Instant.now().minus(duration).toString();
        List<AgentConversationHistory> rows = repository
                .findByAgentAndTsGreaterThanEqualOrderByTsAsc(agent, cutoff);
        StringBuilder sb = new StringBuilder();
        for (AgentConversationHistory row : rows) {
            sb.append("[").append(row.getTs()).append("] ").append(row.getUtterance());
            if (row.getResponse() != null) {
                sb.append(" -> ").append(row.getResponse());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /** Last n exchanges formatted as "User: ...\nAgent: ...", oldest first. */
    public String lastN(String agent, int n) {
        List<AgentConversationHistory> rows = repository.findByAgentOrderByTsDesc(agent, PageRequest.of(0, n));
        Collections.reverse(rows);
        return rows.stream()
                .map(r -> "User: " + r.getUtterance() + "\nAgent: " + r.getResponse())
                .collect(Collectors.joining("\n\n"));
    }
}
