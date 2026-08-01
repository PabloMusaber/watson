package com.pablomusaber.watson.shared.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class TelegramClient {

    private final RestClient restClient;
    private final TelegramProperties properties;

    public TelegramClient(TelegramProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.telegram.org")
                .build();
    }

    public void sendMessage(String chatId, String text) {
        try {
            restClient.post()
                    .uri("/bot{token}/sendMessage", properties.botToken())
                    .body(Map.of("chat_id", chatId, "text", text, "parse_mode", "HTML"))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("failed to send telegram message to {}: {}", chatId, e.getMessage(), e);
        }
    }
}
