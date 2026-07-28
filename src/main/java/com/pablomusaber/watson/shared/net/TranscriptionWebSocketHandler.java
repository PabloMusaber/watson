package com.pablomusaber.watson.shared.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pablomusaber.watson.shared.channel.Utterance;
import com.pablomusaber.watson.shared.channel.UtteranceDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class TranscriptionWebSocketHandler extends TextWebSocketHandler {

    private static final String CHANNEL_ID = "sidecar";

    private final ObjectMapper mapper = new ObjectMapper();
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final UtteranceDispatcher dispatcher;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("sidecar connected: id={}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("sidecar disconnected: id={} status={}", session.getId(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode node = mapper.readTree(message.getPayload());
            String text = node.path("text").asText();
            if (text.isBlank()) return;
            Utterance u = Utterance.fromMic(text, CHANNEL_ID);
            dispatcher.dispatch(u);
        } catch (Exception e) {
            log.warn("failed to handle ws message: {}", e.getMessage());
        }
    }

    public void broadcastTts(String text) {
        try {
            String payload = mapper.writeValueAsString(Map.of("type", "tts", "text", text));
            TextMessage msg = new TextMessage(payload);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(msg);
                    } catch (IOException e) {
                        log.warn("failed to send TTS to session {}: {}", session.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("failed to broadcast TTS message: {}", e.getMessage());
        }
    }
}
