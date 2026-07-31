package com.pablomusaber.watson.shared.net;

import com.pablomusaber.watson.shared.channel.Utterance;
import com.pablomusaber.watson.shared.channel.UtteranceDispatcher;
import com.pablomusaber.watson.shared.openwa.OpenWaProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class WhatsAppWebhookControllerTest {

    private static final String SECRET = "webhook-secret";
    private static final String ALLOWED = "5491111111111@c.us";

    private final UtteranceDispatcher dispatcher = mock(UtteranceDispatcher.class);
    private final OpenWaProperties properties = new OpenWaProperties(
            "http://localhost:2785", "api-key", "session-id", SECRET, ALLOWED, List.of(ALLOWED));
    private final WhatsAppWebhookController controller = new WhatsAppWebhookController(dispatcher, properties);

    @Test
    void allowlistedSenderIsDispatched() {
        byte[] body = payload(ALLOWED, "hello watson");

        ResponseEntity<Void> response = controller.receive(body, sign(body));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(dispatcher).dispatch(any(Utterance.class));
    }

    @Test
    void nonAllowlistedSenderIsIgnored() {
        byte[] body = payload("5490000000000@c.us", "hello watson");

        ResponseEntity<Void> response = controller.receive(body, sign(body));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(dispatcher, never()).dispatch(any());
    }

    @Test
    void invalidSignatureIsRejected() {
        byte[] body = payload(ALLOWED, "hello watson");

        ResponseEntity<Void> response = controller.receive(body, "sha256=deadbeef");

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        verify(dispatcher, never()).dispatch(any());
    }

    private static byte[] payload(String from, String text) {
        return ("{\"event\":\"message.received\",\"from\":\"" + from + "\",\"body\":\"" + text + "\"}")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static String sign(byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
