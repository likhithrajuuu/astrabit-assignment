# Endpoints

This app exposes exactly **two** HTTP routes. Discord doesn't call individual
URLs per command — every slash command, PING, and future button/modal
interaction all arrive as the same POST to `/api/interaction`, with a `type`
(and, for commands, a `data.name`) inside the JSON body that this app routes
internally. So this doc covers the two real HTTP routes first, then
documents each of those internal "virtual routes" (interaction types, then
each slash command) the same way — required input, response shape,
side effects.

---

## HTTP routes

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `POST` | `/api/interaction` | Ed25519 signature (see below) | Discord's interactions webhook — every PING and slash command lands here |
| `GET` | `/actuator/health` | none | Liveness/readiness check (Spring Boot Actuator, `show-details: always`) |

No other routes are registered — `AstrabitAssignmentApplication` has exactly
one `@RestController` (`InteractionController`).

### `GET /actuator/health`

- **Requires:** nothing
- **Response:** standard Spring Boot Actuator health JSON, e.g.:
  ```json
  { "status": "UP", "components": { "db": { "status": "UP", "details": {...} }, "diskSpace": {...} } }
  ```
  `status` flips to `DOWN` if the Postgres connection configured by
  `DATABASE_URL` is unreachable — this doubles as a DB connectivity check.

### `POST /api/interaction`

- **Requires:**
  - Headers `X-Signature-Ed25519` and `X-Signature-Timestamp` on every
    request. `DiscordSignatureFilter` verifies `timestamp + rawBody` against
    `DISCORD_PUBLIC_KEY` before the request reaches the controller.
  - **Missing or invalid signature → `401`, empty body, controller never
    runs.** This is enforced for every request under this path, including
    malformed ones.
