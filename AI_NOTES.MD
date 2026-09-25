# AI Notes

Running log of work done with Claude Code on this project. Newest entries at the bottom.

## 2026-09-25 — Q&A: what is Ed25519

Asked whether Ed25519 is a hashing algorithm and whether it can be converted
to a `java.security.PublicKey`.

- Ed25519 is **not** a hash. It's a digital signature algorithm (EdDSA over
  Curve25519) — signs a message with a private key, verifies with the
  matching public key. Uses SHA-512 internally, but its purpose is
  authentication/integrity, not digesting data. In this project: Discord
  signs each interaction webhook body with its private key; we verify with
  `DISCORD_PUBLIC_KEY` to prove the request came from Discord.
- **Can convert to `java.security.PublicKey`:** yes, JDK 15+ supports Ed25519
  natively via `Signature.getInstance("Ed25519")`, but it won't accept the
  raw 32-byte key directly like Tink's `Ed25519Verify` does — you have to
  wrap it in a DER-encoded `SubjectPublicKeyInfo` (fixed 12-byte ASN.1
  prefix + the raw 32 bytes) and load it via
  `KeyFactory.getInstance("Ed25519")` + `X509EncodedKeySpec`. That raw-key
  friction is why the project uses a library 
  instead of the bare JDK API. No action taken — offered to
  show the JDK-native version if wanted, but no functional reason to switch
  off Tink.

---

## 2026-09-25 — Q&A: Tink primitives — AEAD vs Signature, and signature vs MAC

Asked (after reading up on Tink) whether Tink verifies the Discord public key
via AEAD and KeySet metadata, and whether the platform digitally signs
messages or just uses a MAC.

- **Not AEAD.** AEAD (e.g. AES-GCM) is Tink's *symmetric encryption*
  primitive family — unrelated to what we're doing. `Ed25519Verifier` uses
  Tink's **Signature** primitive family (`PublicKeySign`/`PublicKeyVerify`),
  specifically the low-level `com.google.crypto.tink.subtle.Ed25519Verify`
  class.
- **Not using `Keyset`/`KeysetHandle` either.** Tink's idiomatic API wraps
  keys in a `Keyset` with metadata (key ID, status, output-prefix type), and
  a keyset can itself be *encrypted at rest* with an AEAD key (often
  KMS-backed) — that's envelope encryption for key **storage**, not part of
  signing/verification math. Our code skips the keyset layer entirely and
  calls `subtle.Ed25519Verify` directly with Discord's raw 32-byte public
  key (`Hex.decode(publicKeyHex)`) — no wrapper, no metadata, no AEAD
  anywhere. That's the "simpler than the JDK's raw-key handling" the README
  refers to: Tink's `subtle` API takes raw key bytes directly, where the JDK
  forces DER encoding first.
- **Digital signature, not a MAC.** A MAC (e.g. HMAC) is symmetric — the
  same secret key both produces and verifies it, so anyone who can verify
  could also forge one. A signature is asymmetric — Discord holds the
  private key and signs; we only ever hold the public key
  (`DISCORD_PUBLIC_KEY`) and can verify but never produce a valid signature.
  That's required here: if it were a MAC, we'd need to hold the same secret
  Discord signs with, and a leaked env var would let anyone forge requests
  as Discord.

No code changes — conceptual clarification only.

