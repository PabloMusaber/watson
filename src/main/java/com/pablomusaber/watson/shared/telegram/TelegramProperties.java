package com.pablomusaber.watson.shared.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "telegram")
public record TelegramProperties(
        String botToken,
        String replyChatId,
        List<String> allowedIds,
        String webhookSecret
) {
}
