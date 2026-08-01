package com.pablomusaber.watson.it_news_agent;

import com.pablomusaber.watson.shared.channel.ChannelReply;

public record NewsDigest(String content) implements ChannelReply {

    @Override
    public String text() {
        return content;
    }
}
