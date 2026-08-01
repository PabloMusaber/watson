package com.pablomusaber.watson.knowledge_agent;

import com.pablomusaber.watson.shared.channel.ChannelReply;

public record KnowledgeAgentResult(String content) implements ChannelReply {

    @Override
    public String text() {
        return content;
    }
}
