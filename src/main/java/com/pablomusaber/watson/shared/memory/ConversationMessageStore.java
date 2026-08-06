package com.pablomusaber.watson.shared.memory;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationMessageStore {

    private final ConversationMessageRepository repository;

    @Value("${history.session-timeout-minutes}")
    private long sessionTimeoutMinutes;

    /** Write path: reuses the channel's current session if still within the idle timeout, else mints a new one. */
    public String resolveSessionId(String channelId) {
        return repository.findTopByChannelIdOrderByTsDesc(channelId)
                .filter(latest -> Duration.between(Instant.parse(latest.getTs()), Instant.now())
                        .compareTo(Duration.ofMinutes(sessionTimeoutMinutes)) <= 0)
                .map(ConversationMessage::getSessionId)
                .orElseGet(() -> UUID.randomUUID().toString());
    }

    /** Read path: the session the channel's most recent message already belongs to. No minting, no timeout check. */
    public String currentSessionId(String channelId) {
        return repository.findTopByChannelIdOrderByTsDesc(channelId)
                .map(ConversationMessage::getSessionId)
                .orElseGet(() -> UUID.randomUUID().toString());
    }

    public void logUserMessage(String messageId, String sessionId, String agent, String channelId, String text) {
        repository.save(new ConversationMessage(messageId, sessionId, ConversationMessage.Role.USER, agent,
                channelId, Instant.now().toString(), text));
    }

    public void logAgentResponse(String messageId, String sessionId, String agent, String channelId, String text) {
        repository.save(new ConversationMessage(messageId, sessionId, ConversationMessage.Role.AGENT, agent,
                channelId, Instant.now().toString(), text));
    }

    /** Chronological "[ts] utterance -> response" lines within the given window, oldest first. Unpaired (failed) USER rows are skipped. */
    public String window(String agent, String sessionId, Duration duration) {
        String cutoff = Instant.now().minus(duration).toString();
        List<ConversationMessage> userRows = repository.findByRoleAndAgentAndSessionIdAndTsGreaterThanEqualOrderByTsAsc(
                ConversationMessage.Role.USER, agent, sessionId, cutoff);
        List<ConversationMessage> agentRows = repository.findByRoleAndAgentAndSessionIdAndTsGreaterThanEqualOrderByTsAsc(
                ConversationMessage.Role.AGENT, agent, sessionId, cutoff);
        Map<String, ConversationMessage> responsesByMessageId = agentRows.stream()
                .collect(Collectors.toMap(ConversationMessage::getMessageId, r -> r, (a, b) -> a));

        StringBuilder sb = new StringBuilder();
        for (ConversationMessage userRow : userRows) {
            ConversationMessage responseRow = responsesByMessageId.get(userRow.getMessageId());
            if (responseRow == null) {
                continue;
            }
            sb.append("[").append(userRow.getTs()).append("] ").append(userRow.getText())
                    .append(" -> ").append(responseRow.getText()).append("\n");
        }
        return sb.toString();
    }

    /** Last n complete exchanges formatted as "User: ...\nAgent: ...", oldest first. */
    public String lastN(String agent, String sessionId, int n) {
        List<ConversationMessage> agentRows = repository.findByRoleAndAgentAndSessionIdOrderByTsDesc(
                ConversationMessage.Role.AGENT, agent, sessionId, PageRequest.of(0, n));
        Collections.reverse(agentRows);

        List<String> lines = new ArrayList<>();
        for (ConversationMessage responseRow : agentRows) {
            String userText = repository.findByMessageId(responseRow.getMessageId()).stream()
                    .filter(m -> m.getRole() == ConversationMessage.Role.USER)
                    .findFirst()
                    .map(ConversationMessage::getText)
                    .orElse("");
            lines.add("User: " + userText + "\nAgent: " + responseRow.getText());
        }
        return String.join("\n\n", lines);
    }
}
