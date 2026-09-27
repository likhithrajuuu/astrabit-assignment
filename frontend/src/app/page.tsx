"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { api, type Me } from "@/lib/api";
import { GoogleSignInButton } from "@/components/GoogleSignInButton";

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
    <main className="flex min-h-screen items-center justify-center bg-neutral-950 text-neutral-100">
      <div className="w-96 rounded-lg bg-neutral-900 p-8">
        <h1 className="mb-4 text-lg font-semibold">Astrabot Dashboard</h1>

        {me === null && <p className="text-sm text-neutral-400">Loading…</p>}

        {me && !me.authenticated && (
          <>
            <p className="mb-4 text-sm text-neutral-400">
              Sign in with Google to configure your server.
            </p>
            <GoogleSignInButton />
          </>
        )}

        {me && me.authenticated && (
          <>
            <p className="mb-4 text-sm text-neutral-400">
              Signed in as {me.email}. Enter the Discord server (guild) ID you
              want to configure. Right-click your server icon in Discord
              (with Developer Mode on) and choose &quot;Copy Server ID&quot;.
            </p>
            <form onSubmit={goToGuild}>
              <input
                type="text"
                required
                value={guildId}
                onChange={(e) => setGuildId(e.target.value)}
                placeholder="e.g. 123456789012345678"
                className="mb-3 w-full rounded border border-neutral-700 bg-neutral-800 px-3 py-2 text-sm"
              />
              <button
                type="submit"
                className="w-full rounded bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700"
              >
                Open dashboard
              </button>
            </form>
            <a
              href={api.logoutUrl}
              className="mt-4 block text-center text-xs text-neutral-500 underline"
            >
              Sign out
            </a>
          </>
        )}
      </div>
    </main>
  );
}
