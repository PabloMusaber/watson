package com.pablomusaber.watson.watson;

import com.pablomusaber.watson.shared.channel.ChannelReply;

public record WatsonReply(String text) implements ChannelReply {
}
