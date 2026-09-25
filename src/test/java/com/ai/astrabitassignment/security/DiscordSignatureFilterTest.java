package com.ai.astrabitassignment.security;

import com.google.crypto.tink.subtle.Ed25519Sign;
import com.google.crypto.tink.subtle.Hex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DiscordSignatureFilterTest {

    private static final String TIMESTAMP = "1700000000";
    private static final byte[] BODY = "{\"type\":1}".getBytes(StandardCharsets.UTF_8);

    private Ed25519Sign.KeyPair keyPair;
    private DiscordSignatureFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        keyPair = Ed25519Sign.KeyPair.newKeyPair();
        Ed25519Verifier verifier = new Ed25519Verifier(Hex.encode(keyPair.getPublicKey()));
        filter = new DiscordSignatureFilter(verifier);
    }

    @Test
    void passesThroughARequestWithAValidSignatureAndKeepsTheBodyReadable() throws Exception {
        MockHttpServletRequest request = interactionRequest(sign(TIMESTAMP, BODY), TIMESTAMP, BODY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
        byte[] bodyAsSeenByController = StreamUtils.copyToByteArray(chain.getRequest().getInputStream());
        assertThat(bodyAsSeenByController).isEqualTo(BODY);
    }

    @Test
    void rejectsAnInvalidSignatureWithoutInvokingTheController() throws Exception {
        MockHttpServletRequest request = interactionRequest("00".repeat(64), TIMESTAMP, BODY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void rejectsARequestMissingTheSignatureHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/interaction");
        request.setContent(BODY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void leavesOtherPathsAlone() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    private MockHttpServletRequest interactionRequest(String signature, String timestamp, byte[] body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/interaction");
        request.addHeader("X-Signature-Ed25519", signature);
        request.addHeader("X-Signature-Timestamp", timestamp);
        request.setContent(body);
        return request;
    }

    private String sign(String timestamp, byte[] body) throws Exception {
        Ed25519Sign signer = new Ed25519Sign(keyPair.getPrivateKey());

        byte[] timestampBytes = timestamp.getBytes(StandardCharsets.UTF_8);
        byte[] message = new byte[timestampBytes.length + body.length];
        System.arraycopy(timestampBytes, 0, message, 0, timestampBytes.length);
        System.arraycopy(body, 0, message, timestampBytes.length, body.length);

        return Hex.encode(signer.sign(message));
    }
}
