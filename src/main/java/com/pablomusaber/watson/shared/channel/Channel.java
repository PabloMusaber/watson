package com.pablomusaber.watson.shared.channel;

public interface Channel {

    String id();

    boolean supportsAudioOut();

    void reply(String text);

    default void speak(String text) {
    }
}
