# astrabit-assignment

### Tech Stack
Layer	Choice	Why
App	Spring Boot 3.5 / 4.x, Java 21, virtual threads on	One deployable holds the API, dashboard and workers
UI	Thymeleaf + HTMX + Server-Sent Events	Live log without a separate frontend, and no secrets reach the browser
DB	Neon Postgres + Flyway	Free, and gives you ON CONFLICT and SKIP LOCKED
Hosting	Render free web service (Docker)	Supports JVM apps without a card
Keep-warm	UptimeRobot or cron-job.org pinging /actuator/health every 5 min	Important, see below
Mirror	Slack Incoming Webhook (or a Discord channel webhook)	Paste-a-URL
AI	Groq (Llama) or Gemini free tier	For triage
Crypto	BouncyCastle Ed25519Signer	Simpler than the JDK's raw-key handling