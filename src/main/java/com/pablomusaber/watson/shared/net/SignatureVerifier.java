package com.pablomusaber.watson.shared.net;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;


@Slf4j
public final class SignatureVerifier {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String PREFIX = "sha256=";

    private SignatureVerifier() {
    }

    public static boolean verify(byte[] rawBody, String signatureHeader, String secret) {
        if (signatureHeader == null || !signatureHeader.startsWith(PREFIX)) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] expected = mac.doFinal(rawBody);
            byte[] actual = HexFormat.of().parseHex(signatureHeader.substring(PREFIX.length()));
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception e) {
            log.warn("signature check failed: {}", e.toString());
            return false;
        }
    }
}
