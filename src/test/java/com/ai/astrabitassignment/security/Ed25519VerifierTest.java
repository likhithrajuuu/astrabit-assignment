package com.ai.astrabitassignment.security;

import com.google.crypto.tink.subtle.Ed25519Sign;
import com.google.crypto.tink.subtle.Hex;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class Ed25519VerifierTest {

    private static final String TIMESTAMP = "1700000000";
    private static final byte[] BODY = "{\"type\":1}".getBytes(StandardCharsets.UTF_8);

    @Test
    void acceptsAValidSignature() throws Exception {
        Ed25519Sign.KeyPair keyPair = Ed25519Sign.KeyPair.newKeyPair();
        Ed25519Verifier verifier = new Ed25519Verifier(Hex.encode(keyPair.getPublicKey()));

        String signature = sign(keyPair, TIMESTAMP, BODY);

        assertThat(verifier.verify(signature, TIMESTAMP, BODY)).isTrue();
    }

    @Test
    void rejectsATamperedBody() throws Exception {
        Ed25519Sign.KeyPair keyPair = Ed25519Sign.KeyPair.newKeyPair();
        Ed25519Verifier verifier = new Ed25519Verifier(Hex.encode(keyPair.getPublicKey()));

        String signature = sign(keyPair, TIMESTAMP, BODY);
        byte[] tamperedBody = "{\"type\":2}".getBytes(StandardCharsets.UTF_8);

        assertThat(verifier.verify(signature, TIMESTAMP, tamperedBody)).isFalse();
    }

    @Test
    void rejectsATamperedTimestamp() throws Exception {
        Ed25519Sign.KeyPair keyPair = Ed25519Sign.KeyPair.newKeyPair();
        Ed25519Verifier verifier = new Ed25519Verifier(Hex.encode(keyPair.getPublicKey()));

        String signature = sign(keyPair, TIMESTAMP, BODY);

        assertThat(verifier.verify(signature, "1700000001", BODY)).isFalse();
    }

    @Test
    void rejectsASignatureFromADifferentKey() throws Exception {
        Ed25519Sign.KeyPair signingKeyPair = Ed25519Sign.KeyPair.newKeyPair();
        Ed25519Sign.KeyPair otherKeyPair = Ed25519Sign.KeyPair.newKeyPair();
        Ed25519Verifier verifier = new Ed25519Verifier(Hex.encode(otherKeyPair.getPublicKey()));

        String signature = sign(signingKeyPair, TIMESTAMP, BODY);

        assertThat(verifier.verify(signature, TIMESTAMP, BODY)).isFalse();
    }

    @Test
    void rejectsMalformedHexInsteadOfThrowing() throws Exception {
        Ed25519Sign.KeyPair keyPair = Ed25519Sign.KeyPair.newKeyPair();
        Ed25519Verifier verifier = new Ed25519Verifier(Hex.encode(keyPair.getPublicKey()));

        assertThat(verifier.verify("not-hex", TIMESTAMP, BODY)).isFalse();
    }

    private String sign(Ed25519Sign.KeyPair keyPair, String timestamp, byte[] body) throws Exception {
        Ed25519Sign signer = new Ed25519Sign(keyPair.getPrivateKey());

        byte[] timestampBytes = timestamp.getBytes(StandardCharsets.UTF_8);
        byte[] message = new byte[timestampBytes.length + body.length];
        System.arraycopy(timestampBytes, 0, message, 0, timestampBytes.length);
        System.arraycopy(body, 0, message, timestampBytes.length, body.length);

        return Hex.encode(signer.sign(message));
    }
}
