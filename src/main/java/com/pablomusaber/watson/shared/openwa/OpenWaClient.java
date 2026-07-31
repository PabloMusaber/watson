package com.pablomusaber.watson.shared.openwa;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class OpenWaClient {

    private final RestClient restClient;
    private final OpenWaProperties properties;

    public OpenWaClient(OpenWaProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("X-API-Key", properties.apiKey())
                .defaultHeader("Connection", "close")
                .build();
    }

    public void sendText(String chatId, String text) {
        try {
            restClient.post()
                    .uri("/api/sessions/{sessionId}/messages/send-text", properties.sessionId())
                    .body(Map.of("chatId", chatId, "text", text))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("failed to send whatsapp message to {}: {}", chatId, e.getMessage(), e);
        }
    }
}
