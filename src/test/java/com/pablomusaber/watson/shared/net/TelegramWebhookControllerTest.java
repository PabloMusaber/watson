package com.pablomusaber.watson.shared.net;

import com.pablomusaber.watson.shared.channel.Utterance;
import com.pablomusaber.watson.shared.channel.UtteranceDispatcher;
import com.pablomusaber.watson.shared.telegram.TelegramProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class TelegramWebhookControllerTest {

    private static final String SECRET = "webhook-secret";
    private static final String ALLOWED = "111111111";

    private final UtteranceDispatcher dispatcher = mock(UtteranceDispatcher.class);
    private final TelegramProperties properties = new TelegramProperties(
            "bot-token", "reply-chat-id", List.of(ALLOWED), SECRET);
    private final TelegramWebhookController controller = new TelegramWebhookController(dispatcher, properties);

    @Test
    void allowlistedSenderIsDispatched() {
        byte[] body = payload(ALLOWED, "hello watson");

        ResponseEntity<Void> response = controller.receive(body, SECRET);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(dispatcher).dispatch(any(Utterance.class));
    }

    @Test
    void nonAllowlistedSenderIsIgnored() {
        byte[] body = payload("222222222", "hello watson");

        ResponseEntity<Void> response = controller.receive(body, SECRET);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(dispatcher, never()).dispatch(any());
    }

    @Test
    void invalidSecretTokenIsRejected() {
        byte[] body = payload(ALLOWED, "hello watson");

        ResponseEntity<Void> response = controller.receive(body, "wrong-secret");

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        verify(dispatcher, never()).dispatch(any());
    }

    private static byte[] payload(String chatId, String text) {
        return ("{\"message\":{\"chat\":{\"id\":" + chatId + "},\"text\":\"" + text + "\"}}")
                .getBytes(StandardCharsets.UTF_8);
    }
}
