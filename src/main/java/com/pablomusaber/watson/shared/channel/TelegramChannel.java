package com.pablomusaber.watson.shared.channel;

import com.pablomusaber.watson.shared.telegram.TelegramClient;
import com.pablomusaber.watson.shared.telegram.TelegramProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramChannel implements Channel {

    private final TelegramClient client;
    private final TelegramProperties properties;

    @Override
    public String id() {
        return "telegram";
    }

    @Override
    public boolean supportsAudioOut() {
        return false;
    }

    @Override
    public void reply(String text) {
        client.sendMessage(properties.replyChatId(), text);
    }
}
