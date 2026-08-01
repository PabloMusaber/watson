package com.pablomusaber.watson.shared.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pablomusaber.watson.shared.channel.Utterance;
import com.pablomusaber.watson.shared.channel.UtteranceDispatcher;
import com.pablomusaber.watson.shared.telegram.TelegramProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Slf4j
@RestController
@RequestMapping("/webhooks/telegram")
@RequiredArgsConstructor
public class TelegramWebhookController {

    private static final String CHANNEL_ID = "telegram";
    private static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private final ObjectMapper mapper = new ObjectMapper();
    private final UtteranceDispatcher dispatcher;
    private final TelegramProperties properties;

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody byte[] rawBody,
                                        @RequestHeader(value = SECRET_HEADER, required = false) String secretToken) {
        if (!verifySecret(secretToken, properties.webhookSecret())) {
            log.warn("rejecting telegram webhook: invalid secret token");
            return ResponseEntity.status(401).build();
        }

        JsonNode node;
        try {
            node = mapper.readTree(rawBody);
        } catch (Exception e) {
            log.warn("failed to parse telegram webhook body: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        JsonNode message = node.path("message");
        String from = message.path("chat").path("id").asText();
        String text = message.path("text").asText();
        if (!properties.allowedIds().contains(from) || text.isBlank()) {
            log.info("ignoring telegram message from non-allowlisted or empty sender: {}", from);
            return ResponseEntity.ok().build();
        }

        dispatcher.dispatch(Utterance.fromTyped(text, CHANNEL_ID));
        return ResponseEntity.ok().build();
    }

    private static boolean verifySecret(String header, String secret) {
        if (header == null || secret == null) {
            return false;
        }
        return MessageDigest.isEqual(
                header.getBytes(StandardCharsets.UTF_8),
                secret.getBytes(StandardCharsets.UTF_8));
    }
}
