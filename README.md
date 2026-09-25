# astrabit-assignment

A Discord bot built as a single Spring Boot deployable: Discord sends every
slash-command interaction as an HTTP webhook to `/api/interaction`, we verify
it's really from Discord (Ed25519 signature), route it to a command handler,
and reply. No gateway/websocket connection is used.

### Tech Stack

| Layer     | Choice                                                | Why |
|-----------|--------------------------------------------------------|-----|
| App       | Spring Boot 4.1, Java 21                                | One deployable holds the API, and (later) the dashboard and workers |
| DB        | Postgres (Neon) + Flyway                                | Free tier, and gives you `ON CONFLICT` and `SKIP LOCKED` |
| Discord   | Discord4j (REST only — no gateway connection)           | Used to register slash commands and call the Discord API |
| Crypto    | Google Tink `Ed25519Verify`                             | Verifies Discord's interaction signatures without hand-rolling raw-key parsing |
| Hosting   | Render free web service (Docker) — planned              | Supports JVM apps without a card |
| AI        | Groq (Llama) or Gemini free tier — planned               | For report triage |

See [AI_NOTES.MD](AI_NOTES.MD) for a running log of design decisions.

## Prerequisites

- **JDK 21** (the repo uses the Maven wrapper, so you don't need Maven installed separately)
- A **Postgres database** — the free tier of [Neon](https://neon.tech) works well
- A **Discord Application** — create one at the
  [Discord Developer Portal](https://discord.com/developers/applications)
- A way to expose your local server to the internet for Discord to call —
  [ngrok](https://ngrok.com) is the easiest (`brew install ngrok`)

## 1. Configure environment variables

```bash
cp .env.example .env
```

Fill in `.env`:

| Variable | Where to get it |
|---|---|
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | Your Postgres project's connection details |
| `DISCORD_APP_ID` | Discord Developer Portal → your app → **General Information** → Application ID |
| `DISCORD_PUBLIC_KEY` | Same page → **Public Key** |
| `DISCORD_BOT_TOKEN` | **Bot** tab → Reset/copy **Token** |

`DATABASE_URL_POOLED` and the `AWS_*`/`S3_*` variables are reserved for
upcoming features and aren't read by the app yet — you can leave them blank.

The app reads these from the process environment (`application.yaml` uses
`${DATABASE_URL}` etc. directly — there's no dotenv loader), so load the
file into your shell before running:

```bash
set -a && source .env && set +a
```

Do this in every new terminal you run the app from.

## 2. Set up the database

Nothing to run by hand — Flyway applies everything under
`src/main/resources/db/migration` automatically on startup.
Just make sure `DATABASE_URL` points at a reachable, empty Postgres database.

## 3. Run the app

```bash
./mvnw spring-boot:run
```

On a clean boot this will:
1. Run Flyway migrations against your database
2. Start the web server on `http://localhost:8080`
3. Try to bulk-register this app's slash commands with Discord's global
   command API (see `DiscordCommandRegistrar`). If Discord isn't reachable
   or the credentials are wrong, this only logs a warning — it won't stop
   the app from starting.

Sanity check:

```bash
curl http://localhost:8080/actuator/health
```

## 4. Point Discord at your local server

Discord calls out to a public HTTPS URL, so `localhost` alone isn't enough
for real interaction testing.

```bash
ngrok http 8080
```

Take the `https://<random>.ngrok-free.app` URL ngrok prints, and in the
Discord Developer Portal set your app's **Interactions Endpoint URL** to:

```
https://<random>.ngrok-free.app/api/interaction
```

Discord immediately sends a PING to verify the endpoint — if the app is
running with the right `DISCORD_PUBLIC_KEY`, this should go green in the
portal. If it fails, see Troubleshooting below.

## 5. Try it in Discord

Invite the bot to a test server using an OAuth2 URL from the portal's
**OAuth2 → URL Generator** (scopes: `bot`, `applications.commands`).

Registered commands:

- **`/ping`** — replies `pong`. Quick check that the endpoint and signature
  verification are working end-to-end.
- **`/report <details>`** — accepts free text and replies ephemerally that
  the report was received and will be reviewed. (Persisting reports to the
  `interaction` table and running them through the rule engine is planned,
  not wired up yet.)

Note: global commands can take up to ~1 hour to show up for the first time;
edits to an existing command propagate faster.

## Running tests

```bash
./mvnw test
```

`Ed25519VerifierTest` and `DiscordSignatureFilterTest` are pure unit tests
(no DB or network needed) covering signature verification. The generated
`AstrabitAssignmentApplicationTests` boots the full Spring context and
therefore needs a real, reachable database — make sure your env vars are
loaded (step 1) before running the full suite.

## Troubleshooting

- **Discord Developer Portal won't save the Interactions Endpoint URL** —
  it PINGs the URL and expects a valid signed response. Double-check
  `DISCORD_PUBLIC_KEY` in `.env` matches the portal exactly, and that ngrok
  is still running and pointed at the right port.
- **`401` from `/api/interaction`** — same cause: signature verification
  failed. This is `DiscordSignatureFilter` rejecting the request; check
  `DISCORD_PUBLIC_KEY`.
- **Commands don't show up in Discord** — global command registration can
  take up to an hour on first registration; check the app logs for
  `Registered Discord command: ...` or a `Skipping Discord slash-command
  registration: ...` warning from `DiscordCommandRegistrar`.
- **App fails to start on DB connection** — verify `DATABASE_URL` /
  `DATABASE_USERNAME` / `DATABASE_PASSWORD` are exported in your current
  shell (`echo $DATABASE_URL`) and that your Postgres provider allows
  connections from your IP.
