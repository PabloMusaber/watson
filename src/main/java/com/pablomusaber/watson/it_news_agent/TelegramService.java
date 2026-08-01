package com.pablomusaber.watson.it_news_agent;

import com.pablomusaber.watson.shared.telegram.TelegramClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TelegramService {

    private final TelegramClient client;
    private final String chatId;

    public TelegramService(TelegramClient client,
                           @Value("${news-agent.telegram.chat-id}") String chatId) {
        this.client = client;
        this.chatId = chatId;
    }

    public void sendMessage(String text) {
        client.sendMessage(chatId, text);
    }
}
