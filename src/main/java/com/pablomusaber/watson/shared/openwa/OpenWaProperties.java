package com.pablomusaber.watson.shared.openwa;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "openwa")
public record OpenWaProperties(
        String baseUrl,
        String apiKey,
        String sessionId,
        String webhookSecret,
        String replyChatId,
        List<String> allowedIds
) {
}
