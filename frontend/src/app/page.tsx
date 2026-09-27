"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { api, discordInviteUrl, type Me } from "@/lib/api";
import { GoogleSignInButton } from "@/components/GoogleSignInButton";
import { DiscordIcon } from "@/components/icons";
import { Spinner } from "@/components/Spinner";

export default function Home() {
  const [me, setMe] = useState<Me | null>(null);
  const [guildId, setGuildId] = useState("");
  const router = useRouter();

  useEffect(() => {
    api
      .me()
      .then(setMe)
      .catch(() => setMe({ authenticated: false }));
  }, []);

  function goToGuild(e: FormEvent) {
    e.preventDefault();
    router.push(`/dashboard/${guildId}/commands`);
  }

  return (
    <main className="flex min-h-screen items-center justify-center px-4 py-16 text-neutral-100">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <div className="mb-4 inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-indigo-600/15 text-indigo-400">
            <DiscordIcon className="h-7 w-7" />
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-white">
            Astrabot Dashboard
          </h1>
          <p className="mt-2 text-sm text-neutral-400">
            Configure commands, rules, and review activity for your Discord
            server.
          </p>
        </div>

        <div className="card-panel">
          {me === null && <Spinner label="Loading…" />}

          {me && !me.authenticated && (
            <>
              <p className="mb-5 text-sm text-neutral-400">
                Sign in with Google to manage your server&apos;s bot
                configuration.
              </p>
              <GoogleSignInButton />
            </>
          )}

          {me && me.authenticated && (
            <>
              <div className="mb-6 flex items-center justify-between">
                <p className="text-sm text-neutral-400">
                  Signed in as{" "}
                  <span className="font-medium text-neutral-200">
                    {me.email}
                  </span>
                </p>
                <a
                  href={api.logoutUrl}
                  className="text-xs font-medium text-neutral-500 hover:text-neutral-300"
                >
                  Sign out
                </a>
              </div>

              <a
                href={discordInviteUrl()}
                className="btn-primary w-full !bg-[#5865F2] hover:!bg-[#4752c4]"
              >
                <DiscordIcon className="h-4 w-4" />
                Add Astrabot to a server
              </a>
              <p className="mt-2 text-center text-xs text-neutral-500">
                One click — pick a server on Discord and land straight on its
                dashboard.
              </p>

              <div className="my-6 flex items-center gap-3">
                <div className="h-px flex-1 bg-neutral-800" />
                <span className="text-xs text-neutral-500">
                  or open an existing one
                </span>
                <div className="h-px flex-1 bg-neutral-800" />
              </div>

              <form onSubmit={goToGuild} className="space-y-3">
                <div>
                  <label className="field-label">Server (guild) ID</label>
                  <input
                    type="text"
                    required
                    value={guildId}
                    onChange={(e) => setGuildId(e.target.value)}
                    placeholder="e.g. 123456789012345678"
                    className="input-field"
                  />
                </div>
                <button type="submit" className="btn-secondary w-full">
                  Open dashboard
                </button>
              </form>
            </>
          )}
        </div>
      </div>
    </main>
  );
}
