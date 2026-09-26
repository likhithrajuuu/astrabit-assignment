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

---

## 2026-09-26 — README for local dev + `/ping` command

User registered a second slash command, `/ping` (`PingCommand.java` — replies
`pong`, no options), alongside the existing `/report`. Both committed as
`7ee37ea feat: introduced slash commands component along with registered
commands /ping and /report successfully`. Note: `/report` still only replies
ephemerally that the report was received — it does not yet persist to the
`interaction` table (that's still planned, not done).

Wrote onboarding docs so other developers can run the project locally:
- `.env.example` — added (didn't exist before). Lists every var the real
  `.env` defines; only `DATABASE_URL`/`DATABASE_USERNAME`/`DATABASE_PASSWORD`
  and `DISCORD_APP_ID`/`DISCORD_PUBLIC_KEY`/`DISCORD_BOT_TOKEN` are actually
  read by the app right now (confirmed via grep against
  `application.yaml`) — `DATABASE_URL_POOLED` and the `AWS_*`/`S3_*` vars
  are present but unused, flagged as reserved for later.
- `README.md` — rewritten from the bare tech-stack table into full local-dev
  instructions: prerequisites, loading `.env` into the shell (no dotenv
  loader in the app, confirmed no such dependency in `pom.xml`), Flyway runs
  automatically on boot, `./mvnw spring-boot:run`, exposing localhost via
  ngrok and setting Discord's Interactions Endpoint URL, trying `/ping` and
  `/report`, running tests, and a troubleshooting section for the common
  failure modes (signature mismatch, command-propagation delay, DB
  connectivity). Also corrected the tech-stack table's crypto row from
  "BouncyCastle" to "Google Tink" to match what's actually in `pom.xml`.

Note: `.env` (read only to see which variable names exist, never to copy
values anywhere) currently holds *live* credentials — a real Neon DB
password, live S3-compatible access keys, and a live Discord bot token.
It's correctly gitignored already; just flagging that it's sensitive.

Nothing else changed in application code this session.

---

## 2026-09-26 — Diagnosed: Lombok build failure (`ExceptionInInitializerError`)

User added Lombok (`org.projectlombok:lombok`, resolved to `1.18.46` via the
Spring Boot 4.1.1 BOM, with the `annotationProcessorPaths` entry on
`maven-compiler-plugin` already correctly wired in `pom.xml`) for
getter/setter/constructor boilerplate on entity classes. `mvn clean compile`
was failing with:

```
java.lang.RuntimeException: java.lang.ExceptionInInitializerError
Caused by: java.lang.ExceptionInInitializerError
  at lombok.javac.apt.LombokProcessor.placePostCompileAndDontMakeForceRoundDummiesHook
Caused by: java.lang.ClassNotFoundException: com.sun.tools.javac.tree.EndPosTable
  at lombok.javac.Javac.<clinit>
```

**Root cause:** not a Lombok misconfiguration. Lombok generates code by
reflectively patching javac's *private* internal classes (`com.sun.tools.javac.*`),
not just the public annotation-processing API — so it needs its own release
to track every javac internals change on a new JDK. The only JDK installed
on this machine was **JDK 27** (released 2026-09-15, i.e. ~10 days old at
diagnosis time) — Lombok 1.18.46 doesn't have a compatibility shim for it
yet, so `Javac.<clinit>` fails looking up `EndPosTable`, and that static-init
failure is what surfaces as the generic `ExceptionInInitializerError` — the
actual getters/setters were never the problem.

**Fix:** the `pom.xml` already targets `<java.version>21</java.version>` —
just needed an actual JDK 21 to build with. Installed via
`brew install openjdk@21` (keg-only, doesn't touch the system default JDK 27
or `/Library/Java/JavaVirtualMachines` — no `sudo`/system-wide symlink used).
Verified the fix directly:
`JAVA_HOME=$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home ./mvnw clean compile`
→ `BUILD SUCCESS`, 17 source files compiled including the Lombok-annotated
ones.

Documented in `README.md` Prerequisites (why JDK 21 specifically, the
install + `JAVA_HOME` export commands) and added a reminder in the "Run the
app" step, since `JAVA_HOME` has to be set in every terminal used to build —
it's not a one-time machine-wide fix unless the user chooses to make JDK 21
their shell default themselves.

Not done: no `maven-enforcer-plugin` guard added to fail fast with a clear
message if someone builds with the wrong JDK again — mentioned as an
optional follow-up, not implemented, since it wasn't asked for.

---

## 2026-09-26 — Follow-up: IntelliJ JDK picking, then Jackson 2 vs 3 bean error

**IntelliJ still using JDK 27 after run-config JRE was set to 21.** Setting
the Run/Debug Configuration's JRE only picks the JVM that *executes* the
already-built app — it doesn't control compilation, and Lombok's crash
happens during compilation (its annotation processor runs inside `javac`
itself). Pointed the user at the two settings that actually matter:
- **Project Structure → Project → SDK** (governs IntelliJ's own builder)
- **Settings → Build Tools → Maven → Runner → JRE** (governs Maven's javac
  invocation when IDE build/run is delegated to Maven — this project's
  earlier stack traces were Maven/Plexus-driven, so this one's likely the
  real culprit if the error persisted after fixing Project SDK)

**New app code added by user (not by me):** `entities/Interaction.java`
(JPA entity using Lombok `@Data`/`@NoArgsConstructor`/`@AllArgsConstructor`),
`interactions/InteractionRepository.java` (plain `JpaRepository`), and
`interactions/InteractionService.java` (maps the raw interaction `Map` to an
`Interaction` entity and saves it). Wired into
`InteractionController` — `save()` now runs on every `APPLICATION_COMMAND`
interaction (`/ping` included) before dispatch.

**Bug found and fixed:** `InteractionService`'s constructor took
`(InteractionRepository, ObjectMapper)`, causing
`Parameter 1 of constructor ... required a bean of type
'com.fasterxml.jackson.databind.ObjectMapper' that could not be found`.

Root cause, confirmed by inspecting the actual dependency tree and Spring
Boot's own auto-configuration class (not guessed): **Spring Boot 4.1.1 has
switched its default JSON engine to Jackson 3**, which rebranded its
group/package from `com.fasterxml.jackson.*` to `tools.jackson.*`.
`org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration`
(decompiled with `javap`) only produces a
`tools.jackson.databind.json.JsonMapper` bean — never a
`com.fasterxml.jackson.databind.ObjectMapper` one. The old Jackson 2 classes
were still on the classpath only as an incidental transitive dependency
(something else — likely Discord4j or Spring AI — still uses Jackson 2
internally), with no bean registered for that type, hence the injection
failure.

**Flagged, not fixed:** the FK problem noted back on 2026-09-25 is now live,
not hypothetical — `interaction.guild_id` is `NOT NULL REFERENCES guild(id)`,
and no `guild` row (which itself needs an `app_user` row) gets created
anywhere yet. The very first real `/ping` in a Discord server will now throw
a foreign-key-violation exception on `interactionService.save(...)`. Told
the user; not fixed, since building the OAuth "connect your server" flow
that creates those rows is a bigger, separate piece of work.

---

## 2026-09-26 — User fixed the guild/app_user FK gap; Q&A: what is AI triage

**User's fix for the FK problem flagged above:** `InteractionService.save()`
now auto-provisions both rows before inserting the `interaction` row, via
raw native queries on an injected `EntityManager`, wrapped in
`@Transactional`:
1. Upsert `app_user` keyed on `discord_user_id`
   (`ON CONFLICT (discord_user_id) DO UPDATE ... RETURNING id`) with a
   placeholder `password_hash` of `'auto_generated_no_login'` — these
   Discord-only users aren't meant to log into a dashboard with a password.
2. Upsert `guild` keyed on `id` (`ON CONFLICT (id) DO NOTHING`) using that
   app_user's id as `owner_user_id`.

Reasonable stopgap given there's no OAuth "connect your server" flow yet —
the upsert pattern (`DO UPDATE ... RETURNING id`) is the correct way to get
an id back from Postgres regardless of insert-vs-conflict. Not flagging any
issue with it.

**Q&A: what is "AI triage" in the Discord-bot context, and where does it
fit in this project?** Explained: instead of only matching reports against
literal keyword/regex rules, hand the raw report text to an LLM (Groq/Gemini
— why `spring-ai-starter-model-google-genai` is in `pom.xml`) to judge
severity/category. Maps onto columns already in the schema but unpopulated:
`interaction.severity` (urgency score), `interaction.ai_summary` (plain-
language summary for moderators), `interaction.ai_tags` (JSONB category
labels), and `rule.match_type = 'AI_SEVERITY'` (a rule can fire off the AI's
judgment instead of a literal pattern match).

Intended flow: `/report <details>` → `InteractionService.save()` persists
raw payload (now also auto-provisioning app_user/guild, per above) → an AI
triage step (not built yet) sends `details` to Gemini for
severity/summary/tags → writes those back onto the `interaction` row → the
rule engine (also not built yet) evaluates that guild's `rule` rows in
`priority` order and fires the matched `action` (`MIRROR` to a mod channel,
`PING_ROLE` for urgent cases, `ADD_BUTTONS` for quick mod actions,
`SET_SEVERITY` alone if nothing more is needed).

Point of it: free-text reports like "he keeps DMing me weird stuff" don't
match any keyword rule, but an LLM can read it and flag "harassment,
moderate severity" so it's prioritized instead of buried in a log next to
spam reports.

No code changes — conceptual clarification only. Neither the AI triage call
nor the rule engine exist yet.

---

## 2026-09-26 — Diagnosed (not fixed, per user's request): `ReportCommand`'s Gemini wiring

User implemented the actual AI triage call in `ReportCommand.java`: on
`/report`, it now replies with a deferred response (type 5), then
asynchronously calls Gemini (`gemini-1.5-flash:generateContent`) to
summarize/tag the report, and PATCHes the original Discord interaction
response with the result via Discord's webhook follow-up endpoint
(`/webhooks/{application_id}/{token}/messages/@original`). Real feature
work, not scaffolding.

Hit the same `required a bean of type
'com.fasterxml.jackson.databind.ObjectMapper'` error as `InteractionService`
before it. User explicitly asked for diagnosis only, no fixes applied. Found
**four separate problems** stacked in this one file (only the first is what
the pasted error is actually about; the rest are latent, would surface one
at a time as each prior one gets fixed):

1. **Same Jackson 2 vs 3 mismatch as before.** Imports
   `com.fasterxml.jackson.databind.{ObjectMapper,JsonNode}` (Jackson 2).
   Same root cause as the `InteractionService` fix from earlier today —
   Spring Boot 4.1.1 only auto-configures a Jackson 3
   `tools.jackson.databind.json.JsonMapper` bean.

2. **`@Value` resolves to the wrong annotation.** `import
   com.google.api.client.util.Value;` — not Spring's
   `org.springframework.beans.factory.annotation.Value`. Verified by
   decompiling the actual jar: this is a real, different annotation from
   `google-http-client-1.46.2.jar` (pulled in transitively via
   `spring-ai-starter-model-google-genai`'s Google GenAI SDK dependency
   chain), used internally by Google's HTTP client for over-the-wire enum/
   field serialization (`GenericData`, URL-encoded params) — nothing to do
   with property injection. Its shape (`String value()`, same
   `@Value("...")` call syntax) is why an IDE auto-import could plausibly
   pick it over Spring's — looks identical, compiles clean, no warning.
   Since Spring's `AutowiredAnnotationBeanPostProcessor` only recognizes its
   own `Value` class, it never sees this field at all, so `geminiApiKey`
   silently stays at Java's default (`null`) — no exception, no log, nothing
   — which is exactly the symptom the user found when printing it.

3. **Even with the right annotation, the key string is wrong.**
   `@Value("${gemini.api.key}")` looks for a flat `gemini.api.key` property.
   The actual config in `application.yaml` is nested under Spring AI's own
   structure: `spring.ai.google.genai.api-key` (backed by `${GEMINI_API_KEY}`
   from `.env`). `gemini.api.key` doesn't exist anywhere, so this would
   throw `Could not resolve placeholder` once #2 is fixed.

4. **No `HttpClient` bean exists anywhere in the codebase.** The
   Lombok-generated `@RequiredArgsConstructor` needs `(ObjectMapper,
   HttpClient)` for its two `final` fields, but nothing declares a `@Bean`
   of type `java.net.http.HttpClient`, and Spring Boot doesn't
   auto-configure the JDK's `HttpClient` out of the box. This is very likely
   the *next* missing-bean error once #1 is fixed.

Verified #2 by actually locating and decompiling the class with `javap`
against the resolved dependency jar (`google-http-client-1.46.2.jar`) rather
than assuming from the import name alone.

Nothing changed in the repo this session — diagnosis only, at the user's
explicit request.

