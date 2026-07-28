package com.pablomusaber.watson.shared.channel;

import java.time.Instant;

public record Utterance(String text, Source source, String channelId, Instant ts) {

    public static Utterance fromMic(String text, String channelId) {
        return new Utterance(text, Source.MIC, channelId, Instant.now());
    }

    public static Utterance fromTyped(String text, String channelId) {
        return new Utterance(text, Source.TYPED, channelId, Instant.now());
    }
}
