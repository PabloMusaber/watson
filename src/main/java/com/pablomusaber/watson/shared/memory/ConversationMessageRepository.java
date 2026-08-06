package com.pablomusaber.watson.shared.memory;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    Optional<ConversationMessage> findTopByChannelIdOrderByTsDesc(String channelId);

    List<ConversationMessage> findByMessageId(String messageId);

    List<ConversationMessage> findByRoleAndAgentAndSessionIdAndTsGreaterThanEqualOrderByTsAsc(
            ConversationMessage.Role role, String agent, String sessionId, String cutoff);

    List<ConversationMessage> findByRoleAndAgentAndSessionIdOrderByTsDesc(
            ConversationMessage.Role role, String agent, String sessionId, Pageable pageable);
}
