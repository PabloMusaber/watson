package com.pablomusaber.watson.shared.memory;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "conversation_message")
public class ConversationMessage {

    public enum Role {
        USER, AGENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private String messageId;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private String agent;

    @Column(name = "channel_id", nullable = false)
    private String channelId;

    @Column(nullable = false)
    private String ts;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    public ConversationMessage(String messageId, String sessionId, Role role, String agent, String channelId,
                                String ts, String text) {
        this.messageId = messageId;
        this.sessionId = sessionId;
        this.role = role;
        this.agent = agent;
        this.channelId = channelId;
        this.ts = ts;
        this.text = text;
    }
}
