package com.pablomusaber.watson.shared.channel;

import java.time.Instant;
import java.util.UUID;

public record Utterance(String text, Source source, String channelId, Instant ts, String messageId) {

    public static Utterance fromMic(String text, String channelId) {
        return new Utterance(text, Source.MIC, channelId, Instant.now(), UUID.randomUUID().toString());
    }

    public static Utterance fromTyped(String text, String channelId) {
        return new Utterance(text, Source.TYPED, channelId, Instant.now(), UUID.randomUUID().toString());
    }
}
