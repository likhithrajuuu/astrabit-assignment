package com.ai.astrabitassignment.security;

import com.google.crypto.tink.subtle.Ed25519Verify;
import com.google.crypto.tink.subtle.Hex;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

public class Ed25519Verifier {

    private final Ed25519Verify verifier;

    public Ed25519Verifier(String publicKeyHex) {
        this.verifier =
                new Ed25519Verify(Hex.decode(publicKeyHex));
    }

    public boolean verify(
            String signatureHex,
            String timestamp,
            byte[] rawBody
    ) {
        try {
            byte[] message = buildMessage(timestamp, rawBody);

            byte[] signature = Hex.decode(signatureHex);

            verifier.verify(signature, message);

            return true;

        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    private byte[] buildMessage(
            String timestamp,
            byte[] rawBody
    ) {
        byte[] timestampBytes =
                timestamp.getBytes(StandardCharsets.UTF_8);

        byte[] message =
                new byte[timestampBytes.length + rawBody.length];

        System.arraycopy(
                timestampBytes,
                0,
                message,
                0,
                timestampBytes.length
        );

        System.arraycopy(
                rawBody,
                0,
                message,
                timestampBytes.length,
                rawBody.length
        );

        return message;
    }
}