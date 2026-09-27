const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
const DISCORD_APP_ID = process.env.NEXT_PUBLIC_DISCORD_APP_ID ?? "";

// View Channel, Send Messages, Embed Links, Attach Files, Read Message
// History, Use Application Commands — enough for the bot to actually work,
// nothing admin-level.
const BOT_PERMISSIONS = "2147601408";

/** Builds the "Add to Server" Discord OAuth URL. Client-side only (needs window.location). */
export function discordInviteUrl(): string {
  const redirectUri = `${window.location.origin}/connect/callback`;
  const params = new URLSearchParams({
    client_id: DISCORD_APP_ID,
    permissions: BOT_PERMISSIONS,
    scope: "bot applications.commands",
    response_type: "code",
    redirect_uri: redirectUri,
  });
  return `https://discord.com/oauth2/authorize?${params.toString()}`;
}

export type CommandConfig = {
  guildId: string;
  commandName: string;
  enabled: boolean;
  replyTemplate: string | null;
  mirrorEnabled: boolean;
  aiEnabled: boolean;
  ephemeral: boolean;
};

export type Rule = {
  id: number | null;
  guildId: string;
  commandName: string;
  matchType: "KEYWORD" | "REGEX" | "AI_SEVERITY";
  pattern: string;
  action: "SET_SEVERITY" | "MIRROR" | "PING_ROLE" | "ADD_BUTTONS";
  params: string;
  priority: number;
};

export type Interaction = {
  id: number;
  guildId: string;
  type: string;
  commandName: string | null;
  userId: string | null;
  userName: string | null;
  payload: string | null;
  severity: string | null;
  aiSummary: string | null;
  aiTags: string | null;
  status: string;
  receivedAt: string;
};

export type Page<T> = {
  content: T[];
  number: number;
  size: number;
  totalPages: number;
  totalElements: number;
  first: boolean;
  last: boolean;
};

export type Meta = {
  commandNames: string[];
  matchTypes: Rule["matchType"][];
  actions: Rule["action"][];
};

export type Me =
  | { authenticated: false }
  | { authenticated: true; email: string; name: string };

const TOKEN_KEY = "astrabot_token";

/**
 * The backend and frontend are different registrable domains in
 * production (onrender.com vs vercel.app), and browsers block a
 * cross-site cookie as third-party regardless of SameSite=None - so
 * instead of a session cookie, the backend hands us an opaque token on
 * the post-login redirect URL and we hold it ourselves and send it back
 * explicitly as an Authorization header.
 */
export function captureAuthToken(): void {
  if (typeof window === "undefined") return;
  const url = new URL(window.location.href);
  const token = url.searchParams.get("token");
  if (!token) return;

  try {
    sessionStorage.setItem(TOKEN_KEY, token);
  } catch {
    // sessionStorage unavailable (private mode, etc.) - just won't persist
  }
  url.searchParams.delete("token");
  window.history.replaceState({}, "", url.toString());
}

function getAuthToken(): string | null {
  if (typeof window === "undefined") return null;
  try {
    return sessionStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

function clearAuthToken(): void {
  if (typeof window === "undefined") return;
  try {
    sessionStorage.removeItem(TOKEN_KEY);
  } catch {
    // ignore
  }
}

async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? "GET").toUpperCase();
  const headers = new Headers(init.headers);

  const token = getAuthToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    method,
    headers,
  });

  if (!response.ok) {
    throw new ApiError(response.status, await response.text().catch(() => ""));
  }

  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export class ApiError extends Error {
  status: number;
  constructor(status: number, body: string) {
    super(`API request failed with ${status}: ${body}`);
    this.status = status;
  }
}

export const api = {
  base: API_BASE,
  googleLoginUrl: `${API_BASE}/oauth2/authorization/google`,

  me: () => apiFetch<Me>("/api/me"),
  logout: async () => {
    await apiFetch<void>("/api/logout", { method: "POST" }).catch(() => {});
    clearAuthToken();
  },
  meta: () => apiFetch<Meta>("/api/dashboard/meta"),

  commands: (guildId: string) =>
    apiFetch<CommandConfig[]>(`/api/dashboard/${guildId}/commands`),
  saveCommands: (guildId: string, configs: CommandConfig[]) =>
    apiFetch<void>(`/api/dashboard/${guildId}/commands`, {
      method: "PUT",
      body: JSON.stringify(configs),
    }),

  rules: (guildId: string) => apiFetch<Rule[]>(`/api/dashboard/${guildId}/rules`),
  addRule: (guildId: string, rule: Partial<Rule>) =>
    apiFetch<Rule>(`/api/dashboard/${guildId}/rules`, {
      method: "POST",
      body: JSON.stringify(rule),
    }),
  deleteRule: (guildId: string, ruleId: number) =>
    apiFetch<void>(`/api/dashboard/${guildId}/rules/${ruleId}`, {
      method: "DELETE",
    }),

  interactions: (guildId: string, page: number = 0, size: number = 20) =>
    apiFetch<Page<Interaction>>(
      `/api/dashboard/${guildId}/interactions?page=${page}&size=${size}`
    ),

  connectGuild: (guildId: string) =>
    apiFetch<{ guildId: string; guildName: string }>("/api/dashboard/connect", {
      method: "POST",
      body: JSON.stringify({ guildId }),
    }),
};