- **Request body:** a raw [Discord Interaction
  object](https://discord.com/developers/docs/interactions/receiving-and-responding#interaction-object)
  — arbitrary JSON, deserialized into a `Map<String, Object>`. The fields
  this app actually reads: `type`, `data.name`, `data.options[]`, `guild_id`,
  `member.user` / `user` (`id`, `username`), `token`, `application_id`, `id`.
- **Response:** depends on `interaction.type` —

  | `type` | Meaning | What happens |
  |---|---|---|
  | `1` | PING | Replies `{"type": 1}` immediately. No persistence, no signature-adjacent side effects beyond the filter itself. |
  | `2` | APPLICATION_COMMAND | See "Every command, before dispatch" and the command table below. |
  | anything else | Not implemented | Replies ephemerally: `{"type": 4, "data": {"content": "This interaction type isn't supported yet.", "flags": 64}}` |

  If `type` is `2` but `data.name` doesn't match any registered command:
  `{"type": 4, "data": {"content": "Unknown command.", "flags": 64}}`
  (from `SlashCommandDispatcher`).

#### Every command, before dispatch

`InteractionController` calls `InteractionService.save(interaction)` for
**every** `APPLICATION_COMMAND` interaction, before routing to the command
handler. This:
1. Auto-provisions an `app_user` row keyed on the invoking Discord user ID
   (upsert; placeholder `password_hash` — these users can't log into a
   dashboard, there isn't one yet), then an `guild` row keyed on `guild_id`
   owned by that user (upsert) — satisfies `interaction.guild_id`'s foreign
   key, since there's no OAuth "connect your server" flow yet to create
   these rows any other way.
2. Inserts a row into `interaction` with `status = 'RECEIVED'`, the full raw
   payload as JSON, and the extracted `guild_id`/`command_name`/`user_id`/
   `user_name`.

Each command handler (except `/ping`) later updates that same row's
`status` as it processes (see per-command status column below).

---

## Slash commands

Registered globally on startup by `DiscordCommandRegistrar` (bulk overwrite
— idempotent, safe on every boot; failures are logged, not fatal). Global
commands can take up to ~1 hour to first appear in Discord.

| Command | Options | Response shape | Requires | DB status update |
|---|---|---|---|---|
| `/ping` | none | Immediate (`type 4`): `pong` | nothing | none |
| `/8ball` | `question` (STRING, required) | Immediate (`type 4`): `🎱 Question / 🔮 Answer`, random canned response | nothing | `RECEIVED` → `PROCESSED` |
| `/roll` | `sides` (INTEGER, optional, default `6`, clamped `2`–`1,000,000`) | Immediate (`type 4`): `🎲 You rolled a N (1-sides)!` | nothing | `RECEIVED` → `PROCESSED` |
| `/ask` | `question` (STRING, required) | Deferred (`type 5`) ack, then a follow-up `PATCH` a moment later with Gemini's answer (truncated ~1900 chars) | `GEMINI_API_KEY`, `GEMINI_PROJECT_NUMBER` | `RECEIVED` → `PROCESSED` |
| `/roast` | `target` (STRING, required) | Deferred (`type 5`) ack, then a follow-up `PATCH` with a Gemini-generated roast of `target` (truncated ~1900 chars) | `GEMINI_API_KEY`, `GEMINI_PROJECT_NUMBER` | `RECEIVED` → `PROCESSED` |
| `/report` | `details` (STRING, required) | Deferred + **ephemeral** (`type 5`, `flags: 64`) ack, then a follow-up `PATCH` with an AI triage summary/severity/tags | `GEMINI_API_KEY`, `GEMINI_PROJECT_NUMBER`, `DISCORD_WEBHOOK_URL` (see below) | `RECEIVED` → `PROCESSING` → `PROCESSED` (see below) |

### `/ping`

- **Options:** none
- **Handler:** `PingCommand`
- **Response:** immediate, not ephemeral — `{"type": 4, "data": {"content": "pong"}}`
- Doesn't touch the database beyond the `save()` step every command gets.

### `/8ball <question>`

- **Options:** `question` — STRING, required. "What question do you seek answers for?"
- **Handler:** `EightBallCommand`
- **Response:** immediate, not ephemeral. Picks one of 20 canned Magic-8-Ball
  answers at random and replies:
  `🎱 **Question:** <question>\n🔮 **Answer:** <answer>`
- **Side effect:** `UPDATE interaction SET status = 'PROCESSED' WHERE payload->>'id' = ?`
  (best-effort — failure is caught/logged, doesn't affect the reply).

### `/roll [sides]`

- **Options:** `sides` — INTEGER, optional. "Number of sides (default is 6)."
  Clamped to `[2, 1_000_000]` if given.
- **Handler:** `RollCommand`
- **Response:** immediate, not ephemeral —
  `🎲 You rolled a **<roll>** (1-<sides>)!`
- **Side effect:** same best-effort `PROCESSED` update as `/8ball`.

### `/ask <question>`

- **Options:** `question` — STRING, required. "What do you want to ask?"
- **Handler:** `AskCommand`
- **Response:** deferred ack first (`type 5`, empty `data` — Discord shows
  "Astrabot is thinking…"), then this app calls Gemini asynchronously
  (`ChatClient`, system prompt fixes it as "Astrabot", capped under 1900
  chars) and `PATCH`es
  `/webhooks/{application_id}/{token}/messages/@original` with the answer.
  On failure, the follow-up becomes `"I encountered an error trying to think
  about that."` instead.
- **Requires:** `GEMINI_API_KEY` + `GEMINI_PROJECT_NUMBER` set and valid
  (Spring AI `ChatClient.Builder`, auto-configured by
  `spring-ai-starter-model-google-genai`).
- **Side effect:** `PROCESSED` update, same pattern as `/8ball`.

### `/roast <target>`

- **Options:** `target` — STRING, required. "What or who should be roasted?"
- **Handler:** `RoastCommand`
- **Response:** same deferred + follow-up pattern as `/ask`, but the prompt
  asks Gemini for "a sharp, funny, and witty roast" of `target` (capped
  ~1900 chars), formatted as `🔥 **Roast on <target>:**\n<response>`. Waits
  ~1s before calling Gemini (no functional reason found for the delay).
- **Requires:** same Gemini env vars as `/ask`.
- **Side effect:** `PROCESSED` update, same pattern.

### `/report <details>`

- **Options:** `details` — STRING, required. "What should moderators look into?"
- **Handler:** `ReportCommand`
- **Response:** deferred **and ephemeral** ack (`type 5`, `flags: 64` — only
  the reporter sees anything in-channel), then a follow-up `PATCH` with the
  AI triage result once ready.
- **Requires:** `GEMINI_API_KEY` + `GEMINI_PROJECT_NUMBER` (same as
  `/ask`/`/roast`) **and** `DISCORD_WEBHOOK_URL` for the mirror step below.
  This env var isn't documented in the README/`.env.example` yet — see
  `AI_NOTES.MD`.
- **What actually happens, in order:**
  1. Waits ~1.5s, then flips that interaction's row from `RECEIVED` to
     `PROCESSING` — guarded (`AND status = 'RECEIVED'`) so a duplicate/retry
     delivery of the same interaction is a no-op instead of double-processing.
  2. Calls Gemini with a prompt asking for **strict JSON** back:
     `{"summary": "...", "tags": [...], "severity": "1 - Minor".."5 - Critical"}`,
     strips any ```` ```json ```` fencing, parses it into an `AiTriageResult`.
  3. Writes `ai_summary`, `ai_tags` (as `jsonb`), `severity`, and
     `status = 'PROCESSED'` back onto the `interaction` row.
  4. Sends the reporter a follow-up message with the summary/severity/tags.
  5. Calls `NotificationMirrorService.mirrorCommandExecution(...)` — posts a
     Discord embed (title, description, color-coded by severity, user +
     severity fields) to `DISCORD_WEBHOOK_URL` via a plain webhook POST. This
     is the "Mirror" feature from the original tech-stack table, now
     implemented for `/report` only.
  - On any failure in the above: sends the reporter `"Report received, but
    AI analysis failed: <message>"` and mirrors a `"CRITICAL"`-severity
    failure notice instead.
- Note: `NotificationMirrorService`'s source file lives under
  `src/main/java/.../mirror/` but declares `package
  com.ai.astrabitassignment.services;` — a directory/package mismatch.
  Doesn't break the build (`javac` doesn't enforce the two matching), just
  an inconsistency worth cleaning up at some point.
