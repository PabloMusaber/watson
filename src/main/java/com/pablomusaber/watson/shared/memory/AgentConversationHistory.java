package com.pablomusaber.watson.shared.memory;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "agent_conversation_history")
public class AgentConversationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String agent;

    @Column(nullable = false)
    private String ts;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String utterance;

    @Column(columnDefinition = "TEXT")
    private String response;

    public AgentConversationHistory(String agent, String ts, String utterance, String response) {
        this.agent = agent;
        this.ts = ts;
        this.utterance = utterance;
        this.response = response;
    }
}
