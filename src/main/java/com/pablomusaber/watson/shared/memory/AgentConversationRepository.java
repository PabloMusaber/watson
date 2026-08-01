package com.pablomusaber.watson.shared.memory;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentConversationRepository extends JpaRepository<AgentConversationHistory, Long> {

    List<AgentConversationHistory> findByAgentOrderByTsDesc(String agent, Pageable pageable);

    List<AgentConversationHistory> findByAgentAndTsGreaterThanEqualOrderByTsAsc(String agent, String cutoff);
}
