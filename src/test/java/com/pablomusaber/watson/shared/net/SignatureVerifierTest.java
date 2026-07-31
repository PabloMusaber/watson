package com.pablomusaber.watson.shared.net;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureVerifierTest {

    private static final String SECRET = "shh-its-a-secret";
    private static final String BODY = "{\"event\":\"message.received\",\"from\":\"123@c.us\",\"body\":\"hi\"}";
    // Non-ASCII payload — regression case for the ISO-8859-1/UTF-8 round-trip bug: verifying
    // against a re-encoded String (instead of the original bytes) silently breaks on any
    // multi-byte UTF-8 character, e.g. an accented name or emoji in a real WhatsApp message.
    private static final String NON_ASCII_BODY =
            "{\"event\":\"message.received\",\"from\":\"123@c.us\",\"body\":\"hola José 👋\"}";

    @Test
    void validSignaturePasses() throws Exception {
        String header = "sha256=" + hmac(BODY, SECRET);
        assertTrue(SignatureVerifier.verify(bytes(BODY), header, SECRET));
    }

    @Test
    void validSignaturePassesForNonAsciiBody() throws Exception {
        String header = "sha256=" + hmac(NON_ASCII_BODY, SECRET);
        assertTrue(SignatureVerifier.verify(bytes(NON_ASCII_BODY), header, SECRET));
    }

    @Test
    void tamperedBodyFails() throws Exception {
        String header = "sha256=" + hmac(BODY, SECRET);
        assertFalse(SignatureVerifier.verify(bytes(BODY + "tampered"), header, SECRET));
    }

    @Test
    void wrongSecretFails() throws Exception {
        String header = "sha256=" + hmac(BODY, "wrong-secret");
        assertFalse(SignatureVerifier.verify(bytes(BODY), header, SECRET));
    }

    @Test
    void missingHeaderFails() {
        assertFalse(SignatureVerifier.verify(bytes(BODY), null, SECRET));
    }

    @Test
    void malformedHeaderFails() {
        assertFalse(SignatureVerifier.verify(bytes(BODY), "not-a-valid-header", SECRET));
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static String hmac(String body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }
}
