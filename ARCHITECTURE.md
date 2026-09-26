# Architecture Decisions

### AI Tools used:
- Claude
- Model : Sonnet 5

### How I leveraged AI for the development workflow
- I checked the `Ed25519` parsing flow and how to achieve the cryptographic key that Java can read
- I clearly remember that after creating the bot I was totally clueless. I then researched the same using AI which actually guided me to `ngrok the localhost:8080` and then use that in the bot page.
- There were few bugs that appeared while I was using Java 27 version which my laptop had which actually helped me to install Java 21 as a safer alternative
- I was completely clueless at a point the application was throwing a bug that I found very difficult to trace as the ones that I had suspected were not contributing to any bug
- There were multiple scenarios where I drew a map/flow for the same to verify them I utilized AI.

## Database Design Document
Please find the document and the image of the database design in the root project directory. \
Look for:
- Database Design.drawio (can have a look in any xml viewer)
- Database Design.drawio.png (for quicker image based access)

## Verification Flow for the Ed25519
1. **Extract Headers:** The app intercepts the `X-Signature-Ed25519` and `X-Signature-Timestamp` headers from the incoming HTTP POST request.
2. **Reconstruct the Message:** The timestamp and the raw JSON request body are concatenated into a single byte array (`timestamp + body`).
3. **Cryptographic Proof:** The application uses the Discord bot's Public Key (injected via environment variables) to verify the signature against the reconstructed message.
4. **Action:** If the signature matches, the request is routed to the command handler. If it fails or is malformed, the application immediately drops the request with a `401 Unauthorized` response, preventing unauthorized code execution or database bloat.

## Most Challending Issue(26-09-2026)
### Pre-Context
Initially, I ensured `InteractionController` received the POST request, and synchronously ran something like `INSERT INTO interaction` inorder to log the event and then passed the payload to the command handler. It was actually sending a deferred acknowledgement to the Discord. I just had done the de-duplication

### Problem
1. The AI actually failed to account for behaviour of the Neon DB. Along with that I discovered that even the AI service was also failing saying something like `{"message":"Unknown Webhook, "code": 10015}`\
2. I also asked AI on how to convert the `Ed25519` to `Java Cryptographic Key` which was again pointing me to `X509` which was a lengthier process and involved much complex work that I couldn't grasp quickly.

### Approach
1. I actually first ensured if the API Key of the AI is not returning null by adding a log to print the AI key. AI Key was being recognized successfully. I actually went to Discord's documentation that I found on Google. \
Link : `https://support.discord.com/hc/en-us/articles/33694251638295-Discord-Account-Caps-Server-Caps-and-More` 
2. I found Google's Library called `tink` that actually converted to the desired format i.e byte array and then to manage the keys using tink's KMS and also provided support for rotation of my keys, which was perfect \
Link: `https://developers.google.com/tink`

### Solution I arrived on
I found out from this documentation that for a Base Account on Discord, there is a file sharing limit of `10MB` and then message length / response length was `2000 characters` and thread caps of the `3-day archive` and also the `3-second` rule of the response. This was the sole reason going for the `CompletableFuture` in order to have asynchronous, non-blocking calls as `Gemini-2.5-Flash` is slower.\
Arriving to this solution only took me a half a day 
