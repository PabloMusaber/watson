package com.pablomusaber.watson.shared.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pablomusaber.watson.shared.channel.Utterance;
import com.pablomusaber.watson.shared.channel.UtteranceDispatcher;
import com.pablomusaber.watson.shared.openwa.OpenWaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/webhooks/openwa")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private static final String CHANNEL_ID = "whatsapp";

    private final ObjectMapper mapper = new ObjectMapper();
    private final UtteranceDispatcher dispatcher;
    private final OpenWaProperties properties;

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody byte[] rawBody,
                                        @RequestHeader(value = "X-OpenWA-Signature", required = false) String signature) {
        if (!SignatureVerifier.verify(rawBody, signature, properties.webhookSecret())) {
            log.warn("rejecting whatsapp webhook: invalid signature");
            return ResponseEntity.status(401).build();
        }

        JsonNode node;
        try {
            node = mapper.readTree(rawBody);
        } catch (Exception e) {
            log.warn("failed to parse whatsapp webhook body: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        if (!"message.received".equals(node.path("event").asText())) {
            return ResponseEntity.ok().build();
        }

        JsonNode data = node.path("data");
        String from = data.path("from").asText();
        String text = data.path("body").asText();
        if (!properties.allowedIds().contains(from) || text.isBlank()) {
            log.info("ignoring whatsapp message from non-allowlisted or empty sender: {}", from);
            return ResponseEntity.ok().build();
        }

        dispatcher.dispatch(Utterance.fromTyped(text, CHANNEL_ID));
        return ResponseEntity.ok().build();
    }
}
