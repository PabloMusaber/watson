package com.pablomusaber.watson.shared.channel;

import com.pablomusaber.watson.shared.openwa.OpenWaClient;
import com.pablomusaber.watson.shared.openwa.OpenWaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class WhatsAppChannel implements Channel {

    private final OpenWaClient client;
    private final OpenWaProperties properties;

    @Override
    public String id() {
        return "whatsapp";
    }

    @Override
    public boolean supportsAudioOut() {
        return false;
    }

    @Override
    public void reply(String text) {
        client.sendText(properties.replyChatId(), text);
    }
}
